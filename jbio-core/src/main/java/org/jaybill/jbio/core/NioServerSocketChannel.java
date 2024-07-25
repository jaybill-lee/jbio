package org.jaybill.jbio.core;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ex.AcceptSocketChannelException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NioServerSocketChannel extends AbstractNioChannel implements NioChannel {
    private final ChannelUnsafe unsafe;
    private final SelectorProvider provider;
    private final NioChannelConfig bossConfig;
    private final NioSocketChannelConfigTemplate workerConfigTemplate;
    private final NioChannelInitializer bossInitializer;
    private final NioChannelInitializer workerInitializer;
    private final String host;
    private final int port;
    private final Integer backlog;
    private final NioEventLoopGroup workerGroup;
    private final NioEventLoop eventLoop;
    private final ChannelPipeline pipeline;

    private ServerSocketChannel serverSocketChannel;
    private SelectionKey selectionKey;

    private volatile CompletableFuture<NioServerSocketChannel> stateFuture;
    private final AtomicInteger state = new AtomicInteger(INIT);
    private static final int INIT = 0;
    private static final int ACTIVING = 1;
    private static final int ACTIVE = 2;
    private static final int CLOSED = -1;

    NioServerSocketChannel(
            SelectorProvider provider,
            NioEventLoop eventLoop,
            NioChannelConfigTemplate bossConfigTemplate,
            NioSocketChannelConfigTemplate workerConfigTemplate,
            NioChannelInitializer bossInitializer,
            NioChannelInitializer workerInitializer,
            NioEventLoopGroup workerGroup,
            String host, int port, Integer backlog) {
        super();
        this.unsafe = new ChannelUnsafe();
        this.provider = provider;
        this.bossConfig = bossConfigTemplate.create();
        this.workerConfigTemplate = workerConfigTemplate;
        this.bossInitializer = bossInitializer;
        this.workerInitializer = workerInitializer;
        this.workerGroup = workerGroup;
        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.eventLoop = eventLoop;
        this.pipeline = new DefaultChannelPipeline(new HeadHandler(), new TailHandler(), this, eventLoop);
    }

    @Override
    void ioEvent() {
        if (!selectionKey.isValid()) {
            return;
        }
        if ((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
            unsafe.accept();
        }
    }

    @Override
    public CompletableFuture<NioServerSocketChannel> open() {
        if (!state.compareAndSet(INIT, ACTIVING)) {
            while (stateFuture == null) {
                Thread.onSpinWait();
            }
            return stateFuture;
        }
        stateFuture = eventLoop.submitTask(() -> {
            unsafe.init();
            unsafe.bind();
            unsafe.register();
            return this;
        }).whenComplete((r, t) -> {
            if (t != null) {
                state.compareAndSet(ACTIVING, CLOSED);
            } else {
                state.compareAndSet(ACTIVING, ACTIVE);
            }
        });
        return stateFuture;
    }

    @Override
    public void close() {
        unsafe.close();
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return (InetSocketAddress) serverSocketChannel.getLocalAddress();
        } catch (IOException e) {
            throw new JBIOException("local address error");
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
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
            unsafe.setOption(option, v);
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

    private class ChannelUnsafe implements ServerSocketUnsafe {
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

            pipeline.fireChannelInitialized();
        }

        @Override
        public void bind() {
            try {
                if (backlog == null) {
                    serverSocketChannel.bind(new InetSocketAddress(host, port));
                } else {
                    serverSocketChannel.bind(new InetSocketAddress(host, port), backlog);
                }
                pipeline.fireChannelBound();
            } catch (IOException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("bind socket error", e);
            }
        }

        @Override
        public void register() {
            try {
                selectionKey = serverSocketChannel.register(eventLoop.selector(),
                        SelectionKey.OP_ACCEPT, NioServerSocketChannel.this);
                pipeline.fireChannelRegistered();
            } catch (ClosedChannelException e) {
                ChannelUtil.forceClose(serverSocketChannel);
                throw new JBIOException("ServerSocketChannel register error", e);
            }
        }

        @Override
        public void close() {
            ChannelUtil.forceClose(serverSocketChannel);
            pipeline.fireChannelDeregistered();
            pipeline.fireChannelClosed();
        }

        @Override
        public void deregister() {
            selectionKey.cancel();
            pipeline.fireChannelDeregistered();
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

            var childCh = NioSocketChannel.newAcceptModeInstance(
                    provider, ch, workerGroup.next(), workerConfigTemplate.create(), workerInitializer);
            childCh.open();
        }

        private void setOption(SocketOption<?> k, Object v) throws IOException {
            if (k == SocketOption.SO_RCVBUF) {
                serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, (int) v);
            }
        }
    }

    private class HeadHandler extends DefaultChannelDuplexHandler {
        @Override
        public void close(ChannelHandlerContext ctx) {
            unsafe.close();
        }
    }
    private class TailHandler extends DefaultChannelDuplexHandler {}
}
