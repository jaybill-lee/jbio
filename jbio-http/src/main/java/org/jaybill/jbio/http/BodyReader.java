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
        var dupSrc = src.duplicate()
                .position(src.position())
                .limit(srcRemaining > remaining ? src.position() + remaining : src.limit());
        fixLengthBodyBuf.put(dupSrc);
        if (fixLengthBodyBuf.remaining() != 0) {
            return null;
        }

        // update src pos
        src.position(dupSrc.position());

        // read completed
        fixLengthBodyBuf.flip();
        buffers.add(fixLengthBodyBuf);
        return buffers;
    }
}
