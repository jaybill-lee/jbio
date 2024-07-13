package org.jaybill.jbio.core;

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
    void fireChannelClose();
    CompletableFuture<Void> fireChannelWrite(Object o);
    CompletableFuture<Void> fireChannelFlush();
    CompletableFuture<Void> fireChannelWriteAndFlush(Object buf);
}
