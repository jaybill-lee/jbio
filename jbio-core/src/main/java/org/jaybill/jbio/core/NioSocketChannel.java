package org.jaybill.jbio.core;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NioSocketChannel extends AbstractNioChannel implements NioChannel  {
    public static final int CONNECT_MODE = 0;
    public static final int ACCEPT_MODE = 1;

    private final ChannelUnsafe unsafe;
    private final SocketChannel socketChannel;
    private final NioSocketChannelConfig workerConfig;
    private final NioChannelInitializer initializer;
    private final String host;
    private final Integer port;
    private final int mode;
    private final NioEventLoop eventLoop;
    private final ChannelPipeline pipeline;

    private SelectionKey selectionKey;
    private SendBuffer sendBuffer;
    private boolean channelUnWritable = false;

    private volatile CompletableFuture<NioSocketChannel> stateFuture;
    private final AtomicInteger state = new AtomicInteger(INIT);
    private static final int INIT = 0;
    private static final int ACTIVING = 1;
    private static final int ACTIVE = 2;
    private static final int CLOSED = -1;

    public NioSocketChannel(SocketChannel socketChannel, NioEventLoop eventLoop, NioSocketChannelConfig workerConfig,
                NioChannelInitializer initializer, String host, Integer port, int mode) {
        super();
        this.unsafe = new ChannelUnsafe();
        this.socketChannel = socketChannel;
        this.workerConfig = workerConfig;
        this.initializer = initializer;
        this.host = host;
        this.port = port;
        this.mode = mode;
        this.eventLoop = eventLoop;
        this.pipeline = new DefaultChannelPipeline(new HeadHandler(), new TailHandler(), this, eventLoop);
    }

    @Override
    void ioEvent() {
        if (!selectionKey.isValid()) {
            return;
        }
        try {
            unsafe.connect();
            unsafe.write(false);
            unsafe.read();
        } catch (CancelledKeyException e) {
            // Because all 3 methods above may close the channel due to IOException.
            // Therefore, it is necessary to capture the CancelledKeyException here.
            // ignore
            log.warn("channel key be cancel:", e);
        }
    }

    @Override
    void userEvent(UserEvent event) {

    }

    @Override
    public CompletableFuture<NioSocketChannel> open() {
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
                ChannelUtil.forceClose(socketChannel);
            } else {
                state.compareAndSet(ACTIVING, ACTIVE);
            }
        });
        return stateFuture;
    }

    @Override
    public ByteBufferAllocator allocator() {
        return workerConfig.getAllocator();
    }

    @Override
    public ReadBehavior readBehavior() {
        return workerConfig.getReadBehavior();
    }

    @Override
    public WriteBehavior writeBehavior() {
        return workerConfig.getWriteBehavior();
    }

    @Override
    public Map<SocketOption<?>, Object> options() {
        return Collections.unmodifiableMap(workerConfig.getOptions());
    }

    @Override
    public <T> T option(SocketOption<T> option) {
        return (T) workerConfig.getOptions().get(option);
    }

    @Override
    public <T> boolean option(SocketOption<T> option, T v) {
        try {
            unsafe.setOption(option, v);
            workerConfig.getOptions().put(option, v);
            return true;
        } catch (Exception e) {
            log.warn("set option error: ", e);
            return false;
        }
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    private final class ChannelUnsafe implements SocketUnsafe {
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
                    this.setOption(k, v);
                }
            } catch (IOException e) {
                ChannelUtil.forceClose(socketChannel);
                throw new JBIOException("SocketChannel set options error", e);
            }

            // send buffer
            sendBuffer = new SendBuffer();

            // pipeline
            if (initializer != null) {
                initializer.initChannel(NioSocketChannel.this);
            }

            pipeline.fireChannelInitialized();
        }

        @Override
        public void bind() {
            if (host != null && port != null) {
                try {
                    socketChannel.bind(new InetSocketAddress(host, port));
                    pipeline.fireChannelBound();
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
                if (mode == CONNECT_MODE) {
                    selectionKey.interestOps(SelectionKey.OP_CONNECT);
                } else if (mode == ACCEPT_MODE) {
                    selectionKey.interestOps(SelectionKey.OP_READ);
                }
                pipeline.fireChannelRegistered();
            } catch (ClosedChannelException e) {
                ChannelUtil.forceClose(socketChannel);
                throw new JBIOException("SocketChannel register to Selector error", e);
            }
        }

        @Override
        public void close() {
            ChannelUtil.forceClose(socketChannel);
            pipeline.fireChannelDeregistered();
            pipeline.fireChannelClosed();
        }

        @Override
        public void deregister() {
            selectionKey.cancel();
            pipeline.fireChannelDeregistered();
        }

        @Override
        public void connect() {
            int readyOps = selectionKey.readyOps();
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                try {
                    socketChannel.finishConnect();
                } catch (IOException e) {
                    pipeline.fireChannelException(e);
                    ChannelUtil.forceClose(socketChannel);
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
                        int available = buf.remaining();
                        int c = socketChannel.read(buf);
                        if (c == -1) {
                            socketChannel.close();
                            pipeline.fireChannelDeregistered();
                            pipeline.fireChannelClosed();
                            break;
                        } else if (c == 0) {
                            // release buf
                            break;
                        } else if (c > 0) {
                            buf.flip();
                            pipeline.fireChannelRead(buf);
                            if (c < available) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    this.close();
                } catch (Throwable e) {
                    pipeline.fireChannelException(e);
                }
            }
        }

        @Override
        public void write(boolean flush) {
            int readyOps = selectionKey.readyOps();
            if (flush || (readyOps & SelectionKey.OP_WRITE) != 0) {
                try {
                    var writeBehavior = workerConfig.getWriteBehavior();
                    for (int i = 0; i < writeBehavior.getMaxWritePerLoop(); i++) {
                        var buf = sendBuffer.remove();
                        if (buf == null) {
                            break;
                        }
                        // first write
                        socketChannel.write(buf);
                        if (buf.remaining() != 0) {
                            boolean flushed = false;
                            // spin write
                            for (int j = 0; j < writeBehavior.getSpinCount(); j++) {
                                socketChannel.write(buf);
                                if (buf.remaining() == 0) {
                                    flushed = true;
                                    break;
                                }
                            }
                            if (!flushed) {
                                log.warn("TCP send buffer is full, wait util next event loop, address:{}",
                                        socketChannel.getRemoteAddress());
                                // add OP_WRITE to interest key
                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                                sendBuffer.addFirst(buf);
                                break; // don't continue to write the next ByteBuffer
                            }
                        }
                    }

                    if (sendBuffer.isEmpty()) {
                        // All byte be sent, cancel the OP_WRITE
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    }

                    // edge trigger channelWritable()
                    if (channelUnWritable && writeBehavior.getLowWatermark() >= sendBuffer.unsentBytes()) {
                        channelUnWritable = false;
                        pipeline.fireChannelWritable();
                    }
                } catch (IOException e) {
                    unsafe.close();
                }
            }
        }

        private void setOption(SocketOption<?> k, Object v) throws IOException {
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
    }

    private final class HeadHandler extends DefaultChannelDuplexHandler {

        @Override
        public void close(ChannelHandlerContext ctx) {
            unsafe.close();
        }

        @Override
        public CompletableFuture<Void> write(ChannelHandlerContext ctx, Object b) {
            var buf = (ByteBuffer) b;
            sendBuffer.add(buf);
            var writeBehavior = workerConfig.getWriteBehavior();
            if (writeBehavior.getHighWatermark() <= sendBuffer.unsentBytes()) {
                channelUnWritable = true;
                pipeline.fireChannelUnWritable();
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> flush(ChannelHandlerContext ctx) {
            unsafe.write(true);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, Object buf) {
            this.write(ctx, buf);
            return this.flush(ctx);
        }
    }

    private final class TailHandler extends DefaultChannelDuplexHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object o) {
            if (!(o instanceof ByteBuffer)) {
                return;
            }
            var buffer = (ByteBuffer) o;
            // release buffer
        }
    }

    private final class SendBuffer {
        private Deque<ByteBuffer> bufferQueue;
        private volatile int unsentBytes = 0;

        public SendBuffer() {
            this.bufferQueue = new ArrayDeque<>(16);
        }

        public void add(ByteBuffer buf) {
            bufferQueue.offer(buf);
            unsentBytes += buf.remaining();
        }

        public void addFirst(ByteBuffer buf) {
            bufferQueue.offerFirst(buf);
            unsentBytes += buf.remaining();
        }

        public ByteBuffer remove() {
            var buf = bufferQueue.poll();
            if (buf != null) {
                unsentBytes -= buf.remaining();
            }
            return buf;
        }

        public boolean isEmpty() {
            return unsentBytes == 0;
        }

        public int size() {
            return bufferQueue.size();
        }

        public int unsentBytes() {
            return unsentBytes;
        }
    }
}
