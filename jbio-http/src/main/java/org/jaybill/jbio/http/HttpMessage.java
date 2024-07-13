package org.jaybill.jbio.http;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpMessage {
    private Map<String, String> headers;

    public HttpMessage() {
        this.headers = new HashMap<>();
    }
}
