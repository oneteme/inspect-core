package org.usf.inspect.core;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(-1000),
    TIMEOUT_OR_INTERRUPTION(-1001),
    CONNECTION_UNAVAILABLE(-1002),
    UNKNOWN_ERROR(-1003),
    AUTHENTIFICATION_ERROR(-1004);



    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

}