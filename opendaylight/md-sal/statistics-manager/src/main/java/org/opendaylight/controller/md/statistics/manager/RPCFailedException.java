/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;

import org.opendaylight.yangtools.yang.common.RpcError;

final class RPCFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Collection<RpcError> errors;

    public RPCFailedException(final String message, final Collection<RpcError> errors) {
        super(message);
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "RPCFailedException [errors=" + errors + ", message=" + getMessage() + ']';
    }
}
