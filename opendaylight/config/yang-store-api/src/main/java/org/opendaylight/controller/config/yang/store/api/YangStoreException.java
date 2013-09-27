/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.api;

public class YangStoreException extends Exception {

    private static final long serialVersionUID = 2841238836278528836L;

    public YangStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
