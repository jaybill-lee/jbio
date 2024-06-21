package org.jaybill.jbio.core;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicInteger;

public class NioEventLoopGroup implements EventLoopGroup {

    private final NioEventLoop [] loops;
    private AtomicInteger counter = new AtomicInteger(0);

    public NioEventLoopGroup(int n, SelectorProvider provider) {
        loops = new NioEventLoop[n];
        for (int i = 0; i < n; i++) {
            loops[i] = new NioEventLoop(provider);
        }
    }

    @Override
    public NioEventLoop next() {
        return loops[counter.getAndIncrement() % loops.length];
    }
}
