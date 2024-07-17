package org.jaybill.jbio.example.echo;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ChannelHandlerContext;
import org.jaybill.jbio.core.DefaultChannelDuplexHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class EchoServerHandler extends DefaultChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        var sb = new StringBuilder();
        if (o instanceof ByteBuffer buf) {
            while (buf.hasRemaining()) {
                char c = (char) buf.get();
                sb.append(c);
            }
            log.debug("Get msg:{}", sb);
        }
        ctx.fireChannelWriteAndFlush(ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
