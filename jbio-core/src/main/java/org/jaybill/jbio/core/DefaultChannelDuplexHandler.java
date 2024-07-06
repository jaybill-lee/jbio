package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class DefaultChannelDuplexHandler implements ChannelInboundHandler, ChannelOutboundHandler {

    @Override
    public void channelInitialized(ChannelHandlerContext ctx) {
        ctx.fireChannelInitialized();
    }

    @Override
    public void channelBound(ChannelHandlerContext ctx) {
        ctx.fireChannelBound();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelDeregistered(ChannelHandlerContext ctx) {
        ctx.fireChannelDeregistered();
    }

    @Override
    public void channelUnWritable(ChannelHandlerContext ctx) {
        ctx.fireChannelUnWritable();
    }

    @Override
    public void channelWritable(ChannelHandlerContext ctx) {
        ctx.fireChannelWritable();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx) {
        ctx.fireChannelClosed();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        ctx.fireChannelRead(o);
    }

    @Override
    public void channelException(ChannelHandlerContext ctx, Throwable t) {
        ctx.fireChannelException(t);
    }

    @Override
    public CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf) {
        return ctx.fireChannelWrite(buf);
    }

    @Override
    public CompletableFuture<Void> flush(ChannelHandlerContext ctx) {
        return ctx.fireChannelFlush();
    }

    @Override
    public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf) {
        return ctx.fireChannelWriteAndFlush(buf);
    }
}
