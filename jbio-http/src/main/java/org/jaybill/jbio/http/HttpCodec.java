package org.jaybill.jbio.http;

import org.jaybill.jbio.core.ByteBufferAllocator;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface HttpCodec<T extends HttpMessage> {

    /**
     * decode the HTTP request or response
     * @param buf request or response bytebuffer
     * @param consumer consume decode events
     */
    void decode(ByteBuffer buf, Consumer<HttpDecodeEvent> consumer);

    /**
     * encode the HTTP request or response
     * @param message request or response
     * @return
     */
    ByteBuffer encode(T message, ByteBufferAllocator allocator);
}
