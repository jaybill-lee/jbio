package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public class FixLengthByteBufferAllocateStrategy implements ByteBufferAllocateStrategy {

    private final ByteBufferAllocator delegate;

    public FixLengthByteBufferAllocateStrategy(ByteBufferAllocator delegate) {
        this.delegate = delegate;
    }

    @Override
    public ByteBuffer allocate() {
        return delegate.allocate(128);
    }
}
