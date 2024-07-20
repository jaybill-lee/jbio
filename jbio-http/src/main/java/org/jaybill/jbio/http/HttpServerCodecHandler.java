package org.jaybill.jbio.http;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ChannelHandlerContext;
import org.jaybill.jbio.core.DefaultChannelDuplexHandler;
import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HttpServerCodecHandler extends DefaultChannelDuplexHandler {

    public static final int DEFAULT_MAX_BODY_SIZE = 65535; // bytes
    public static final int DEFAULT_DECODE_TIMEOUT_SEC = 15;

    // control properties
    private final int maxBodySize;
    private final int decodeTimeoutSec;
    private CompletableFuture<?> decodeTimeoutFuture;

    // internal set
    private int requestId = 0; // the first one can use id
    private HttpRequest decodingRequest;
    private HttpServerCodec codec;
    private final Queue<Integer> requestIdQueue = new ArrayDeque<>();
    private final Map<Integer, HttpResponse> waitForFlushResponseMap = new HashMap<>();

    public HttpServerCodecHandler() {
        this(DEFAULT_MAX_BODY_SIZE, DEFAULT_DECODE_TIMEOUT_SEC);
    }

    public HttpServerCodecHandler(int maxBodySize) {
        this(maxBodySize, DEFAULT_DECODE_TIMEOUT_SEC);
    }

    public HttpServerCodecHandler(int maxBodySize, int decodeTimeoutSec) {
        this.maxBodySize = maxBodySize;
        this.decodeTimeoutSec = decodeTimeoutSec;
    }

    @Override
    public void channelInitialized(ChannelHandlerContext ctx) {
        // create the codec when channel is initialized
        this.codec = new HttpServerCodec(ctx.channel().allocator());
        ctx.fireChannelInitialized();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        var buf = (ByteBuffer) o;
        try {
            codec.decode(buf, (evt) -> {
                if (decodingRequest == null) {
                    decodingRequest = new HttpRequest();

                    // Detect whether decoding for this request has timed out.
                    // If the timeout is exceeded and not completed, close the TCP connection.
                    // This helps prevent resource wastage caused by malicious requests.
                    decodeTimeoutFuture = ctx.eventloop().scheduleTask(() -> {
                        log.error("HTTP request decode timeout:{}", decodeTimeoutSec);
                        ctx.channel().pipeline().fireChannelClose();
                    }, decodeTimeoutSec, TimeUnit.SECONDS);
                }

                switch (evt.getType()) {
                    case METHOD -> decodingRequest.setMethod((HttpMethod) evt.getResult());
                    case PATH -> decodingRequest.setPath((String) evt.getResult());
                    case VER -> decodingRequest.setVersion((HttpVersion) evt.getResult());
                    case HEADERS -> decodingRequest.setHeaders((Map<String, String>) evt.getResult());
                    case BODY -> decodingRequest.setBody((Queue<ByteBuffer>) evt.getResult());
                    case END -> {
                        log.debug("HTTP request decode end, id:{}", requestId);
                        // cancel to prevent the channel be closed
                        decodeTimeoutFuture.cancel(false);
                        decodeTimeoutFuture = null;

                        int id = requestId++;
                        var resp = new HttpResponse();
                        resp.requestId(id);
                        requestIdQueue.offer(id);
                        ctx.fireChannelRead(new HttpPair(decodingRequest, resp));
                        decodingRequest = null;
                    }
                }
            });
        } catch (HttpProtocolException e) {
            log.error("HTTP request decode error: ", e);
            // close channel
            ctx.channel().pipeline().fireChannelClose();
        }
    }

    @Override
    public void channelException(ChannelHandlerContext ctx, Throwable t) {

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object o) {
        doWrite(ctx, o, ctx::fireChannelWrite);
    }

    @Override
    public void writeAndFlush(ChannelHandlerContext ctx, Object o) {
        doWrite(ctx, o, ctx::fireChannelWriteAndFlush);
    }

    private void doWrite(ChannelHandlerContext ctx, Object o, Consumer<ByteBuffer> consumer) {
        var resp = (HttpResponse) o;
        Integer id = resp.requestId();

        if (Objects.equals(requestIdQueue.peek(), id)) {
            // clear resource
            requestIdQueue.poll();

            // encode and write
            var buf = codec.encode(resp, ctx.channel().allocator());
            consumer.accept(buf);

            // try to flush other request, support HTTP pipeline
            Integer peekId = requestIdQueue.peek();
            var waitForFlushResp = peekId == null ? null : waitForFlushResponseMap.get(peekId);
            while (waitForFlushResp != null) {
                // clear resource
                requestIdQueue.poll();
                waitForFlushResponseMap.remove(id);

                // encode and write
                var waitForFlushBuf = codec.encode(waitForFlushResp, ctx.channel().allocator());
                consumer.accept(waitForFlushBuf);

                // next request
                peekId = requestIdQueue.peek();
                waitForFlushResp = peekId == null ? null : waitForFlushResponseMap.get(peekId);
            }
        } else {
            waitForFlushResponseMap.put(id, resp);
        }
    }
}
