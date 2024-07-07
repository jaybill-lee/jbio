package org.jaybill.jbio.http;

import org.jaybill.jbio.core.ChannelHandlerContext;
import org.jaybill.jbio.core.DefaultChannelDuplexHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class HttpHandler extends DefaultChannelDuplexHandler {

    private HttpDecoder decoder;

    public HttpHandler() {
        this.decoder = new HttpDecoder();
    }

    @Override
    public void channelUnWritable(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelWritable(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        var buf = (ByteBuffer) o;
        decoder.decode(buf, System.out::println);
    }

    @Override
    public void channelException(ChannelHandlerContext ctx, Throwable t) {

    }

    @Override
    public CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf) {
        return null;
    }

    @Override
    public CompletableFuture<Void> flush(ChannelHandlerContext ctx) {
        return null;
    }

    @Override
    public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf) {
        return null;
    }
}
