package org.jaybill.jbio.core;

import java.net.StandardSocketOptions;
import java.util.concurrent.CompletableFuture;

public interface NioChannel {

    NioChannelConfig config();

    StandardSocketOptions options();

    CompletableFuture<? extends NioChannel> open(NioEventLoop eventLoop);

    CompletableFuture<Void> deregister();

    CompletableFuture<Void> close();

    CompletableFuture<Void> reset();
}
