package org.jaybill.jbio.core;

import lombok.Data;

@Data
public class NioSocketChannelConfig extends NioChannelConfig {
    private ByteBufferAllocator allocator;
    private ReadBehavior readBehavior;
    private WriteBehavior writeBehavior;
}
