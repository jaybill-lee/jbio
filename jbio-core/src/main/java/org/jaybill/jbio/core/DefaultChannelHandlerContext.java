package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DefaultChannelHandlerContext implements ChannelHandlerContext {
    private final ChannelHandler handler;
    private final EventLoop eventLoop;
    DefaultChannelHandlerContext next;
    DefaultChannelHandlerContext prev;

    public DefaultChannelHandlerContext(ChannelHandler handler, EventLoop eventLoop) {
        this.handler = handler;
        this.eventLoop = eventLoop;
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
    public void fireChannelActive() {
        this.doInvokeInbound(ChannelInboundHandler::channelActive);
    }

    @Override
    public void fireChannelRead(ByteBuffer buf) {
        this.doInvokeInbound((handler, cur) -> handler.channelRead(cur, buf));
    }

    @Override
    public void fireChannelClosed() {
        this.doInvokeInbound(ChannelInboundHandler::channelClosed);
    }

    @Override
    public void fireChannelInactive() {
        this.doInvokeInbound(ChannelInboundHandler::channelInactive);
    }

    @Override
    public void fireChannelDeregistered() {
        this.doInvokeInbound(ChannelInboundHandler::channelDeregistered);
    }

    @Override
    public void fireChannelException(Throwable t) {

    }

    @Override
    public CompletableFuture<Void> fireChannelWrite(ByteBuffer buf) {
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
