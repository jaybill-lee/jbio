package org.jaybill.jbio.http;

import java.util.Map;

public class GeneralHttpHeader {
    public static final String CONTENT_LENGTH = "Content-Length";

    public static int getContentLength(Map<String, String> headers) {
        var lenStr = headers.get(GeneralHttpHeader.CONTENT_LENGTH);
        if (lenStr == null) {
            return  0;
        } else {
            return Integer.parseInt(lenStr);
        }
    }
}
