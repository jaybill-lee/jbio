package org.jaybill.jbio.core;

public interface SocketUnsafe extends Unsafe {

    void connect();

    void read();

    /**
     * write ByteBuffer to Channel
     * @param flush if true, ignore the OP_WRITE and always try to write to channel;
     */
    void write(boolean flush);
}
