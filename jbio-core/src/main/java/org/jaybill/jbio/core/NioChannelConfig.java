package org.jaybill.jbio.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class NioChannelConfig {
    private Map<SocketOption<?>, Object> options;
    private ByteBufferAllocator allocator;
    private ReadBehavior readBehavior;
    private WriteBehavior writeBehavior;

    public static final NioChannelConfig DEFAULT = new NioChannelConfig();
    static {
        DEFAULT.setAllocator(new UnpooledByteBufferAllocator());
        DEFAULT.setOptions(new HashMap<>());
        DEFAULT.setReadBehavior(new ReadBehavior(1, new AdaptiveByteBufferAllocateStrategy(DEFAULT.getAllocator())));
        DEFAULT.setWriteBehavior(new WriteBehavior(100, 100, 1024 * 1024 * 16, 1024 * 1024 * 8));
    }
}
