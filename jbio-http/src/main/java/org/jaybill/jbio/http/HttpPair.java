package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HttpPair {
    private HttpRequest request;
    private HttpResponse response;
}
