package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DataPreconditionFailedException extends Exception {

    private final InstanceIdentifier path;

    public DataPreconditionFailedException(final InstanceIdentifier path) {
        this.path = path;
    }

    public DataPreconditionFailedException(final InstanceIdentifier path,final String message) {
        super(message);
        this.path = path;
    }


    public DataPreconditionFailedException(final InstanceIdentifier path,final Throwable cause) {
        super(cause);
        this.path = path;
    }

    public DataPreconditionFailedException(final InstanceIdentifier path,final String message, final Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    public DataPreconditionFailedException(final InstanceIdentifier path,final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.path = path;
    }

    public InstanceIdentifier getPath() {
        return path;
    }

}
