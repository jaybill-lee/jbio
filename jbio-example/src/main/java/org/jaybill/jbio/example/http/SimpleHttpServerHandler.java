package org.jaybill.jbio.example.http;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ChannelHandlerContext;
import org.jaybill.jbio.core.DefaultChannelDuplexHandler;
import org.jaybill.jbio.http.GeneralStatusCode;
import org.jaybill.jbio.http.HttpPair;
import org.jaybill.jbio.http.HttpVersion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SimpleHttpServerHandler extends DefaultChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        var pair = (HttpPair) o;
        var req = pair.getRequest();
        log.debug("Receive -> {}", req);
        log.debug("        -> {}", toStr(req.getBody().peek()));

        var respBody = "Hi, I received the request:" + req.getPath();
        var resp = pair.getResponse();
        resp.setVersion(HttpVersion.HTTP1_1);
        resp.setStatusCode(GeneralStatusCode.OK.getCode());
        resp.setReasonPhrase(GeneralStatusCode.OK.getReasonPhrase());
        resp.setBody(respBody.getBytes(StandardCharsets.UTF_8));
        ctx.channel().pipeline().fireChannelWriteAndFlush(resp);
    }

    @Override
    public void writeAndFlush(ChannelHandlerContext ctx, Object o) {
        log.debug("Send -> {}", o);
        ctx.fireChannelWriteAndFlush(o);
    }

    private String toStr(ByteBuffer buffer) {
        var sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        return sb.toString();
    }
}
