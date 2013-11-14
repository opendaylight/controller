package org.opendaylight.controller.sal.rest.impl;

public class UnsupportedJsonFormatException extends Exception {

    private static final long serialVersionUID = -1741388894406313402L;

    public UnsupportedJsonFormatException() {
        super();
    }

    public UnsupportedJsonFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedJsonFormatException(String message) {
        super(message);
    }

    public UnsupportedJsonFormatException(Throwable cause) {
        super(cause);
    }

}
