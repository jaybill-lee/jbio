package org.jaybill.jbio.core;

public interface ChannelHandlerInvoker {
    // inbound event
    void fireChannelInitialized();
    void fireChannelBound();
    void fireChannelRegistered();
    void fireChannelConnected();
    void fireChannelRead(Object o);
    void fireChannelClosed();
    void fireChannelDeregistered();
    void fireChannelException(Throwable t);
    void fireChannelUnWritable();
    void fireChannelWritable();
    void fireChannelSendBufferFull();

    // outbound event
    void fireChannelClose();
    void fireChannelWrite(Object o);
    void fireChannelFlush();
    void fireChannelWriteAndFlush(Object buf);
}
