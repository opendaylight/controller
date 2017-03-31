/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Sent to the local {@link ShardedDataTreeActor} to initiate the lookup of the shard, once the shard is removed from
 * the system entirely the actor responds with a success.
 */
public class PrefixShardRemovalLookup {

    private final DOMDataTreeIdentifier prefix;

    public PrefixShardRemovalLookup(final DOMDataTreeIdentifier prefix) {

        this.prefix = Preconditions.checkNotNull(prefix);
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return "PrefixShardRemovalLookup{"
                + "prefix=" + prefix
                + '}';
    }
}
