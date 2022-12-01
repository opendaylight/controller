/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a request to create a transaction which has
 * already been purged.
 */
public final class DeadTransactionException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final RangeSet<UnsignedLong> purgedIdentifiers;

    public DeadTransactionException(final RangeSet<UnsignedLong> purgedIdentifiers) {
        super("Transactions " + purgedIdentifiers + " have been purged");
        this.purgedIdentifiers = ImmutableRangeSet.copyOf(purgedIdentifiers);
    }

    @Override
    public boolean isRetriable() {
        return false;
    }

    public RangeSet<UnsignedLong> getPurgedIdentifier() {
        return purgedIdentifiers;
    }
}
