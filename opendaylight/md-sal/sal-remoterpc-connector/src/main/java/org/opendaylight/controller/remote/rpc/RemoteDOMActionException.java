package org.opendaylight.controller.remote.rpc;

import org.opendaylight.mdsal.dom.api.DOMActionException;

public class RemoteDOMActionException extends DOMActionException {
    private static final long serialVersionUID = 1L;

    RemoteDOMActionException(final String message, final Throwable cause) {
        super(message,cause);
    }
}
