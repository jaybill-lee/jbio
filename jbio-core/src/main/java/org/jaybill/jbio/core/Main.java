package org.jaybill.jbio.core;


import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) {
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .initializer(null, channel -> {
                    channel.pipeline().addLast(new ChannelDuplexHandler() {
                        @Override
                        public void channelInitialized(ChannelHandlerContext ctx) {
                            System.out.println("channelInitialized 触发了");
                            ctx.fireChannelInitialized();
                        }

                        @Override
                        public void channelBound(ChannelHandlerContext ctx) {
                            System.out.println("channelBound 触发了");
                            ctx.fireChannelBound();
                        }

                        @Override
                        public void channelRegistered(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public void channelDeregistered(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public void channelClosed(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object o) {

                        }

                        @Override
                        public CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf) {
                            return null;
                        }

                        @Override
                        public CompletableFuture<Void> flush() {
                            return null;
                        }

                        @Override
                        public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf) {
                            return null;
                        }
                    });
                })
                .eventLoop(1, Runtime.getRuntime().availableProcessors());

        server.start("127.0.0.1", 8080).join();
    }
}
