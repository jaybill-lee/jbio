package org.jaybill.jbio.core;

public interface ChannelHandlerContext extends ChannelHandlerInvoker {

    NioChannel channel();

    ChannelHandler handler();

    EventLoop eventloop();
}
