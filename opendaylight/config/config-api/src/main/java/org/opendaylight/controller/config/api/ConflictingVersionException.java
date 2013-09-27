/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

/**
 * Can be thrown during
 * {@link ConfigRegistry#commitConfig(javax.management.ObjectName)} to indicate
 * that the transaction cannot be committed due to the fact that another
 * transaction was committed after creating this transaction. Clients can create
 * new transaction and merge the changes.
 */
public class ConflictingVersionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictingVersionException() {
        super();
    }

    public ConflictingVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictingVersionException(String message) {
        super(message);
    }

    public ConflictingVersionException(Throwable cause) {
        super(cause);
    }

}
