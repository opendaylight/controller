/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Documented exception occurrs when an error is thrown that is documented
 * in any RFC or draft for the specific protocol.
 *
 * @deprecated This exception no longer carries any special meaning. Users
 * are advised to stop using it and define their own replacement.
 */
@Deprecated
public class DocumentedException extends Exception  {

    private static final long serialVersionUID = -3727963789710833704L;

    /**
     * Creates a documented exception
     * @param message string
     */
    public DocumentedException(final String message) {
        super(message);
    }

    /**
     * Creates a documented exception
     * @param err string
     * @param cause the cause (which is saved for later retrieval by the
     * Throwable.getCause() method). (A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.)
     */
    public DocumentedException(final String err, final Exception cause) {
        super(err, cause);
    }
}
