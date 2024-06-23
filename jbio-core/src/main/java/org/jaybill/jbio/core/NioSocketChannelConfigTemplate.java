package org.jaybill.jbio.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class NioSocketChannelConfigTemplate extends NioChannelConfigTemplate {
    private Class<? extends ByteBufferAllocator> allocatorCls;

    // read behavior
    private int maxReadCountPerLoop;
    private Class<? extends ByteBufferAllocateStrategy> strategyCls;

    // write behavior
    private int spinCount;
    private int maxWritePerLoop;
    private int highWatermark;
    private int lowWatermark;

    public static final NioSocketChannelConfigTemplate DEFAULT = new NioSocketChannelConfigTemplate();
    static {
        DEFAULT.setOptions(new ConcurrentHashMap<>());
        DEFAULT.setAllocatorCls(UnpooledByteBufferAllocator.class);

        // read
        DEFAULT.setMaxReadCountPerLoop(100);
        DEFAULT.setStrategyCls(AdaptiveByteBufferAllocateStrategy.class);

        // write
        DEFAULT.setSpinCount(100);
        DEFAULT.setMaxWritePerLoop(100);
        DEFAULT.setHighWatermark(1024 * 1024 * 16);
        DEFAULT.setLowWatermark(1024 * 1024 * 8);
    }

    @Override
    public NioSocketChannelConfig create() {
        var config = new NioSocketChannelConfig();
        // options
        var optionMap = new ConcurrentHashMap<SocketOption<?>, Object>();
        config.setOptions(optionMap);
        if (options != null) {
            optionMap.putAll(options);
        }

        // allocator
        ByteBufferAllocator allocator;
        try {
            allocator = allocatorCls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("can not create allocator, use default", e);
            allocator = new UnpooledByteBufferAllocator();
        }
        config.setAllocator(allocator);

        // read behavior
        var readBehavior = new ReadBehavior();
        readBehavior.setMaxReadCountPerLoop(maxReadCountPerLoop);
        ByteBufferAllocateStrategy strategy;
        try {
            strategy = strategyCls.getDeclaredConstructor(ByteBufferAllocator.class).newInstance(allocator);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("can not create strategy, use default", e);
            strategy = new AdaptiveByteBufferAllocateStrategy(allocator);
        }
        readBehavior.setStrategy(strategy);
        config.setReadBehavior(readBehavior);

        // write behavior
        var writeBehavior = new WriteBehavior();
        writeBehavior.setSpinCount(spinCount);
        writeBehavior.setMaxWritePerLoop(maxWritePerLoop);
        writeBehavior.setLowWatermark(lowWatermark);
        writeBehavior.setHighWatermark(highWatermark);
        config.setWriteBehavior(writeBehavior);
        return config;
    }
}
