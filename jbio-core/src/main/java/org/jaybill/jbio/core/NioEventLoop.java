package org.jaybill.jbio.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscArrayQueue;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

@Slf4j
public class NioEventLoop implements EventLoop, Runnable {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final int INIT = 0;
    private static final int STARTED = 1;
    private static final int CLOSING = 2;
    private static final int CLOSED = 3;
    private static final AtomicIntegerFieldUpdater<NioEventLoop> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(
            NioEventLoop.class, "state");
    private int state = INIT;
    private CompletableFuture<Void> closedFuture = new CompletableFuture<>();

    private Thread thread;
    private PriorityBlockingQueue<DelayTask<?>> delayTaskQueue;
    private Queue<Runnable> taskQueue;
    private Selector selector;
    private SelectorProvider provider;

    public NioEventLoop(SelectorProvider provider, String namePrefix) {
        this.provider = provider;
        this.thread = new Thread(this, namePrefix + COUNTER.getAndAdd(1));
        this.taskQueue = new MpscArrayQueue<>(16);
        this.delayTaskQueue = new PriorityBlockingQueue<>();
        try {
            this.selector = provider.openSelector();
        } catch (IOException e) {
            throw new JBIOException("can not open selector", e);
        }
    }

    public Selector selector() {
        return selector;
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == thread;
    }

    @Override
    public <T> CompletableFuture<T> submitTask(Callable<T> c) {
        var future = new CompletableFuture<T>();
        Runnable wrapperRunnable = () -> {
            try {
                var r = c.call();
                future.complete(r);
            } catch (Throwable t) {
                future.completeExceptionally(new TaskExecutionException(t));
            }
        };

        if (this.inEventLoop()) {
            wrapperRunnable.run();
        } else {
            if (stateUpdater.compareAndSet(this, INIT, STARTED)) {
                this.thread.start();
            }
            boolean added = taskQueue.offer(wrapperRunnable);
            if (added) {
                selector.wakeup();
            } else {
                future.completeExceptionally(new RejectTaskException("queue overflow"));
            }
        }
        return future;
    }

    @Override
    public CompletableFuture<?> scheduleTask(Runnable r, int delay, TimeUnit unit) {
        var future = new CompletableFuture<>();
        delayTaskQueue.offer(new DelayTask<>(r, System.nanoTime() + unit.toNanos(delay), future));
        // We must wake up the selector, in case of the selector is blocking in select() method.
        selector.wakeup();
        return future;
    }

    @Override
    public CompletableFuture<Void> close() {
        var future = new CompletableFuture<Void>();
        this.submitTask(() -> {
            state = CLOSING;
            closedFuture.whenComplete((r, t) -> {
                if (t != null) {
                    log.error("eventloop close error:", t);
                }
                future.complete(r);
            });
            return null;
        });
        return future;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                long selectTimeout = Long.MAX_VALUE;
                // 0. process delay task
                DelayTask<?> delayTask;
                while ((delayTask = delayTaskQueue.peek()) != null) {
                    selectTimeout = (delayTask.executeTime - System.nanoTime()) / (1000 * 1000);
                    if (selectTimeout <= 0) {
                        // remove task
                        delayTaskQueue.remove();
                        // if task be cancelled, continue next
                        if (delayTask.future.isCancelled()) {
                            continue;
                        }
                        try {
                            delayTask.r.run();
                        } catch (Throwable e) {
                            log.error("run delay task e:", e);
                        } finally {
                            // ignore exception
                            delayTask.future.complete(null);
                        }
                    } else {
                        break;
                    }
                }

                // 1. process io event
                int n;
                if (selectTimeout <= 0) {
                    n = selector.selectNow();
                } else {
                    n = selector.select(selectTimeout);
                }
                if (n != 0) {
                    var keys = selector.selectedKeys();
                    var it = keys.iterator();
                    while (it.hasNext()) {
                        var key = it.next();
                        it.remove();
                        var attr = key.attachment();
                        if (attr instanceof AbstractNioChannel ch) {
                            ch.ioEvent();
                        }
                    }
                }

                // 2. process task
                Runnable task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                }
            } catch (Throwable e) {
                log.error("eventloop occur error:", e);
            } finally {
                // shutdown gratefully
                if (state == CLOSING) {
                    try {
                        // Using `selectNow()` clears the effect of a `wakeup()` call by another thread,
                        // causing the selector to wake up directly in subsequent `select()` calls.
                        selector.selectNow();
                    } catch (Throwable e) {
                        log.error("selectNow() error:", e);
                    }

                    // 0. close all channel
                    selector.keys().forEach(k -> {
                        try {
                            ((NioChannel)(k.attachment())).close();
                        } catch (Throwable e) {
                            log.error("close channel error:", e);
                        }
                    });

                    // 1. try to finish all real-time tasks
                    Runnable r;
                    while ((r = taskQueue.poll()) != null) {
                        try {
                            r.run();
                        } catch (Throwable e) {
                            log.error("run task error:", e);
                        }
                    }

                    // 2. cancel all delay task
                    DelayTask<?> task;
                    while ((task = delayTaskQueue.poll()) != null) {
                        try {
                            task.future.cancel(true);
                        } catch (Throwable e) {
                            log.error("cancel task error:", e);
                        }
                    }

                    // 3. close selector
                    try {
                        selector.close();
                    } catch (Throwable e) {
                        log.error("selector close error:", e);
                    }

                    // 4. notify
                    state = CLOSED;
                    closedFuture.complete(null);
                    break;
                }
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DelayTask<T> implements Comparable<DelayTask<T>> {
        private Runnable r;
        private long executeTime;
        private CompletableFuture<T> future;

        @Override
        public int compareTo(DelayTask<T> o) {
            // small first
            long x = this.executeTime - o.executeTime;
            if (x < 0) {
                return -1;
            } else if (x == 0){
                return 0;
            } else {
                return 1;
            }
        }
    }
}
