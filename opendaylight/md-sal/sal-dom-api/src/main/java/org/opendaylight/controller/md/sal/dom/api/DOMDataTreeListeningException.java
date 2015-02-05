/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

/**
 * Base exception for various causes why and {@link DOMDataTreeListener}
 * may be terminated by the {@link DOMDataTreeService} implementation.
 */
public abstract class DOMDataTreeListeningException extends Exception {
    private static final long serialVersionUID = 1L;

    protected DOMDataTreeListeningException(final String message) {
        super(message);
    }

    protected DOMDataTreeListeningException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
