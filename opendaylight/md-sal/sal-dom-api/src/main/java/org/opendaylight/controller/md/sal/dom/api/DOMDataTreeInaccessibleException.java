/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;


/**
 * Failure reported when a data tree is no longer accessible.
 */
public class DOMDataTreeInaccessibleException extends DOMDataTreeListeningException {
    private static final long serialVersionUID = 1L;
    private final DOMDataTreeIdentifier treeIdentifier;

    public DOMDataTreeInaccessibleException(final DOMDataTreeIdentifier treeIdentifier, final String message) {
        super(message);
        this.treeIdentifier = Preconditions.checkNotNull(treeIdentifier);
    }

    public DOMDataTreeInaccessibleException(final DOMDataTreeIdentifier treeIdentifier, final String message, final Throwable cause) {
        super(message);
        this.treeIdentifier = Preconditions.checkNotNull(treeIdentifier);
    }

    public final DOMDataTreeIdentifier getTreeIdentifier() {
        return treeIdentifier;
    }
}
