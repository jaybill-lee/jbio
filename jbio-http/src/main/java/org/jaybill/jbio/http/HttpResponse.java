package org.jaybill.jbio.http;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class HttpResponse extends HttpMessage {
    private Integer requestId;
    @Getter
    @Setter
    private HttpVersion version;
    @Getter
    @Setter
    private int statusCode;
    @Getter
    @Setter
    private String reasonPhrase;
    @Getter
    @Setter
    private byte[] body;

    void requestId(int requestId) {
        this.requestId = requestId;
    }

    int requestId() {
        return this.requestId;
    }
}
