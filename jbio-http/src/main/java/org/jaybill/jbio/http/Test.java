package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import org.jaybill.jbio.core.*;

import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) {
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .initializer(null, channel -> {
                    channel.pipeline().addLast(new HttpServerCodecHandler());
                    channel.pipeline().addLast(new DefaultChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object o) {
                            var pair = (HttpPair) o;
                            var resp = pair.getResponse();
                            resp.setVersion(HttpVersion.HTTP1_1);
                            resp.setStatusCode(GeneralStatusCode.OK.getCode());
                            resp.setReasonPhrase(GeneralStatusCode.OK.getReasonPhrase());
                            resp.setBody("hello world".getBytes(StandardCharsets.UTF_8));
                            ctx.channel().pipeline().fireChannelWriteAndFlush(resp);
                        }
                    });
                })
                .eventLoop(1, Runtime.getRuntime().availableProcessors());

        server.start("127.0.0.1", 8080).join();
    }

    @AllArgsConstructor
    public static class Dto {
        private String name;
        private int age;
        private String city;
        private String desc;
    }
}
