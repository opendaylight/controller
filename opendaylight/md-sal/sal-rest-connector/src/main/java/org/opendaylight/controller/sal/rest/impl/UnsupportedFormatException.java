/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

public class UnsupportedFormatException extends Exception {

    private static final long serialVersionUID = -1741388894406313402L;

    public UnsupportedFormatException() {
        super();
    }

    public UnsupportedFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFormatException(String message) {
        super(message);
    }

    public UnsupportedFormatException(Throwable cause) {
        super(cause);
    }

}
