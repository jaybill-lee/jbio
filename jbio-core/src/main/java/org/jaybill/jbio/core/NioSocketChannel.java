package org.jaybill.jbio.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class NioSocketChannel extends AbstractNioChannel implements NioChannel  {
    private final ChannelLifecycle lifecycle;
    private final SocketChannel socketChannel;
    private NioEventLoop eventLoop;
    private SelectionKey selectionKey;
    private ChannelPipeline pipeline;
    private NioChannelConfig workerConfig;
    private NioChannelInitializer initializer;

    private final String host;
    private final Integer port;
    private final int mode;
    public static final int CONNECT_MODE = 0;
    public static final int ACCEPT_MODE = 1;

    private volatile CompletableFuture<NioSocketChannel> stateFuture;
    private final AtomicInteger state = new AtomicInteger(INIT);
    private static final int INIT = 0;
    private static final int ACTIVING = 1;
    private static final int ACTIVE = 2;
    private static final int INACTIVE = -1;

    public NioSocketChannel(SocketChannel socketChannel, NioChannelConfig workerConfig, NioChannelInitializer initializer, String host, Integer port, int mode) {
        super();
        this.lifecycle = new ChannelLifecycle();
        this.pipeline = new DefaultChannelPipeline();
        this.socketChannel = socketChannel;
        this.workerConfig = workerConfig;
        this.initializer = initializer;
        this.host = host;
        this.port = port;
        this.mode = mode;
    }

    @Override
    void handleIOEvent() {
        if (!selectionKey.isValid()) {
            return;
        }
        lifecycle.connect();
        lifecycle.read();
        lifecycle.write();
    }

    @Override
    public CompletableFuture<NioSocketChannel> open(NioEventLoop eventLoop) {
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
                ChannelUtil.forceClose(socketChannel);
            } else {
                state.compareAndSet(ACTIVING, ACTIVE);
            }
        });
        this.eventLoop = eventLoop;
        return stateFuture;
    }

    @Override
    public NioChannelConfig config() {
        return null;
    }

    @Override
    public StandardSocketOptions options() {
        return null;
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
        return null;
    }

    private final class ChannelLifecycle implements SocketLifecycle {
        @Override
        public void init() {
            // set non-blocking mode
            try {
                socketChannel.configureBlocking(false);
            } catch (IOException e) {
                ChannelUtil.forceClose(socketChannel);
                throw new JBIOException("config jdk SocketChannel to non-block exception", e);
            }

            // set socket option
            try {
                for (Map.Entry<SocketOption<?>, Object> entry : workerConfig.getOptions().entrySet()) {
                    var k = entry.getKey();
                    var v = entry.getValue();
                    if (k == SocketOption.SO_KEEPALIVE) {
                        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, (boolean) v);
                    } else if (k == SocketOption.TCP_NODELAY) {
                        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, (boolean) v);
                    } else if (k == SocketOption.SO_LINGER) {
                        socketChannel.setOption(StandardSocketOptions.SO_LINGER, (int) v);
                    } else if (k == SocketOption.SO_SNDBUF) {
                        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, (int) v);
                    } else if (k == SocketOption.SO_RCVBUF) {
                        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, (int) v);
                    } else if (k == SocketOption.SO_REUSEADDR) {
                        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, (boolean) v);
                    }
                }
            } catch (IOException e) {
                ChannelUtil.forceClose(socketChannel);
                throw new JBIOException("SocketChannel set options error", e);
            }

            // pipeline
            initializer.initChannel(NioSocketChannel.this);
        }

        @Override
        public void bind() {
            if (host != null && port != null) {
                try {
                    socketChannel.bind(new InetSocketAddress(host, port));
                } catch (IOException e) {
                    ChannelUtil.forceClose(socketChannel);
                    throw new JBIOException("SocketChannel bind error", e);
                }
            }
        }

        @Override
        public void register() {
            try {
                selectionKey = socketChannel.register(eventLoop.selector(), 0, NioSocketChannel.this);
            } catch (ClosedChannelException e) {
                ChannelUtil.forceClose(socketChannel);
                throw new JBIOException("SocketChannel register to Selector error", e);
            }
        }

        @Override
        public void active() {
            if (mode == CONNECT_MODE) {
                selectionKey.interestOps(SelectionKey.OP_CONNECT);
            } else if (mode == ACCEPT_MODE) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
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
        public void connect() {
            int readyOps = selectionKey.readyOps();
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                try {
                    socketChannel.finishConnect();
                } catch (IOException e) {
                    pipeline.fireChannelException(e);
                }
            }
        }

        @Override
        public void read() {
            int readyOps = selectionKey.readyOps();
            if ((readyOps & SelectionKey.OP_READ) != 0) {
                try {
                    int maxReadCount = workerConfig.getReadBehavior().getMaxReadCountPerLoop();
                    var strategy = workerConfig.getReadBehavior().getStrategy();
                    for (int i = 0; i < maxReadCount; i++) {
                        var buf = strategy.allocate();
                        int c = socketChannel.read(buf);
                        if (c == -1) {
                            socketChannel.close();
                            closeNotification();
                            break;
                        } else if (c == 0) {
                            // release buf
                            break;
                        } else if (c > 0) {
                            pipeline.fireChannelRead(buf);
                        }
                    }
                } catch (IOException e) {
                    try {
                        socketChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
                        socketChannel.close();
                        closeNotification();
                    } catch (IOException ex) {
                        // log
                    }
                }
            }
        }

        @Override
        public void write() {
            int readyOps = selectionKey.readyOps();
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {

            }
        }

        private void closeNotification() {
            pipeline.fireChannelClosed();
            deregister();
            pipeline.fireChannelInactive();
            pipeline.fireChannelDeregistered();
        }
    }
}
