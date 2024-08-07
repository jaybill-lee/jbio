package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HttpVersion {
    HTTP1_1("HTTP/1.1"), HTTP2("HTTP2");

    private String code;
}
