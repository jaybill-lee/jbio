package org.jaybill.jbio.example.echo;


import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.*;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class EchoServer {

    public static void main(String[] args) {
        var server = JBIOServer.newInstance()
                .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
                .initializer(null, channel -> channel.pipeline().addLast(new EchoServerHandler()))
                .eventLoop(1, Runtime.getRuntime().availableProcessors());
        server.start("127.0.0.1", 8080).join();
    }
}
