/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen;

import com.google.common.base.Preconditions;

/**
 * Exception is raised when supplied Bidning Aware
 * RPCService class is not routed and was used in context
 * where routed RPCs should only be used.
 *
 */
public class RpcIsNotRoutedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public RpcIsNotRoutedException(final String message, final Throwable cause) {
        super(Preconditions.checkNotNull(message), cause);
    }

    public RpcIsNotRoutedException(final String message) {
        super(Preconditions.checkNotNull(message));
    }
}
