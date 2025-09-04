/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A remote message sent to locate the primary shard.
 *
 * @author Thomas Pantelis
 */
public final class RemoteFindPrimary extends FindPrimary {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final Set<String> visitedAddresses;

    public RemoteFindPrimary(final String shardName, final boolean waitUntilReady,
            final @NonNull Collection<String> visitedAddresses) {
        super(shardName, waitUntilReady);
        this.visitedAddresses = new HashSet<>(requireNonNull(visitedAddresses));
    }

    // FIXME: leaks mutable state
    public @NonNull Set<String> getVisitedAddresses() {
        return visitedAddresses;
    }
}
