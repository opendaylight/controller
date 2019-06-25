/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import org.opendaylight.mdsal.dom.api.DOMActionException;

public class RemoteDOMActionException extends DOMActionException {
    private static final long serialVersionUID = 1L;

    RemoteDOMActionException(final String message, final Throwable cause) {
        super(message,cause);
    }
}
