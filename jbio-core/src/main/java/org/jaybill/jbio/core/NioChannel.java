package org.jaybill.jbio.core;

import java.net.StandardSocketOptions;
import java.util.concurrent.CompletableFuture;

public interface NioChannel {

    NioChannelConfig config();

    StandardSocketOptions options();

    ChannelPipeline pipeline();

    CompletableFuture<? extends NioChannel> open(NioEventLoop eventLoop);
}
