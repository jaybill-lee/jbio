package org.jaybill.jbio.core;

public interface ChannelPipeline extends ChannelHandlerInvoker {

    ChannelPipeline addLast(ChannelHandler handler);
    ChannelPipeline remove(ChannelHandler handler);
}
