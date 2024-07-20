package org.jaybill.jbio.core;

public interface ChannelHandlerContext extends ChannelHandlerInvoker {

    NioChannel channel();

    ChannelHandler handler();

    EventLoop eventloop();

    void attr(String k, Object v);

    Object attr(String k);
}
