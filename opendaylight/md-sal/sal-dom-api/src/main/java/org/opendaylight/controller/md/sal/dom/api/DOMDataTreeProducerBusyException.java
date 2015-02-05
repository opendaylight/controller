/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

/**
 * Exception indicating that the {@link DOMDataTreeProducer} has an open user
 * transaction and cannot be closed.
 */
public class DOMDataTreeProducerBusyException extends DOMDataTreeProducerException {
    private static final long serialVersionUID = 1L;

    public DOMDataTreeProducerBusyException(final String message) {
        super(message);
    }

    public DOMDataTreeProducerBusyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
