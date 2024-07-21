package org.jaybill.jbio.core.util;

import java.nio.ByteBuffer;

public class ByteBufferUtil {

    /**
     * read to string
     * @param buf
     * @return
     */
    public static String toString(ByteBuffer buf) {
        var sb = new StringBuilder();
        while (buf.hasRemaining()) {
            sb.append((char) buf.get());
        }
        return sb.toString();
    }
}
