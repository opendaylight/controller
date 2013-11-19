package org.opendaylight.controller.sal.rest.impl;

public class UnsupportedFormatException extends Exception {

    private static final long serialVersionUID = -1741388894406313402L;

    public UnsupportedFormatException() {
        super();
    }

    public UnsupportedFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFormatException(String message) {
        super(message);
    }

    public UnsupportedFormatException(Throwable cause) {
        super(cause);
    }

}
