package org.jaybill.jbio.core;

import org.jctools.queues.MpscArrayQueue;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NioEventLoop implements EventLoop, Runnable {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final AtomicBoolean started;
    private Thread thread;
    private Queue<Runnable> taskQueue;
    private Selector selector;
    private SelectorProvider provider;

    public NioEventLoop(SelectorProvider provider) {
        this.provider = provider;
        this.started = new AtomicBoolean(false);
        this.thread = new Thread(this, "nio-eventloop-" + COUNTER.getAndAdd(1));
        this.taskQueue = new MpscArrayQueue<>(16);
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
    public <T> CompletableFuture<T> submit(Callable<T> c) {
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
            if (!started.compareAndExchange(false, true)) {
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
    public void run() {
        for (;;) {
            try {
                int n = selector.select();
                if (n != 0) {
                    var keys = selector.selectedKeys();
                    var it = keys.iterator();
                    while (it.hasNext()) {
                        var key = it.next();
                        it.remove();
                        var attr = key.attachment();
                        if (attr instanceof NioSocketChannel ch) {
                            ch.handleIOEvent();
                        } else if (attr instanceof NioServerSocketChannel ch) {
                            ch.handleIOEvent();
                        }
                    }
                }
                Runnable task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
