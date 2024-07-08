package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HttpDecodeEvent {
    public enum Type {
        // request
        METHOD, PATH,

        // response
        STATUS_CODE, REASON_PHRASE,

        // common
        VER, HEADERS, BODY
    }

    private Type type;
    private Object result;
}
