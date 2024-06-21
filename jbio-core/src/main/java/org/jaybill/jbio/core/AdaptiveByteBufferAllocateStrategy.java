package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public class AdaptiveByteBufferAllocateStrategy implements ByteBufferAllocateStrategy {

    private final ByteBufferAllocator delegate;

    public AdaptiveByteBufferAllocateStrategy(ByteBufferAllocator delegate) {
        this.delegate = delegate;
    }

    @Override
    public ByteBuffer allocate() {
        return delegate.allocate(1024);
    }
}
