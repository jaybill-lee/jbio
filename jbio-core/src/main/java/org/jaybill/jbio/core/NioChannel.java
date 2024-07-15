package org.jaybill.jbio.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NioChannel {

    ByteBufferAllocator allocator();

    ReadBehavior readBehavior();

    WriteBehavior writeBehavior();

    /**
     * Return an unmodifiable map, please note modify it.
     */
    Map<SocketOption<?>, Object> options();

    /**
     * get option value
     */
    <T> T option(SocketOption<T> option);

    /**
     * update options
     * @return true: if success; false: if not support;
     */
    <T> boolean option(SocketOption<T> option, T v);

    ChannelPipeline pipeline();

    CompletableFuture<? extends NioChannel> open();
}
