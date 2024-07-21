package org.jaybill.jbio.example.echo;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.*;
import org.jaybill.jbio.core.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class EchoClient {

    public static void main(String[] args) {
        var handler = new DefaultChannelDuplexHandler() {
            @Override
            public void channelConnected(ChannelHandlerContext ctx) {
                var msg = "hello world, port: " + ctx.channel().localAddress().getPort();
                log.debug("Send -> {}", msg);
                // send
                ctx.channel().pipeline().fireChannelWriteAndFlush(
                        ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
                ctx.fireChannelConnected();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                // receive
                log.debug("Receive -> {}", ByteBufferUtil.toString((ByteBuffer) o));

                var msg = "hello world, port: " + ctx.channel().localAddress().getPort();
                log.debug("Send -> {}", msg);
                // send
                ctx.channel().pipeline().fireChannelWriteAndFlush(
                        ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
                ctx.fireChannelRead(o);
            }
        };
        var client = JBIOClient.newInstance()
                .eventLoop(1)
                .config(NioSocketChannelConfigTemplate.DEFAULT)
                .initializer((ch) -> ch.pipeline().addLast(handler));
        client.connect(new Address("127.0.0.1", 12345), new Address("127.0.0.1", 8080)).join();
        log.debug("Connect from 12345 to 8080");
        client.connect(new Address("127.0.0.1", 12346), new Address("127.0.0.1", 8080)).join();
        log.debug("Connect from 12346 to 8080");
    }
}
