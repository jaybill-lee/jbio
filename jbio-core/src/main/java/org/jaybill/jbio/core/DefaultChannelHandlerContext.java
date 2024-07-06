package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DefaultChannelHandlerContext implements ChannelHandlerContext {
    private final ChannelHandler handler;
    private final NioChannel channel;
    private final EventLoop eventLoop;
    DefaultChannelHandlerContext next;
    DefaultChannelHandlerContext prev;

    public DefaultChannelHandlerContext(ChannelHandler handler, NioChannel channel, EventLoop eventLoop) {
        this.handler = handler;
        this.channel = channel;
        this.eventLoop = eventLoop;
    }

    @Override
    public NioChannel channel() {
        return channel;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }

    @Override
    public void fireChannelInitialized() {
        this.doInvokeInbound(ChannelInboundHandler::channelInitialized);
    }

    @Override
    public void fireChannelBound() {
        this.doInvokeInbound(ChannelInboundHandler::channelBound);
    }

    @Override
    public void fireChannelRegistered() {
        this.doInvokeInbound(ChannelInboundHandler::channelRegistered);
    }

    @Override
    public void fireChannelRead(Object o) {
        this.doInvokeInbound((handler, cur) -> handler.channelRead(cur, o));
    }

    @Override
    public void fireChannelClosed() {
        this.doInvokeInbound(ChannelInboundHandler::channelClosed);
    }

    @Override
    public void fireChannelDeregistered() {
        this.doInvokeInbound(ChannelInboundHandler::channelDeregistered);
    }

    @Override
    public void fireChannelException(Throwable t) {
        this.doInvokeInbound((handler, ctx) -> handler.channelException(ctx, t));
    }

    @Override
    public void fireChannelUnWritable() {
        this.doInvokeInbound(ChannelInboundHandler::channelUnWritable);
    }

    @Override
    public void fireChannelWritable() {
        this.doInvokeInbound(ChannelInboundHandler::channelWritable);
    }

    @Override
    public CompletableFuture<Void> fireChannelWrite(ByteBuffer buf) {
        return null;
    }

    @Override
    public CompletableFuture<Void> fireChannelFlush() {
        return null;
    }

    @Override
    public CompletableFuture<Void> fireChannelWriteAndFlush(ByteBuffer buf) {
        return null;
    }

    private void doInvokeInbound(BiConsumer<ChannelInboundHandler, DefaultChannelHandlerContext> consumer) {
        eventLoop.submitTask(() -> {
            DefaultChannelHandlerContext cur = this.next;
            while (cur != null) {
                if (cur.handler() instanceof ChannelInboundHandler handler) {
                    consumer.accept(handler, cur);
                    break;
                } else {
                    cur = this.next;
                }
            }
            return cur;
        });
    }
}
