package org.jaybill.jbio.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GeneralStatusCode {

    OK(200, "OK"),
    NOT_FOUND(404, "Not found"),
    INTERNAL_ERROR(500, "Internal error")
    ;

    private int code;
    private String reasonPhrase;
}
