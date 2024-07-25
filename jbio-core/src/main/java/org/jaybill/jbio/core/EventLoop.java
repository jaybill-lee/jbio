package org.jaybill.jbio.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface EventLoop {

    /**
     * check if current thread is the event-loop thread
     */
    boolean inEventLoop();

    /**
     * submit task
     * @param c callable
     * @return future
     * @throws RejectTaskException if queue overflows.
     * @throws TaskExecutionException if callable throw exception
     */
    <T> CompletableFuture<T> submitTask(Callable<T> c);

    /**
     * schedule task
     * @param r runnable
     * @param delay delay
     * @param unit time unit
     */
    CompletableFuture<?> scheduleTask(Runnable r, int delay, TimeUnit unit);

    /**
     * close eventloop
     */
    CompletableFuture<Void> close();
}
