package org.jaybill.jbio.core;

public interface ChannelInboundHandler extends ChannelHandler {

    void channelRead(ChannelHandlerContext ctx, Object o);
}
