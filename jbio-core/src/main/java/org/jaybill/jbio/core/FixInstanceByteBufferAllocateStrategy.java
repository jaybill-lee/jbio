package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Each call to allocate() returns a fixed instance, which can reduce memory allocation overhead.
 */
public class FixInstanceByteBufferAllocateStrategy implements ByteBufferAllocateStrategy {

    private final ByteBufferAllocator delegate;
    private final Lock lock;
    private ByteBuffer readByteBuf;

    public FixInstanceByteBufferAllocateStrategy(ByteBufferAllocator delegate) {
        this.delegate = delegate;
        this.lock = new ReentrantLock();
    }

    @Override
    public ByteBuffer allocate() {
        tryAllocate();
        readByteBuf.clear(); // make sure the byte buffer is clear
        return readByteBuf;
    }

    private void tryAllocate() {
        if (readByteBuf == null) {
            try {
                lock.lock();
                if (readByteBuf == null) {
                    readByteBuf = delegate.allocate(128);
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
