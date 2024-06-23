package org.jaybill.jbio.core;

public interface ChannelInboundHandler extends ChannelHandler {

    void channelInitialized(ChannelHandlerContext ctx);

    void channelBound(ChannelHandlerContext ctx);

    void channelRegistered(ChannelHandlerContext ctx);

    void channelActive(ChannelHandlerContext ctx);

    void channelDeregistered(ChannelHandlerContext ctx);

    void channelInactive(ChannelHandlerContext ctx);

    void channelClosed(ChannelHandlerContext ctx);

    void channelRead(ChannelHandlerContext ctx, Object o);
}
