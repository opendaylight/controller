/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a Request whose sequence does not match the
 * next expected sequence for the target. This is a hard error, as it indicates a Request is missing in the stream.
 */
public final class OutOfOrderRequestException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public OutOfOrderRequestException(final long expectedRequest) {
        super("Expecting request " + Long.toUnsignedString(expectedRequest));
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
