package org.jaybill.jbio.core;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CompletableFuture;

public class JBIOServer {
    private static final JBIOServer INSTANCE = new JBIOServer();
    private final SelectorProvider provider = SelectorProvider.provider();
    private NioChannelConfig bossConfig;
    private NioChannelConfig workerConfig;
    private NioChannelInitializer bossInitializer;
    private NioChannelInitializer workerInitializer;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private int bosses;
    private int workers;

    private JBIOServer() {}

    public static JBIOServer getInstance() {
        return INSTANCE;
    }

    public JBIOServer config(NioChannelConfig bossConfig, NioChannelConfig workerConfig) {
        this.bossConfig = bossConfig;
        this.workerConfig = workerConfig;
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
        var serverSocketChannel = new NioServerSocketChannel(provider, bossConfig, workerConfig,
                bossInitializer, workerInitializer, workerGroup, host, port, backlog);
        return serverSocketChannel.open(bossGroup.next());
    }
}
