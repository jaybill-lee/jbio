package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public interface ByteBufferAllocator {

    ByteBuffer allocate(int capacity);
}
