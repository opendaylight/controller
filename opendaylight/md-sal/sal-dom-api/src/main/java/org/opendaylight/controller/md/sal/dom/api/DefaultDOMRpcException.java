/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

/**
 * Default implementation of DOMRpcException.
 *
 * @author Thomas Pantelis
 */
public class DefaultDOMRpcException extends DOMRpcException {
    private static final long serialVersionUID = 1L;

    public DefaultDOMRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
