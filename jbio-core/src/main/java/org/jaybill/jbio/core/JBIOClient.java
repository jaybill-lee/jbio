package org.jaybill.jbio.core;

import lombok.extern.slf4j.Slf4j;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class JBIOClient {
    private final SelectorProvider provider = SelectorProvider.provider();
    private NioSocketChannelConfigTemplate workerConfigTemplate;
    private NioChannelInitializer workerInitializer;
    private NioEventLoopGroup workerGroup;
    private int workers;

    private JBIOClient() {}

    public static JBIOClient newInstance() {
        return new JBIOClient();
    }

    public JBIOClient eventLoop(int workers) {
        this.workers = workers;
        workerGroup = new NioEventLoopGroup(workers, provider, "nio-worker-eventloop-");
        return this;
    }

    public JBIOClient config(NioSocketChannelConfigTemplate template) {
        this.workerConfigTemplate = template;
        return this;
    }

    public JBIOClient initializer(NioChannelInitializer initializer) {
        this.workerInitializer = initializer;
        return this;
    }

    public CompletableFuture<NioSocketChannel> connect(Address remoteAddress) {
        return connect(null, remoteAddress);
    }

    public CompletableFuture<NioSocketChannel> connect(Address localAddress, Address remoteAddress) {
        var nioSocketChannel = NioSocketChannel.newConnectModeInstance(provider, workerGroup.next(),
                workerConfigTemplate.create(), workerInitializer, localAddress, remoteAddress);
        return nioSocketChannel.open();
    }
}
