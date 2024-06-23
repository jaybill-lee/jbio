package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface ChannelHandlerInvoker {
    // inbound event
    void fireChannelInitialized();
    void fireChannelBound();
    void fireChannelRegistered();
    void fireChannelActive();
    void fireChannelRead(ByteBuffer buf);
    void fireChannelClosed();
    void fireChannelInactive();
    void fireChannelDeregistered();
    void fireChannelException(Throwable t);

    // outbound event
    CompletableFuture<Void> fireChannelWrite(ByteBuffer buf);
}
