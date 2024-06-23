package org.jaybill.jbio.core;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CompletableFuture;

public class JBIOServer {
    private final SelectorProvider provider = SelectorProvider.provider();
    private NioChannelConfigTemplate bossConfigTemplate;
    private NioSocketChannelConfigTemplate workerConfigTemplate;
    private NioChannelInitializer bossInitializer;
    private NioChannelInitializer workerInitializer;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private int bosses;
    private int workers;

    private JBIOServer() {}

    public static JBIOServer newInstance() {
        return new JBIOServer();
    }

    public JBIOServer config(NioChannelConfigTemplate bossConfigTemplate,
                             NioSocketChannelConfigTemplate workerConfigTemplate) {
        this.bossConfigTemplate = bossConfigTemplate;
        this.workerConfigTemplate = workerConfigTemplate;
        return this;
    }

    public JBIOServer initializer(
            NioChannelInitializer bossInitializer, NioChannelInitializer workerInitializer) {
        this.bossInitializer = bossInitializer;
        this.workerInitializer = workerInitializer;
        return this;
    }

    public JBIOServer eventLoop(int bosses, int workers) {
        this.bosses = bosses;
        this.workers = workers;
        this.bossGroup = new NioEventLoopGroup(bosses, provider);
        this.workerGroup = new NioEventLoopGroup(workers, provider);
        return this;
    }

    public CompletableFuture<NioServerSocketChannel> start(String host, int port) {
        return this.start(host, port, null);
    }

    public CompletableFuture<NioServerSocketChannel> start(String host, int port, Integer backlog) {
        var serverSocketChannel = new NioServerSocketChannel(provider, bossConfigTemplate, workerConfigTemplate,
                bossInitializer, workerInitializer, workerGroup, host, port, backlog);
        return serverSocketChannel.open(bossGroup.next());
    }
}
