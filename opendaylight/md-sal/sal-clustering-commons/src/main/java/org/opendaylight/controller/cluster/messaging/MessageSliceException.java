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

    /**
     * Constructs an instance.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public MessageSliceException(final String message, final Throwable cause) {
        super(message, cause);
        isRetriable = false;
    }

    /**
     * Constructs an instance.
     *
     * @param message the detail message
     * @param isRetriable if true, indicates the original operation can be retried
     */
    public MessageSliceException(final String message, final boolean isRetriable) {
        super(message);
        this.isRetriable = isRetriable;
    }

    /**
     * Returns whether or not the original operation can be retried.
     *
     * @return true if it can be retried, false otherwise
     */
    public boolean isRetriable() {
        return isRetriable;
    }
}
