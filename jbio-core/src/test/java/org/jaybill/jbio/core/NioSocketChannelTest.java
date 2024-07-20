package org.jaybill.jbio.core;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class NioSocketChannelTest {

    private static int initialPort = 9090;

    /**
     * base usage
     */
    @Test
    public void test_serverAcceptSocket_base() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var clientMsg = "hello world";
        var counter = new AtomicInteger(0);
        var initFuture = new CompletableFuture<Integer>();
        var registeredFuture = new CompletableFuture<Integer>();
        var readFuture = new CompletableFuture<Integer>();
        var writeFuture = new CompletableFuture<Integer>();
        var flushFuture = new CompletableFuture<Integer>();
        var readAgainFuture = new CompletableFuture<Integer>();
        var writeAndFlushFuture = new CompletableFuture<Integer>();
        var deregisteredFuture = new CompletableFuture<Integer>();
        var closedFuture = new CompletableFuture<Integer>();

        int port = initialPort++;
        System.out.println("port = " + port);
        // server side
        var handler = new ChannelDuplexHandler() {
            @Override
            public void close(ChannelHandlerContext ctx) {
                ctx.fireChannelClose();
            }

            @Override
            public void channelSendBufferFull(ChannelHandlerContext ctx) {
                ctx.fireSendChannelFull();
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object o) {
                var buf = ByteBuffer.wrap(o.toString().getBytes(StandardCharsets.UTF_8));
                writeFuture.complete(counter.incrementAndGet());
                ctx.fireChannelWrite(buf);
            }

            @Override
            public void flush(ChannelHandlerContext ctx) {
                flushFuture.complete(counter.incrementAndGet());
                ctx.fireChannelFlush();
            }

            @Override
            public void writeAndFlush(ChannelHandlerContext ctx, Object o) {
                var buf = ByteBuffer.wrap(o.toString().getBytes(StandardCharsets.UTF_8));
                writeAndFlushFuture.complete(counter.incrementAndGet());
                ctx.fireChannelWriteAndFlush(buf);
            }

            @Override
            public void channelInitialized(ChannelHandlerContext ctx) {
                initFuture.complete(counter.incrementAndGet());
                ctx.fireChannelInitialized();
            }

            @Override
            public void channelBound(ChannelHandlerContext ctx) {
                ctx.fireChannelBound();
            }

            @Override
            public void channelRegistered(ChannelHandlerContext ctx) {
                registeredFuture.complete(counter.incrementAndGet());
                ctx.fireChannelRegistered();
            }

            @Override
            public void channelDeregistered(ChannelHandlerContext ctx) {
                deregisteredFuture.complete(counter.incrementAndGet());
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
                closedFuture.complete(counter.incrementAndGet());
                ctx.fireChannelClosed();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                var buf = (ByteBuffer) o;
                StringBuilder sb = (StringBuilder) ctx.attr("sb");
                if (sb == null) {
                    sb = new StringBuilder();
                    ctx.attr("sb", sb);
                }
                while (buf.hasRemaining()) {
                    sb.append((char) buf.get());
                    if (sb.toString().equals(clientMsg)) {
                        sb.setLength(0);
                        // response
                        if (ctx.attr("cnt") == null) {
                            readFuture.complete(counter.incrementAndGet());
                            ctx.channel().pipeline().fireChannelWrite(clientMsg);
                            ctx.channel().pipeline().fireChannelFlush();
                            ctx.attr("cnt", 1);
                        } else {
                            readAgainFuture.complete(counter.incrementAndGet());
                            ctx.channel().pipeline().fireChannelWriteAndFlush(clientMsg);
                        }
                    }
                }
            }

            @Override
            public void channelException(ChannelHandlerContext ctx, Throwable t) {

            }
        };
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .eventLoop(1, 1)
                .initializer(null, (ch) -> ch.pipeline().addLast(handler));
        server.start("127.0.0.1", port).join();

        // client side
        var socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port));
        var out = socket.getOutputStream();
        var in = socket.getInputStream();
        Assert.assertEquals(1, initFuture.get(3, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(2, registeredFuture.get(3, TimeUnit.SECONDS).intValue());

        // write
        out.write(clientMsg.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(3, readFuture.get(3, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(4, writeFuture.get(3, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(5, flushFuture.get(3, TimeUnit.SECONDS).intValue());

        // read
        byte [] bs = new byte[64];
        int len = in.read(bs);
        Assert.assertEquals(clientMsg, new String(bs, 0, len)); // req = res

        // write again
        out.write(clientMsg.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(6, readAgainFuture.get(3, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(7, writeAndFlushFuture.get(3, TimeUnit.SECONDS).intValue());

        // read again
        len = in.read(bs);
        Assert.assertEquals(clientMsg, new String(bs, 0, len)); // req = res

        // close
        socket.close();
        Assert.assertEquals(8, deregisteredFuture.get(3, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(9, closedFuture.get(3, TimeUnit.SECONDS).intValue());
    }

    /**
     * trigger watermark
     */
    @Test
    public void test_serverAcceptSocket_watermark() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var counter = new AtomicInteger();
        var write1Future = new CompletableFuture<Integer>();
        var write2Future = new CompletableFuture<Integer>();
        var write3Future = new CompletableFuture<Integer>();
        var unWritableFuture = new CompletableFuture<Integer>();
        var writableFuture = new CompletableFuture<Integer>();
        var flushFuture = new CompletableFuture<Integer>();
        var closedFuture = new CompletableFuture<Integer>();

        int port = initialPort++;
        System.out.println("port = " + port);
        var msg = "hello world";
        var len = msg.getBytes(StandardCharsets.UTF_8).length;
        int loop = 2;
        var socketChannelConfig = NioSocketChannelConfigTemplate.DEFAULT;
//        socketChannelConfig.getOptions().put(SocketOption.SO_REUSEADDR, true);
//        socketChannelConfig.getOptions().put(SocketOption.SO_REUSE_PORT, true);
        // reset high & low watermark
        socketChannelConfig.setHighWatermark(len * loop);
        socketChannelConfig.setLowWatermark(len);
        socketChannelConfig.getOptions().put(SocketOption.SO_SNDBUF, len); // set TCP send buffer
        socketChannelConfig.setSpinCount(0); // disable spin write

        // server
        var handler = new DefaultChannelDuplexHandler() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) {
                // write 3 times
                ctx.channel().pipeline().fireChannelWrite(msg);
                ctx.eventloop().scheduleTask(() -> ctx.channel().pipeline().fireChannelWrite(msg), 1, TimeUnit.SECONDS);
                ctx.eventloop().scheduleTask(() -> ctx.channel().pipeline().fireChannelWrite(msg), 2, TimeUnit.SECONDS);
                ctx.fireChannelRegistered();
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object o) {
                String key = "writeCnt";
                Integer cnt = (Integer) ctx.attr(key);
                if (cnt == null) {
                    cnt = 1;
                    ctx.attr(key, cnt);
                } else {
                    cnt++;
                    ctx.attr(key, cnt);
                }

                var buf = ByteBuffer.wrap(o.toString().getBytes(StandardCharsets.UTF_8));
                counter.incrementAndGet();
                if (cnt == 1) {
                    write1Future.complete(counter.intValue());
                    ctx.fireChannelWrite(buf);
                } else if (cnt == 2) {
                    write2Future.complete(counter.intValue()); // after this, should trigger unWritable method
                    ctx.fireChannelWrite(buf);
                } else if (cnt == 3) {
                    // will not trigger unWritable anymore
                    write3Future.complete(counter.intValue());
                    ctx.fireChannelWrite(buf);
                    ctx.channel().pipeline().fireChannelFlush(); // flush
                }
            }

            @Override
            public void flush(ChannelHandlerContext ctx) {
                flushFuture.complete(counter.incrementAndGet());
                ctx.fireChannelFlush();
            }

            @Override
            public void channelWritable(ChannelHandlerContext ctx) {
                writableFuture.complete(counter.incrementAndGet());
                ctx.fireChannelWritable();
            }

            @Override
            public void channelUnWritable(ChannelHandlerContext ctx) {
                unWritableFuture.complete(counter.incrementAndGet());
                ctx.fireChannelUnWritable();
            }

            @Override
            public void channelClosed(ChannelHandlerContext ctx) {
                closedFuture.complete(counter.incrementAndGet());
                ctx.fireChannelClosed();
            }
        };
        var server = JBIOServer.newInstance()
                .eventLoop(1, 1)
                .config(NioChannelConfigTemplate.DEFAULT, socketChannelConfig)
                .initializer(null, ch -> ch.pipeline().addLast(handler));
        server.start("127.0.0.1", port).join();

        // client
        var socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port));

        Assert.assertEquals(1, write1Future.get(5, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(2, write2Future.get(5, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(3, unWritableFuture.get(5, TimeUnit.SECONDS).intValue()); // trigger unWritable
        Assert.assertEquals(4, write3Future.get(5, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(5, flushFuture.get(5, TimeUnit.SECONDS).intValue());
        Assert.assertEquals(6, writableFuture.get(5, TimeUnit.SECONDS).intValue());

        // close
        socket.close();
        Assert.assertEquals(7, closedFuture.get(5, TimeUnit.SECONDS).intValue());
    }

    @Test
    public void test_sendBufferFull() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var msg = "hello world, I am very happy!";
        int port = initialPort++;
        System.out.println("port = " + port);
        var fullFuture = new CompletableFuture<Boolean>();
        var handler = new DefaultChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                ctx.channel().pipeline()
                        .fireChannelWriteAndFlush(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
                ctx.fireChannelRead(o);
            }

            @Override
            public void channelSendBufferFull(ChannelHandlerContext ctx) {
                fullFuture.complete(true);
                ctx.fireSendChannelFull();
            }
        };

        var socketChannelConfig = NioSocketChannelConfigTemplate.DEFAULT;
        socketChannelConfig.getOptions().put(SocketOption.SO_SNDBUF, 1); // set TCP send buffer
//        socketChannelConfig.getOptions().put(SocketOption.SO_REUSEADDR, true);
//        socketChannelConfig.getOptions().put(SocketOption.SO_REUSE_PORT, true);
        socketChannelConfig.setSpinCount(0); // disable spin write

        var server = JBIOServer.newInstance()
                .eventLoop(1, 1)
                .config(NioChannelConfigTemplate.DEFAULT, socketChannelConfig)
                .initializer(null, ch -> ch.pipeline().addLast(handler));
        server.start("127.0.0.1", port).join();

        // client, always send and don't read
        var client = new Socket();
        client.connect(new InetSocketAddress("127.0.0.1", port));
        client.setOption(StandardSocketOptions.SO_RCVBUF, 1);
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) <= 30 * 1000) { // try to wait for send buffer full.
            if (fullFuture.isDone()) {
                break;
            }
            client.getOutputStream().write("1".getBytes(StandardCharsets.UTF_8));
        }
        Assert.assertTrue(fullFuture.get(1, TimeUnit.SECONDS));
    }
}
