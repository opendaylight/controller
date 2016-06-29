/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a request referencing an unknown history. This
 * typically happens when the linear history ID is newer than the highest observed {@link CreateLocalHistoryRequest}.
 *
 * @author Robert Varga
 */
@Beta
public final class UnknownHistoryException extends RequestException {
    private static final long serialVersionUID = 1L;

    public UnknownHistoryException(final long lastSeenHistory) {
        super("Last known history is " + Long.toUnsignedString(lastSeenHistory));
    }

    @Override
    public boolean isRetriable() {
        return true;
    }
}
