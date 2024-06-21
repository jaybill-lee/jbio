package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface ChannelOutboundHandler extends ChannelHandler {

    CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf);

    CompletableFuture<Void> flush();

    CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf);
}
