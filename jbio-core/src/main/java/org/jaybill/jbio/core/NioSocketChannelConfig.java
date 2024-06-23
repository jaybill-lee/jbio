package org.jaybill.jbio.core;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class NioSocketChannelConfig extends NioChannelConfig {
    private ByteBufferAllocator allocator;
    private ReadBehavior readBehavior;
    private WriteBehavior writeBehavior;

    public static final NioSocketChannelConfig DEFAULT = new NioSocketChannelConfig();
    static {
        DEFAULT.setAllocator(new UnpooledByteBufferAllocator());
        DEFAULT.setOptions(new ConcurrentHashMap<>());
        DEFAULT.setReadBehavior(new ReadBehavior(1, new AdaptiveByteBufferAllocateStrategy(DEFAULT.getAllocator())));
        DEFAULT.setWriteBehavior(new WriteBehavior(100, 100, 1024 * 1024 * 16, 1024 * 1024 * 8));
    }
}
