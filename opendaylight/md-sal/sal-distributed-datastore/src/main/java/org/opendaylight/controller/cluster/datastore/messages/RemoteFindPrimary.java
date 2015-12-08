/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A remote message sent to locate the primary shard.
 *
 * @author Thomas Pantelis
 */
public class RemoteFindPrimary extends FindPrimary {
    private static final long serialVersionUID = 1L;

    private final Set<String> visitedAddresses;

    public RemoteFindPrimary(String shardName, boolean waitUntilReady, @Nonnull Collection<String> visitedAddresses) {
        super(shardName, waitUntilReady);
        this.visitedAddresses = new HashSet<>(Preconditions.checkNotNull(visitedAddresses));
    }

    @Nonnull
    public Set<String> getVisitedAddresses() {
        return visitedAddresses;
    }
}
