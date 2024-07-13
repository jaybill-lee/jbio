package org.jaybill.jbio.http;

import lombok.extern.slf4j.Slf4j;
import org.jaybill.jbio.core.ChannelHandlerContext;
import org.jaybill.jbio.core.DefaultChannelDuplexHandler;
import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class HttpServerCodecHandler extends DefaultChannelDuplexHandler {

    private int requestId = 0; // the first one can use id
    private HttpRequest decodingRequest;
    private final Queue<Integer> requestIdQueue = new ArrayDeque<>();
    private final Map<Integer, HttpResponse> waitForFlushResponseMap = new HashMap<>();
    private final HttpServerCodec codec;

    public HttpServerCodecHandler() {
        this.codec = new HttpServerCodec();
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
                }

                switch (evt.getType()) {
                    case METHOD -> decodingRequest.setMethod((HttpMethod) evt.getResult());
                    case PATH -> decodingRequest.setPath((String) evt.getResult());
                    case VER -> decodingRequest.setVersion((HttpVersion) evt.getResult());
                    case HEADERS -> decodingRequest.setHeaders((Map<String, String>) evt.getResult());
                    case BODY -> decodingRequest.setBody((Queue<ByteBuffer>) evt.getResult());
                    case END -> {
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
            // close channel
            ctx.channel().pipeline().fireChannelClose();
        }
    }

    @Override
    public void channelException(ChannelHandlerContext ctx, Throwable t) {

    }

    @Override
    public CompletableFuture<Void> write(ChannelHandlerContext ctx, Object o) {
        return doWrite(ctx, o, ctx::fireChannelWrite);
    }

    @Override
    public CompletableFuture<Void> writeAndFlush(ChannelHandlerContext ctx, Object o) {
        return doWrite(ctx, o, ctx::fireChannelWriteAndFlush);
    }

    private CompletableFuture<Void> doWrite(ChannelHandlerContext ctx, Object o, Consumer<ByteBuffer> consumer) {
        var resp = (HttpResponse) o;
        Integer id = resp.requestId();

        if (Objects.equals(requestIdQueue.peek(), id)) {
            // clear resource
            requestIdQueue.poll();

            // encode and write
            var buf = codec.encode(resp);
            consumer.accept(buf);

            // try to flush other request, support HTTP pipeline
            Integer peekId = requestIdQueue.peek();
            var waitForFlushResp = peekId == null ? null : waitForFlushResponseMap.get(peekId);
            while (waitForFlushResp != null) {
                // clear resource
                requestIdQueue.poll();
                waitForFlushResponseMap.remove(id);

                // encode and write
                var waitForFlushBuf = codec.encode(resp);
                consumer.accept(waitForFlushBuf);

                // next request
                peekId = requestIdQueue.peek();
                waitForFlushResp = peekId == null ? null : waitForFlushResponseMap.get(peekId);
            }
        } else {
            waitForFlushResponseMap.put(id, resp);
        }
        return null;
    }
}
