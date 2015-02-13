/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Exception indicating that no implementation of the requested RPC service is available.
 */
public class DOMRpcImplementationNotAvailableException extends DOMRpcException {
    private static final long serialVersionUID = 1L;

    public DOMRpcImplementationNotAvailableException(@Nonnull final String format, final Object... args) {
        super(String.format(format, args));
    }

    public DOMRpcImplementationNotAvailableException(@Nonnull final Throwable cause, @Nonnull final String format, final Object... args) {
        super(String.format(format, args), Preconditions.checkNotNull(cause));
    }
}
