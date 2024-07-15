package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

/**
 * When eventloop reads(), it uses this strategy to allocate an ByteBuffer to receive data read from the Channel.
 */
public interface ByteBufferAllocateStrategy {

    /**
     * allocate a ByteBuffer
     */
    ByteBuffer allocate();
}
