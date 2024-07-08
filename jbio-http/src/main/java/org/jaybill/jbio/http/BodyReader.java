package org.jaybill.jbio.http;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class BodyReader {

    private final Deque<ByteBuffer> buffers;
    private final int expectLen;
    private int currentLen;

    BodyReader(int expectLen) {
        this.buffers = new ArrayDeque<>();
        this.expectLen = expectLen;
    }

    /**
     * read body
     * @param buf
     * @return not null if read completed
     */
    Deque<ByteBuffer> readBody(ByteBuffer buf) {
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
                return buffers;
            } else {
                var limit = buf.limit();
                var newBuf = buf.duplicate();
                newBuf.position(buf.position());
                newBuf.limit(limit);
                buf.position(limit);
                currentLen = len;
                buffers.add(newBuf);
                if (currentLen == expectLen) {
                    return buffers;
                }
            }
        } else {
            return buffers;
        }
        return null;
    }
}
