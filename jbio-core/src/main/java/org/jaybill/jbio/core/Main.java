package org.jaybill.jbio.core;


public class Main {

    public static void main(String[] args) {
        var server = JBIOServer.getInstance()
                .config(NioChannelConfig.DEFAULT, NioChannelConfig.DEFAULT)
                .initializer(null, null)
                .eventLoop(2, Runtime.getRuntime().availableProcessors());

        server.start("127.0.0.1", 8080).join();
    }
}
