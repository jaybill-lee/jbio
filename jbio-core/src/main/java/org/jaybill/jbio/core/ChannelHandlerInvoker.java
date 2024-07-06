package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface ChannelHandlerInvoker {
    // inbound event
    void fireChannelInitialized();
    void fireChannelBound();
    void fireChannelRegistered();
    void fireChannelRead(Object o);
    void fireChannelClosed();
    void fireChannelDeregistered();
    void fireChannelException(Throwable t);
    void fireChannelUnWritable();
    void fireChannelWritable();

    // outbound event
    CompletableFuture<Void> fireChannelWrite(ByteBuffer buf);
    CompletableFuture<Void> fireChannelFlush();
    CompletableFuture<Void> fireChannelWriteAndFlush(ByteBuffer buf);
}
