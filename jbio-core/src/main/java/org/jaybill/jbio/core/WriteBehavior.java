package org.jaybill.jbio.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WriteBehavior {
    /**
     * After the TCP send buffer is full, retry several times to reduce latency.
     */
    private int spinCount;

    /**
     * The maximum number of ByteBuffer writes per write operation
     */
    private int maxBufferNumPerWrite;

    /**
     * When the number of bytes squeezed in SendBuffer is greater than or equals to highWatermark,
     * trigger {@link ChannelInboundHandler#channelUnWritable(ChannelHandlerContext)}.
     */
    private int highWatermark;

    /**
     * The channel is in an UnWritable state,
     * when the number of bytes squeezed in SendBuffer is less than or equal to lowWatermark,
     * trigger {@link ChannelInboundHandler#channelWritable(ChannelHandlerContext)}
     */
    private int lowWatermark;

}
