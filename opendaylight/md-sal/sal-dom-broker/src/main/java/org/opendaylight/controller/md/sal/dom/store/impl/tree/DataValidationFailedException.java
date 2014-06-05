/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 * Exception thrown when a proposed change fails validation before being
 * applied into the datastore. This can have multiple reasons, for example
 * the datastore has been concurrently modified such that a conflicting
 * node is present, or the modification is structurally incorrect.
 */
public class DataValidationFailedException extends Exception {
    private static final long serialVersionUID = 1L;
    private final InstanceIdentifier path;

    /**
     * Create a new instance.
     *
     * @param path Object path which caused this exception
     * @param message Specific message describing the failure
     */
    public DataValidationFailedException(final InstanceIdentifier path, final String message) {
        this(path, message, null);
    }
    /**
     * Create a new instance, initializing
     *
     * @param path Object path which caused this exception
     * @param message Specific message describing the failure
     * @param cause Exception which triggered this failure, may be null
     */
    public DataValidationFailedException(final InstanceIdentifier path, final String message, final Throwable cause) {
        super(message, cause);
        this.path = Preconditions.checkNotNull(path);
    }

    /**
     * Returns the offending object path.
     *
     * @return Path of the offending object
     */
    public InstanceIdentifier getPath() {
        return path;
    }
}
