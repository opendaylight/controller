/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.io.Serializable;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Sent to the local {@link ShardedDataTreeActor} when there was a shard created
 * on the local node. The local actor should notify the remote actors with {@link PrefixShardCreated} which should
 * create the required frontend/backend shards.
 */
@Beta
@Deprecated(forRemoval = true)
public class LookupPrefixShard implements Serializable {
    private static final long serialVersionUID = 1L;

    private final DOMDataTreeIdentifier prefix;

    public LookupPrefixShard(final DOMDataTreeIdentifier prefix) {
        this.prefix = requireNonNull(prefix);
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }


    @Override
    public String toString() {
        return "LookupPrefixShard{"
                + "prefix="
                + prefix
                + '}';
    }
}
