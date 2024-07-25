package org.jaybill.jbio.core;

import java.util.concurrent.CompletableFuture;

public interface EventLoopGroup {
    EventLoop next();

    CompletableFuture<Void> close();
}
