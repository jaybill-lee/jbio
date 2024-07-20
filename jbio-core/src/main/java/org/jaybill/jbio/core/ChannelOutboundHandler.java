package org.jaybill.jbio.core;

public interface ChannelOutboundHandler extends ChannelHandler {

    void close(ChannelHandlerContext ctx);

    void write(ChannelHandlerContext ctx, Object o);

    void flush(ChannelHandlerContext ctx);

    void writeAndFlush(ChannelHandlerContext ctx, Object o);
}
