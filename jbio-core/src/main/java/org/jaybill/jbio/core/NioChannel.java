package org.jaybill.jbio.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NioChannel {

    NioChannelConfig config();

    Map<SocketOption<?>, Object> options();

    <T> T option(SocketOption<T> option);

    /**
     * update options
     * @return true: if success; false: if not support;
     */
    <T> boolean option(SocketOption<T> option, T v);

    ChannelPipeline pipeline();

    CompletableFuture<? extends NioChannel> open(NioEventLoop eventLoop);
}
