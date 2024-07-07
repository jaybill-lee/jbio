package org.jaybill.jbio.http;

import org.jaybill.jbio.core.*;

public class Test {

    public static void main(String[] args) {
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .initializer(null, channel -> {
                    channel.pipeline().addLast(new HttpHandler());
                })
                .eventLoop(1, Runtime.getRuntime().availableProcessors());

        server.start("127.0.0.1", 8080).join();
    }
}
