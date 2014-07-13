/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.controller.md.sal.common.api.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;

/**
 * An exception for a failed read.
 */
public class ReadFailedException extends OperationFailedException {

    private static final long serialVersionUID = 1L;

    public ReadFailedException( String message, RpcError... errors ) {
        super( message, errors );
    }

    public ReadFailedException( String message, Throwable cause, RpcError... errors ) {
        super( message, cause, errors );
    }
}
