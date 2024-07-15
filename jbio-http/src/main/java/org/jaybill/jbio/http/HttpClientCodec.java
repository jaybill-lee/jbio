package org.jaybill.jbio.http;

import org.jaybill.jbio.core.ByteBufferAllocator;
import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class HttpClientCodec implements HttpCodec<HttpRequest> {

    enum State {
        SKIP_SPACE,
        READ_VERSION,
        READ_STATUS_CODE,
        READ_REASON_PHRASE,
        READ_HEADERS,
        READ_BODY
    }

    private State state = State.SKIP_SPACE;
    private State stateAfterSP = State.READ_VERSION;

    // version
    private int versionIndex = 0;

    // status code
    private int statusCode = 0;
    private int statusCodeIndex = 0;

    // reason phrase
    private final StringBuilder reasonPhrase = new StringBuilder();
    private boolean readCR = false;

    // headers
    private final HeaderCodec headerCodec;

    // body
    private BodyReader bodyReader = null;

    public HttpClientCodec() {
        this.headerCodec = new HeaderCodec();
    }

    @Override
    public void decode(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        try {
            while (buf.hasRemaining()) {
                buf.mark();
                byte b = buf.get();
                switch (state) {
                    case SKIP_SPACE -> skipSpace(b, buf);
                    case READ_VERSION -> readVer(b, consumer);
                    case READ_STATUS_CODE -> readStatusCode(b, consumer);
                    case READ_REASON_PHRASE -> readReasonPhrase(b, buf, consumer);
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
    public ByteBuffer encode(HttpRequest request, ByteBufferAllocator allocator) {
        return null;
    }

    private void skipSpace(byte b, ByteBuffer buf) {
        if (b != ' ') {
            state = stateAfterSP;
            buf.reset(); // to last position
        }
    }

    private void readVer(byte b, Consumer<HttpDecodeEvent> consumer) {
        switch (versionIndex) {
            case 0 -> decodeChar(b, 'H');
            case 1, 2 -> decodeChar(b, 'T');
            case 3 -> decodeChar(b, 'P');
            case 4 -> decodeChar(b, '/');
            case 5, 7 -> decodeChar(b, '1');
            case 6 -> decodeChar(b, '.');
            case 8 -> {
                if (b != ' ') {
                    throw new HttpProtocolException();
                }
                state = State.SKIP_SPACE;
                stateAfterSP = State.READ_STATUS_CODE;
                consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.VER, HttpVersion.HTTP1_1));
                // reuse
                versionIndex = 0;
            }
        }
    }

    private void readStatusCode(byte b, Consumer<HttpDecodeEvent> consumer) {
        switch (statusCodeIndex) {
            case 0 -> decodeNumber(b, 5, 1);
            case 1, 2 -> decodeNumber(b, 9, 0);
            case 3 -> {
                if (b != ' ') {
                    throw new HttpProtocolException();
                }
                state = State.SKIP_SPACE;
                stateAfterSP = State.READ_REASON_PHRASE;
                consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.STATUS_CODE, statusCode));
                // reuse
                statusCodeIndex = 0;
                statusCode = 0;
            }
        }
    }

    private void readReasonPhrase(byte b, ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        if (readCR) {
            if (b != '\n') {
                buf.reset();
            }
            endReasonPhrase(consumer);
        } else {
            if (b == '\r') {
                readCR = true;
            } else if (b == '\n') {
                endReasonPhrase(consumer);
            } else {
                reasonPhrase.append((char) b);
            }
        }
    }

    private void readHeaders(byte b, ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        var headers = headerCodec.readHeaders(b, buf);
        if (headers != null) {
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.HEADERS, headers));
            bodyReader = new BodyReader(GeneralHttpHeader.getContentLength(headers));
            state = State.READ_BODY;
        }
    }

    private void readBody(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        buf.reset();
        var buffers = bodyReader.readBody(buf);
        if (buffers != null) {
            consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.BODY, buffers));
            reuse();
        }
    }

    private void decodeChar(byte b, char c) {
        if (b != c) {
            throw new HttpProtocolException();
        } else {
            versionIndex++;
        }
    }

    private void decodeNumber(byte b, int notGreaterThan, int notLessThan) {
        char c = (char) b;
        int n = c - '0';
        if (n > notGreaterThan || n < notLessThan) {
            throw new HttpProtocolException();
        }
        statusCodeIndex++;
        statusCode = statusCode * 10 + n;
    }

    private void endReasonPhrase(Consumer<HttpDecodeEvent> consumer) {
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_HEADERS;
        consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.REASON_PHRASE, reasonPhrase.toString()));
        // reuse
        reasonPhrase.setLength(0);
        readCR = false;
    }

    private void reuse() {
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_VERSION;
        bodyReader = null;
    }
}
