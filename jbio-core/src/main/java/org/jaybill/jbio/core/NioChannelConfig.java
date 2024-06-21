package org.jaybill.jbio.core;

import lombok.Data;

import java.util.Map;

@Data
public class NioChannelConfig {
    private Map<SocketOption<?>, Object> options;
    private ByteBufferAllocator allocator;
    private ReadBehavior readBehavior;
    private WriteBehavior writeBehavior;
}
