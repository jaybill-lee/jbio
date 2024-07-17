package org.jaybill.jbio.example.http;

import org.jaybill.jbio.core.JBIOServer;
import org.jaybill.jbio.core.NioChannelConfigTemplate;
import org.jaybill.jbio.core.NioSocketChannelConfigTemplate;
import org.jaybill.jbio.http.HttpServerCodecHandler;

public class SimpleHttpServer {

    public static void main(String[] args) {
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .eventLoop(1, Runtime.getRuntime().availableProcessors())
                .initializer(null, channel -> {
                    var p = channel.pipeline();
                    p.addLast(new HttpServerCodecHandler());
                    p.addLast(new SimpleHttpServerHandler());
                });
        server.start("127.0.0.1", 8080).join();
    }
}
