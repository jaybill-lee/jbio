package org.jaybill.jbio.http;

import lombok.Data;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.Queue;

@Data
@ToString(callSuper = true)
public class HttpRequest extends HttpMessage {
    private HttpMethod method;
    private String path;
    private HttpVersion version;
    private Queue<ByteBuffer> body;
}
