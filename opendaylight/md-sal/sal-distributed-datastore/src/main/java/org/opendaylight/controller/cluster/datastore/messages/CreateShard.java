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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.ShardPropsCreator;

/**
 * A message sent to the ShardManager to dynamically create a new shard.
 *
 * @author Thomas Pantelis
 */
public class CreateShard {
    private final String shardName;
    private final Collection<String> memberNames;
    private final ShardPropsCreator shardPropsCreator;
    private final DatastoreContext datastoreContext;

    /**
     * Constructor.
     *
     * @param shardName the name of the new shard.
     * @param memberNames the names of all the member replicas.
     * @param shardPropsCreator used to obtain the Props for creating the shard actor instance.
     * @param datastoreContext the DatastoreContext for the new shard. If null, the default is used.
     */
    public CreateShard(@Nonnull String shardName, @Nonnull Collection<String> memberNames,
            @Nonnull ShardPropsCreator shardPropsCreator, @Nullable DatastoreContext datastoreContext) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.memberNames = Preconditions.checkNotNull(memberNames);
        this.shardPropsCreator = Preconditions.checkNotNull(shardPropsCreator);
        this.datastoreContext = datastoreContext;
    }

    @Nonnull public String getShardName() {
        return shardName;
    }

    @Nonnull public Collection<String> getMemberNames() {
        return memberNames;
    }

    @Nonnull public ShardPropsCreator getShardPropsCreator() {
        return shardPropsCreator;
    }

    @Nullable public DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreateShard [shardName=").append(shardName).append(", memberNames=").append(memberNames)
                .append("]");
        return builder.toString();
    }
}
