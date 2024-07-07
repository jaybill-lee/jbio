package org.jaybill.jbio.http;

import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class HttpClientCodec {

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

    private final HeaderCodec headerCodec;

    public HttpClientCodec() {
        this.headerCodec = new HeaderCodec();
    }

    public void decode(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer) {
        try {
            while (buf.hasRemaining()) {
                buf.mark();
                byte b = buf.get();
                switch (state) {
                    case SKIP_SPACE -> {}
                    case READ_VERSION -> {}
                    case READ_STATUS_CODE -> {}
                    case READ_REASON_PHRASE -> {}
                    case READ_HEADERS -> {}
                    case READ_BODY -> {}
                }
            }
        } catch (HttpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpProtocolException();
        }
    }
}
