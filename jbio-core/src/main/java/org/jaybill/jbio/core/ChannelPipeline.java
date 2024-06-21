package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public interface ChannelPipeline {

    ChannelPipeline addLast(ChannelHandler handler);
    ChannelPipeline fireChannelInitialized();
    ChannelPipeline fireChannelBound();
    ChannelPipeline fireChannelRegistered();
    ChannelPipeline fireChannelActive();
    ChannelPipeline fireChannelRead(ByteBuffer buf);
    ChannelPipeline fireChannelClosed();
    ChannelPipeline fireChannelInactive();
    ChannelPipeline fireChannelDeregistered();
    ChannelPipeline fireChannelException(Throwable t);
}
