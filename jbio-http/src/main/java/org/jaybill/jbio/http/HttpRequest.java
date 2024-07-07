package org.jaybill.jbio.http;

import java.nio.ByteBuffer;
import java.util.Map;

public class HttpRequest {
    private HttpMethod method;
    private String path;
    private HttpVersion version;
    private Map<String, String> headers;
    private ByteBuffer body;
}
