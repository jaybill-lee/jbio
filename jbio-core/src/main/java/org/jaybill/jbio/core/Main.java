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
                        public void channelDeregistered(ChannelHandlerContext ctx) {

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
                            ctx.fireChannelWrite((ByteBuffer) o);
                        }

                        @Override
                        public void channelException(ChannelHandlerContext ctx, Throwable t) {

                        }

                        @Override
                        public void close(ChannelHandlerContext ctx) {

                        }

                        @Override
                        public CompletableFuture<Void> write(ChannelHandlerContext ctx, Object buf) {
                            return null;
                        }

                        @Override
                        public CompletableFuture<Void> flush(ChannelHandlerContext ctx) {
                            return null;
                        }

                        @Override
                        public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, Object buf) {
                            return null;
                        }
                    });
                })
                .eventLoop(1, Runtime.getRuntime().availableProcessors());

        server.start("127.0.0.1", 8080).join();
    }
}
