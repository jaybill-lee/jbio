package org.jaybill.jbio.core;

public interface EventLoopGroup {
    EventLoop next();

    void close();
}
