/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

/**
 * Exception reported when no RPC implementation is found in the system.
 *
 * @deprecated Use {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException} instead.
 */
@Deprecated
public class RpcImplementationUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RpcImplementationUnavailableException(final String message) {
        super(message);
    }

    public RpcImplementationUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
