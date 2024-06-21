package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public class DefaultChannelPipeline implements ChannelPipeline {

    @Override
    public ChannelPipeline addLast(ChannelHandler handler) {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelInitialized() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelBound() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelRead(ByteBuffer buf) {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelClosed() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelDeregistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelException(Throwable t) {
        return this;
    }
}
