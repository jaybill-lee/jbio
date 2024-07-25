package org.jaybill.jbio.core;

import lombok.extern.slf4j.Slf4j;

import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NioEventLoopGroup implements EventLoopGroup {

    private final NioEventLoop [] loops;
    private final AtomicInteger counter = new AtomicInteger(0);

    public NioEventLoopGroup(int n, SelectorProvider provider, String namePrefix) {
        loops = new NioEventLoop[n];
        for (int i = 0; i < n; i++) {
            loops[i] = new NioEventLoop(provider, namePrefix);
        }
    }

    @Override
    public NioEventLoop next() {
        return loops[counter.getAndIncrement() % loops.length];
    }

    @Override
    public CompletableFuture<Void> close() {
        var futureList = new ArrayList<CompletableFuture<Void>>();
        for (var eventloop : loops) {
            try {
                futureList.add(eventloop.close());
            } catch (Throwable e) {
                log.debug("eventloop close error:", e);
            }
        }
        return CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
    }
}
