package org.jaybill.jbio.core;

public interface ChannelInboundHandler extends ChannelHandler {

    void channelInitialized(ChannelHandlerContext ctx);

    void channelBound(ChannelHandlerContext ctx);

    void channelRegistered(ChannelHandlerContext ctx);

    void channelConnected(ChannelHandlerContext ctx);

    void channelDeregistered(ChannelHandlerContext ctx);

    void channelSendBufferFull(ChannelHandlerContext ctx);

    /**
     * When the number of bytes squeezed in SendBuffer is greater than or equals to {@link WriteBehavior#getHighWatermark()},
     * this method is triggered.
     */
    void channelUnWritable(ChannelHandlerContext ctx);

    /**
     * The channel is in an UnWritable state, and this method is triggered
     * when the number of bytes squeezed in SendBuffer is less than or equal to {@link WriteBehavior#getLowWatermark()}
     */
    void channelWritable(ChannelHandlerContext ctx);

    /**
     * It will be trigger once channel be closed. <br/>
     * If the channel has never been active, then this method will not be triggered
     */
    void channelClosed(ChannelHandlerContext ctx);

    void channelRead(ChannelHandlerContext ctx, Object o);

    /**
     * It will be trigger once channel occur exception. <br/>
     * If the channel has never been active, then this method will not be triggered
     */
    void channelException(ChannelHandlerContext ctx, Throwable t);
}
