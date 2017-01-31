/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;

class RemoteDOMRpcException extends DOMRpcException {

    private static final long serialVersionUID = 1L;

    RemoteDOMRpcException(final String message, final Throwable cause) {
        super(message,cause);
    }
}
