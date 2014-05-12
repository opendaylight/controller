/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DataPreconditionFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 596430355175413427L;
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
