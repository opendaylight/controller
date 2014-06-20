/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * Exception thrown when a proposed change fails validation before being
 * applied into the Data Tree because the Data Tree has been modified
 * in way that a conflicting
 * node is present.
 */
public class ConflictingModificationAppliedException extends DataValidationFailedException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConflictingModificationAppliedException(final InstanceIdentifier path, final String message, final Throwable cause) {
        super(path, message, cause);
    }

    public ConflictingModificationAppliedException(final InstanceIdentifier path, final String message) {
        super(path, message);
    }

}
