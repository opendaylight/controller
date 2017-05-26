/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

/**
 * This exception occurs if the datastore is temporarily unavailable.
 * A retry of the transaction may succeed after a period of time
 */

public class DataStoreUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public DataStoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }


}
