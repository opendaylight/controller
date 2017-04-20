/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

/**
 * An exception indicating a message slice failure.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceException extends Exception {
    private static final long serialVersionUID = 1L;

    private final boolean isRetriable;

    public MessageSliceException(final String message, final Throwable cause) {
        super(message, cause);
        isRetriable = false;
    }

    public MessageSliceException(final String message, final boolean isRetriable) {
        super(message);
        this.isRetriable = isRetriable;
    }

    public boolean isRetriable() {
        return isRetriable;
    }
}
