package org.jaybill.jbio.core;

import org.jaybill.jbio.core.ex.AcceptSocketChannelException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class NioServerSocketChannel extends AbstractNioChannel implements NioChannel {
    private final ChannelLifecycle lifecycle;
    private final ChannelPipeline pipeline;
    private final SelectorProvider provider;
    private final NioChannelConfig serverSocketChannelConfig;
    private final NioChannelConfig socketChannelConfig;
    private final NioChannelInitializer bossInitializer;
    private final NioChannelInitializer workerInitializer;
    private final String host;
    private final int port;
    private final Integer backlog;
    private final NioEventLoopGroup workerGroup;

    private NioEventLoop eventLoop;
    private ServerSocketChannel serverSocketChannel;
    private SelectionKey selectionKey;

    private volatile CompletableFuture<NioServerSocketChannel> stateFuture;
    private final AtomicInteger state = new AtomicInteger(INIT);
    private static final int INIT = 0;
    private static final int ACTIVING = 1;
    private static final int ACTIVE = 2;
    private static final int INACTIVE = -1;

    NioServerSocketChannel(
            SelectorProvider provider,
            NioChannelConfig serverSocketChannelConfig,
            NioChannelConfig socketChannelConfig,
            NioChannelInitializer bossInitializer,
            NioChannelInitializer workerInitializer,
            NioEventLoopGroup workerGroup,
            String host, int port, Integer backlog) {
        super();
        this.lifecycle = new ChannelLifecycle();
        this.pipeline = new DefaultChannelPipeline();
        this.provider = provider;
        this.serverSocketChannelConfig = serverSocketChannelConfig;
        this.socketChannelConfig = socketChannelConfig;
        this.bossInitializer = bossInitializer;
        this.workerInitializer = workerInitializer;
        this.workerGroup = workerGroup;
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    @Override
    void handleIOEvent() {
        lifecycle.accept();
    }

    @Override
    public CompletableFuture<NioServerSocketChannel> open(NioEventLoop eventLoop) {
        if (!state.compareAndSet(INIT, ACTIVING)) {
            while (stateFuture == null) {
                Thread.onSpinWait();
            }
            return stateFuture;
        }
        stateFuture = eventLoop.submit(() -> {
            lifecycle.init();
            pipeline.fireChannelInitialized();
            lifecycle.bind();
            pipeline.fireChannelBound();
            lifecycle.register();
            pipeline.fireChannelRegistered();
            lifecycle.active();
            pipeline.fireChannelActive();
            return this;
        }).whenComplete((r, t) -> {
            if (t != null) {
                state.compareAndSet(ACTIVING, INACTIVE);
            } else {
                state.compareAndSet(ACTIVING, ACTIVE);
            }
        });
        this.eventLoop = eventLoop;
        return stateFuture;
    }

    @Override
    public CompletableFuture<Void> deregister() {
        return null;
    }

    @Override
    public CompletableFuture<Void> close() {
        return null;
    }

    @Override
    public CompletableFuture<Void> reset() {
        throw new IllegalStateException("not support");
    }

    @Override
    public NioChannelConfig config() {
        return null;
    }

    @Override
    public StandardSocketOptions options() {
        return null;
    }

    private class ChannelLifecycle implements ServerSocketLifecycle {
        @Override
        public void init() {
            // open
            try {
                serverSocketChannel = provider.openServerSocketChannel();
            } catch (IOException e) {
                throw new JBIOException("open NioServerSocketChannel error", e);
            }

            // set non-blocking mode
            try {
                serverSocketChannel.configureBlocking(false);
            } catch (IOException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("config jdk ServerSocketChannel to non-block exception", e);
            }

            // set socket option
            try {
                for (Map.Entry<SocketOption<?>, Object> option : serverSocketChannelConfig.getOptions().entrySet()) {
                    var k = option.getKey();
                    var v = option.getValue();
                    if (k == SocketOption.SO_RCVBUF) {
                        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, (int) v);
                        break;
                    }
                }
            } catch (IOException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("set socket option error", e);
            }

            // initialize the pipeline
            if (bossInitializer != null) {
                bossInitializer.initChannel(NioServerSocketChannel.this);
            }
            pipeline.addLast(new Acceptor());
        }

        @Override
        public void bind() {
            try {
                if (backlog == null) {
                    serverSocketChannel.bind(new InetSocketAddress(host, port));
                } else {
                    serverSocketChannel.bind(new InetSocketAddress(host, port), backlog);
                }
            } catch (IOException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("bind socket error", e);
            }
        }

        @Override
        public void register() {
            try {
                selectionKey = serverSocketChannel.register(eventLoop.selector(), 0, NioServerSocketChannel.this);
            } catch (ClosedChannelException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("ServerSocketChannel register error", e);
            }
        }

        @Override
        public void active() {
            selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        }

        @Override
        public void close() {

        }

        @Override
        public void inactive() {

        }

        @Override
        public void deregister() {

        }

        @Override
        public void accept() {
            SocketChannel ch;
            try {
                ch = serverSocketChannel.accept();
            } catch (IOException e) {
                pipeline.fireChannelException(new AcceptSocketChannelException(e));
                return;
            }

            var childCh = new NioSocketChannel(ch, socketChannelConfig, workerInitializer, null, null, NioSocketChannel.ACCEPT_MODE);
            childCh.open(workerGroup.next());
        }
    }

    private class Acceptor implements ChannelInboundHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object o) {

        }
    }
}
