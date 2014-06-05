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
 * applied into the datastore because of incorrect structure of user supplied
 * data.
 *
 */
public class IncorrectDataStructureException extends DataValidationFailedException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public IncorrectDataStructureException(final InstanceIdentifier path, final String message, final Throwable cause) {
        super(path, message, cause);
    }

    public IncorrectDataStructureException(final InstanceIdentifier path, final String message) {
        super(path, message);
    }

}
