package org.jaybill.jbio.core;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ex.AcceptSocketChannelException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NioServerSocketChannel extends AbstractNioChannel implements NioChannel {
    private final ChannelLifecycle lifecycle;
    private ChannelPipeline pipeline;
    private final SelectorProvider provider;
    private final NioChannelConfig bossConfig;
    private final NioSocketChannelConfigTemplate workerConfigTemplate;
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
            NioChannelConfigTemplate bossConfigTemplate,
            NioSocketChannelConfigTemplate workerConfigTemplate,
            NioChannelInitializer bossInitializer,
            NioChannelInitializer workerInitializer,
            NioEventLoopGroup workerGroup,
            String host, int port, Integer backlog) {
        super();
        this.lifecycle = new ChannelLifecycle();
        this.provider = provider;
        this.bossConfig = bossConfigTemplate.create();
        this.workerConfigTemplate = workerConfigTemplate;
        this.bossInitializer = bossInitializer;
        this.workerInitializer = workerInitializer;
        this.workerGroup = workerGroup;
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    @Override
    void ioEvent() {
        if (!selectionKey.isValid()) {
            return;
        }
        if ((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
            lifecycle.accept();
        }
    }

    @Override
    void userEvent(UserEvent event) {

    }

    @Override
    public CompletableFuture<NioServerSocketChannel> open(NioEventLoop eventLoop) {
        if (!state.compareAndSet(INIT, ACTIVING)) {
            while (stateFuture == null) {
                Thread.onSpinWait();
            }
            return stateFuture;
        }
        this.eventLoop = eventLoop;
        this.pipeline = new DefaultChannelPipeline(new HeadHandler(), new TailHandler(), eventLoop);
        stateFuture = eventLoop.submitTask(() -> {
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
        return stateFuture;
    }

    @Override
    public ByteBufferAllocator allocator() {
       throw new IllegalStateException("not support");
    }

    @Override
    public ReadBehavior readBehavior() {
        throw new IllegalStateException("not support");
    }

    @Override
    public WriteBehavior writeBehavior() {
        throw new IllegalStateException("not support");
    }

    @Override
    public Map<SocketOption<?>, Object> options() {
        return Collections.unmodifiableMap(bossConfig.getOptions());
    }

    @Override
    public <T> T option(SocketOption<T> option) {
        return (T) bossConfig.getOptions().get(option);
    }

    @Override
    public <T> boolean option(SocketOption<T> option, T v) {
        try {
            lifecycle.setOption(option, v);
            bossConfig.getOptions().put(option, v);
            return true;
        } catch (Exception e) {
            log.warn("set option error:", e);
            return false;
        }
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
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
                for (Map.Entry<SocketOption<?>, Object> option : bossConfig.getOptions().entrySet()) {
                    var k = option.getKey();
                    var v = option.getValue();
                    this.setOption(k, v);
                }
            } catch (IOException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("set socket option error", e);
            }

            // initialize the pipeline
            if (bossInitializer != null) {
                bossInitializer.initChannel(NioServerSocketChannel.this);
            }
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

            var childCh = new NioSocketChannel(ch, workerConfigTemplate.create(), workerInitializer,
                    null, null, NioSocketChannel.ACCEPT_MODE);
            childCh.open(workerGroup.next());
        }

        private void setOption(SocketOption<?> k, Object v) throws IOException {
            if (k == SocketOption.SO_RCVBUF) {
                serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, (int) v);
            }
        }
    }

    private class HeadHandler implements ChannelDuplexHandler {

        @Override
        public void channelInitialized(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelBound(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelDeregistered(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object o) {

        }

        @Override
        public CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf) {
            return null;
        }

        @Override
        public CompletableFuture<Void> flush() {
            return null;
        }

        @Override
        public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf) {
            return null;
        }
    }

    private class TailHandler implements ChannelDuplexHandler {

        @Override
        public void channelInitialized(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelBound(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelDeregistered(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx) {

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object o) {

        }

        @Override
        public CompletableFuture<Void> write(ChannelHandlerContext ctx, ByteBuffer buf) {
            return null;
        }

        @Override
        public CompletableFuture<Void> flush() {
            return null;
        }

        @Override
        public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, ByteBuffer buf) {
            return null;
        }
    }
}
