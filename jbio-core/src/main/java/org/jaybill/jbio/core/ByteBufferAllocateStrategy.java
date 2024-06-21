package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public interface ByteBufferAllocateStrategy {

    ByteBuffer allocate();
}
