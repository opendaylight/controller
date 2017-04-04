/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * A message sent to the ShardManager to dynamically add a new local shard
 *  that is a replica for an existing prefix shard that is already available
 *  in the cluster.
 */
public class AddPrefixShardReplica {

    private final YangInstanceIdentifier prefix;

    /**
     * Constructor.
     *
     * @param prefix prefix of the shard that is to be locally replicated.
     */

    public AddPrefixShardReplica(@Nonnull final YangInstanceIdentifier prefix) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix should not be null");
    }

    public YangInstanceIdentifier getShardPrefix() {
        return this.prefix;
    }

    @Override
    public String toString() {
        return "AddPrefixShardReplica[prefix=" + prefix + "]";
    }
}
