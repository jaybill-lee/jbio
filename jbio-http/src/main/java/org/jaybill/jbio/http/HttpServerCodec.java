package org.jaybill.jbio.http;

import org.jaybill.jbio.core.ByteBufferAllocator;
import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class HttpServerCodec implements HttpCodec<HttpResponse> {

    enum State {
        SKIP_SPACE,
        READ_METHOD,
        READ_PATH,
        READ_VERSION,
        READ_HEADERS,
        READ_BODY,
    }

    // state machine
    private State state = State.SKIP_SPACE;
    private State stateAfterSP = State.READ_METHOD; // for process space

    // method & version
    private final char[] reqLineBuf = new char[8]; // because HTTP/1.1 has 8 char, so 8 is enough
    private int reqLineBufIndex = 0; // next can read or write index

    // path
    private final StringBuilder pathBuilder = new StringBuilder();

    // header
    private final HeaderCodec headerCodec;

    // body
    private final ByteBufferAllocator allocator;
    private BodyReader bodyReader = null;

    public HttpServerCodec(ByteBufferAllocator allocator) {
        this.headerCodec = new HeaderCodec();
        this.allocator = allocator;
    }

    @Override
    public void decode(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        try {
            while (buf.hasRemaining()) {
                buf.mark();
                byte b = buf.get();
                switch (state) {
                    case SKIP_SPACE -> skipSpace(b, buf);
                    case READ_METHOD -> readMethod(b, consumer);
                    case READ_PATH -> readPath(b, consumer);
                    case READ_VERSION -> readVer(b, buf, consumer);
                    case READ_HEADERS -> readHeaders(b, buf, consumer);
                    case READ_BODY -> readBody(buf, consumer);
                }
            }
        } catch (HttpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpProtocolException();
        }
    }

    @Override
    public ByteBuffer encode(HttpResponse response, ByteBufferAllocator allocator) {
        var body = response.getBody();
        var headers = response.getHeaders();
        int bodySize = 0;
        if (body != null) {
            bodySize = body.length;
            // add header
            headers.put(GeneralHttpHeader.CONTENT_LENGTH, String.valueOf(bodySize));
        }

        // to string
        var sb = new StringBuilder();
        sb.append(response.getVersion().getCode()).append(" ")
                .append(response.getStatusCode()).append(" ")
                .append(response.getReasonPhrase()).append("\r\n");
        response.getHeaders().forEach((k, v) -> {
            sb.append(k).append(":").append(v).append("\r\n");
        });
        sb.append("\r\n");

        // to buffer
        var messageBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        var buf = allocator.allocate(messageBytes.length + bodySize);
        buf.put(messageBytes);
        if (body != null) {
            buf.put(body);
        }
        buf.flip();
        return buf;
    }

    private void skipSpace(byte b, ByteBuffer buf) {
        if (b != ' ') {
            state = stateAfterSP;
            buf.reset(); // to last position
        }
    }

    private void readMethod(byte b, Consumer<HttpDecodeEvent> consumer) {
        if (reqLineBufIndex == 0) {
            if (b != 'G' && b != 'P' && b != 'D') {
                throw new HttpProtocolException();
            }
            reqLineBuf[reqLineBufIndex++] = (char) b;
            return;
        }

        switch (reqLineBuf[0]) {
            case 'G' -> {
                switch (reqLineBufIndex) {
                    case 1 -> decodeChar(b, 'E');
                    case 2 -> decodeChar(b, 'T');
                    case 3 -> decodeMethodCompleted(b, HttpMethod.GET, consumer);
                }
            }
            case 'D' -> {
                switch (reqLineBufIndex) {
                    case 1, 3, 5 -> decodeChar(b, 'E');
                    case 2 -> decodeChar(b, 'L');
                    case 4 -> decodeChar(b, 'T');
                    case 6 -> decodeMethodCompleted(b, HttpMethod.DELETE, consumer);
                }
            }
            case 'P' -> {
                if (reqLineBufIndex == 1) {
                    if (b != 'O' && b != 'U') {
                        throw new HttpProtocolException();
                    }
                    reqLineBuf[reqLineBufIndex++] = (char) b;
                    return;
                }
                switch (reqLineBuf[1]) {
                    case 'O' -> {
                        switch (reqLineBufIndex) {
                            case 2 -> decodeChar(b, 'S');
                            case 3 -> decodeChar(b, 'T');
                            case 4 -> decodeMethodCompleted(b, HttpMethod.POST, consumer);
                        }
                    }
                    case 'U' -> {
                        switch (reqLineBufIndex) {
                            case 2 -> decodeChar(b, 'T');
                            case 3 -> decodeMethodCompleted(b, HttpMethod.PUT, consumer);
                        }
                    }
                }
            }
        }
    }

    private void readPath(byte b, Consumer<HttpDecodeEvent> consumer) {
        if (b != ' ') {
            pathBuilder.append((char) b);
        } else {
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.PATH, pathBuilder.toString()));
            state = State.SKIP_SPACE;
            stateAfterSP = State.READ_VERSION;
            pathBuilder.setLength(0);
        }
    }

    private void readHeaders(byte b, ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        var headers = headerCodec.readHeaders(b, buf);
        if (headers != null) {
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.HEADERS, headers));
            int length = GeneralHttpHeader.getContentLength(headers);
            if (length == 0) {
                consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.END, null));
                reuse();
            } else {
                bodyReader = new BodyReader(allocator.allocate(length));
                state = State.READ_BODY;
            }
        }
    }

    private void readBody(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        buf.reset();
        var buffers = bodyReader.readFixLengthBody(buf);
        if (buffers != null) {
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.BODY, buffers));
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.END, null));
            reuse();
        }
    }

    private void readVer(byte b, ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        switch (reqLineBufIndex) {
            case 0 -> decodeChar(b, 'H');
            case 1, 2 -> decodeChar(b, 'T');
            case 3 -> decodeChar(b, 'P');
            case 4 -> decodeChar(b, '/');
            case 5, 7 -> decodeChar(b, '1');
            case 6 -> decodeChar(b, '.');
            case 8 -> {
                if (b == '\r') {
                    reqLineBufIndex++;
                } else if (b == '\n') {
                    endReqLine();
                    consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.VER, HttpVersion.HTTP1_1));
                } else {
                    throw new HttpProtocolException();
                }
            }
            case 9 -> {
                if (b != '\n') {
                    buf.reset();
                    endReqLine();
                    consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.VER, HttpVersion.HTTP1_1));
                }
            }
        }
    }

    private void decodeChar(byte b, char c) {
        if (b != c) {
            throw new HttpProtocolException();
        } else {
            reqLineBuf[reqLineBufIndex++] = (char) b;
        }
    }

    private void decodeMethodCompleted(byte b, HttpMethod method, Consumer<HttpDecodeEvent> consumer) {
        if (b != ' ') {
            throw new HttpProtocolException();
        }
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_PATH;
        consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.METHOD, method));

        // reuse
        reqLineBufIndex = 0;
    }

    private void endReqLine() {
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_HEADERS;
        reqLineBufIndex = 0;
    }

    private void reuse() {
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_METHOD;
        bodyReader = null;
    }
}
