package org.jaybill.jbio.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

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
    <T> CompletableFuture<T> submit(Callable<T> c);
}
