package org.jaybill.jbio.http;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class BodyReader {

    private final Deque<ByteBuffer> buffers; // for trunk
    private final ByteBuffer fixLengthBodyBuf; // for content-length body

    BodyReader(ByteBuffer fixLengthBodyBuf) {
        this.buffers = new ArrayDeque<>();
        this.fixLengthBodyBuf = fixLengthBodyBuf;
    }

    /**
     * read fix length body to a new buffer
     * @param src
     * @return not null if read completed
     */
    Deque<ByteBuffer> readFixLengthBody(ByteBuffer src) {
        var remaining = fixLengthBodyBuf.remaining();
        var srcRemaining = src.remaining();
        fixLengthBodyBuf.put(src.duplicate()
                .position(src.position())
                .limit(srcRemaining > remaining ? src.position() + remaining : src.limit()));
        if (fixLengthBodyBuf.remaining() != 0) {
            return null;
        }

        // read completed
        buffers.add(fixLengthBodyBuf);
        return buffers;
    }
}
