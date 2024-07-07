package org.jaybill.jbio.http;

import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HttpDecoder {

    enum State {
        SKIP_SPACE,
        READ_METHOD,
        READ_PATH,
        READ_VERSION,
        READ_HEADERS,
        READ_BODY
    }

    enum HeaderState {
        SKIP_SPACE,
        READ_KEY,
        READ_COLON,
        READ_VAL
    }

    // state machine
    private State state = State.READ_METHOD;
    private State stateAfterSP; // for process space
    private HeaderState headerState;
    private HeaderState headerStateAfterSP;

    // method & version
    private char[] reqLineBuf = new char[8]; // because HTTP/1.1 has 8 char, so 8 is enough
    private int reqLineBufIndex = 0; // next can read or write index

    // path
    private StringBuilder pathBuilder = new StringBuilder();

    // header
    private StringBuilder headerKeyBuilder = new StringBuilder();
    private StringBuilder headerValBuilder = new StringBuilder();
    private boolean headerKeyBeginWithCR = false;
    private boolean headerValLastReadCR = false;
    private Map<String, String> headers = new HashMap<>();

    // body
    private Deque<ByteBuffer> buffers;
    private int currentLen;
    private int expectLen;

    public HttpDecoder() {
        this.buffers = new ArrayDeque<>();
    }

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
        switch (headerState) {
            case SKIP_SPACE -> {
                if (b != ' ') {
                    headerState = headerStateAfterSP;
                    buf.reset();
                }
            }
            case READ_KEY -> {
                headerValLastReadCR = false;
                if (headerKeyBuilder.isEmpty()) {
                    if (b == '\r') {
                        headerKeyBeginWithCR = true;
                    } else if (b == '\n') {
                        endHeaders(consumer);
                    } else {
                        headerKeyBuilder.append((char) b);
                    }
                } else if (headerKeyBeginWithCR){
                    if (b != '\n') {
                        buf.reset(); // to last pos
                    }
                    endHeaders(consumer);
                } else {
                    if (b == ' ') {
                        headerState = HeaderState.SKIP_SPACE;
                        headerStateAfterSP = HeaderState.READ_COLON;
                    } else if (b == ':') {
                        headerState = HeaderState.READ_COLON;
                        buf.reset();
                    } else {
                        if (b == '\r' || b == '\n') {
                            throw new HttpProtocolException();
                        }
                        headerKeyBuilder.append((char) b);
                    }
                }
            }
            case READ_COLON -> {
                if (b != ':') {
                    throw new HttpProtocolException();
                }
                headerState = HeaderState.SKIP_SPACE;
                headerStateAfterSP = HeaderState.READ_VAL;
            }
            case READ_VAL -> {
                if (headerValLastReadCR) {
                    if (b != '\n') {
                        buf.reset();
                    }
                    endHeaderLine();
                } else {
                    if (b == '\r') {
                        headerValLastReadCR = true;
                    } else if (b == '\n') {
                        endHeaderLine();
                    } else {
                        headerValBuilder.append((char) b);
                    }
                }
            }
        }
    }

    private void readBody(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        buf.reset();
        if (currentLen < expectLen) {
            int len = currentLen + buf.remaining();
            if (len > expectLen) {
                int newLimit = buf.position() + (expectLen - currentLen);
                var newBuf = buf.duplicate();
                newBuf.position(buf.position());
                newBuf.limit(newLimit);
                buf.position(newLimit);
                currentLen = expectLen;
                buffers.add(newBuf);
                consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.BODY, buffers));
            } else {
                var limit = buf.limit();
                var newBuf = buf.duplicate();
                newBuf.position(buf.position());
                newBuf.limit(limit);
                buf.position(limit);
                currentLen = len;
                buffers.add(newBuf);
                if (currentLen == expectLen) {
                    consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.BODY, buffers));
                    reuse();
                }
            }
        } else {
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
        consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.METHOD, method.name()));

        // reuse
        reqLineBufIndex = 0;
    }

    private void endReqLine() {
        state = State.SKIP_SPACE;
        stateAfterSP = State.READ_HEADERS;
        headerState = HeaderState.SKIP_SPACE;
        headerStateAfterSP = HeaderState.READ_KEY;
        reqLineBufIndex = 0;
    }

    private void endHeaderLine() {
        headers.put(headerKeyBuilder.toString(), headerValBuilder.toString());
        // clear
        headerKeyBuilder.setLength(0);
        headerValBuilder.setLength(0);
        // next
        headerState = HeaderState.SKIP_SPACE;
        headerStateAfterSP = HeaderState.READ_KEY;
    }

    private void endHeaders(Consumer<HttpDecodeEvent> consumer) {
        var lenStr = headers.get(GeneralHttpHeader.CONTENT_LENGTH);
        if (lenStr == null) {
            expectLen = 0;
        } else {
            expectLen = Integer.parseInt(lenStr);
        }

        state = State.READ_BODY;
        consumer.accept(new HttpDecodeEvent(HttpDecodeEvent.Type.HEADERS, headers));
        headerKeyBuilder.setLength(0);
        headerValBuilder.setLength(0);
    }

    private void reuse() {
        state = State.READ_METHOD;
        stateAfterSP = null;
        headerState = null;
        headerStateAfterSP = null;
        headerKeyBeginWithCR = false;
        headerValLastReadCR = false;
    }
}