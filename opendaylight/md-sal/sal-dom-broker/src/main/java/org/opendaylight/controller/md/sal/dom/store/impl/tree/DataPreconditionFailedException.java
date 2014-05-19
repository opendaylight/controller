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

public class DataPreconditionFailedException extends Exception {
    private static final long serialVersionUID = 1L;
    private final InstanceIdentifier path;

    public DataPreconditionFailedException(final InstanceIdentifier path, final String message) {
        super(message);
        this.path = Preconditions.checkNotNull(path);
    }

    public DataPreconditionFailedException(final InstanceIdentifier path, final String message, final Throwable cause) {
        super(message, cause);
        this.path = Preconditions.checkNotNull(path);
    }

    public InstanceIdentifier getPath() {
        return path;
    }
}
