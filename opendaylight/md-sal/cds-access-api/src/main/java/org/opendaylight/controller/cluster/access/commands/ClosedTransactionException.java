/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a request for a transaction which has already
 * been closed, either via a successful commit or abort (which is indicated via {@link #isSuccessful()}. This can
 * happen if the corresponding journal record is replicated, but the message to the frontend gets lost and the backed
 * leader moved before the frontend retried the corresponding request.
 *
 * @author Robert Varga
 */
@Beta
public final class ClosedTransactionException extends RequestException {
    private static final long serialVersionUID = 1L;

    private final boolean successful;

    public ClosedTransactionException(final boolean successful) {
        super("Transaction has been " + (successful ? "committed" : "aborted"));
        this.successful = successful;
    }

    @Override
    public boolean isRetriable() {
        return false;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
