package org.jaybill.jbio.http;

import org.jaybill.jbio.http.ex.HttpProtocolException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HeaderCodec {
    enum HeaderState {
        SKIP_SPACE,
        READ_KEY,
        READ_COLON,
        READ_VAL
    }

    // initial state
    private HeaderState headerState = HeaderState.SKIP_SPACE;
    private HeaderState headerStateAfterSP = HeaderState.READ_KEY;
    
    // header
    private final StringBuilder headerKeyBuilder = new StringBuilder();
    private final StringBuilder headerValBuilder = new StringBuilder();
    private boolean headerKeyBeginWithCR = false;
    private boolean headerValLastReadCR = false;
    private final Map<String, String> headers = new HashMap<>();

    /**
     * read headers
     * @param b current byte
     * @param buf current ByteBuffer
     * @return null is not completed; else return headers map.
     */
    Map<String, String> readHeaders(byte b, ByteBuffer buf) {
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
                        return endHeaders();
                    } else {
                        headerKeyBuilder.append((char) b);
                    }
                } else if (headerKeyBeginWithCR){
                    if (b != '\n') {
                        buf.reset(); // to last pos
                    }
                    return endHeaders();
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
        return null;
    }

    void reuse() {
        // clear
        headerKeyBuilder.setLength(0);
        headerValBuilder.setLength(0);
        // next
        headerState = HeaderState.SKIP_SPACE;
        headerStateAfterSP = HeaderState.READ_KEY;
    }

    private void endHeaderLine() {
        headers.put(headerKeyBuilder.toString(), headerValBuilder.toString());
        reuse();
    }

    private Map<String, String> endHeaders() {
        reuse();
        return headers;
    }
}
