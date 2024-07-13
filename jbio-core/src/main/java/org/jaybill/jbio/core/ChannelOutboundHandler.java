package org.jaybill.jbio.core;

import java.util.concurrent.CompletableFuture;

public interface ChannelOutboundHandler extends ChannelHandler {

    void close(ChannelHandlerContext ctx);

    CompletableFuture<Void> write(ChannelHandlerContext ctx, Object buf);

    CompletableFuture<Void> flush(ChannelHandlerContext ctx);

    CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, Object buf);
}
