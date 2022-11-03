/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.Serial;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a request referencing an unknown history. This
 * typically happens when the linear history ID is newer than the highest observed {@link CreateLocalHistoryRequest}.
 */
public final class UnknownHistoryException extends RequestException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UnknownHistoryException(final Long lastSeenHistory) {
        super("Last known history is " + historyToString(lastSeenHistory));
    }

    private static String historyToString(final Long history) {
        return history == null ? "null" : Long.toUnsignedString(history);
    }

    @Override
    public boolean isRetriable() {
        return true;
    }
}
