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

@Beta
public final class AbortedTransactionException extends RequestException {
    private static final long serialVersionUID = 1L;

    public AbortedTransactionException(final long lastKnownTransaction) {
        super("Transactions up to " + Long.toUnsignedString(lastKnownTransaction) + " have completed");
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
