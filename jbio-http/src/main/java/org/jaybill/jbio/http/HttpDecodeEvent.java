package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HttpDecodeEvent {
    public enum Type {
        METHOD, PATH, VER, HEADERS, BODY
    }

    private Type type;
    private Object result;
}
