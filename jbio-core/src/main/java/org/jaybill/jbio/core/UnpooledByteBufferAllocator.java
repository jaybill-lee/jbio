package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public class UnpooledByteBufferAllocator implements ByteBufferAllocator {

    @Override
    public ByteBuffer allocate(int capacity) {
        return ByteBuffer.allocate(capacity);
    }
}
