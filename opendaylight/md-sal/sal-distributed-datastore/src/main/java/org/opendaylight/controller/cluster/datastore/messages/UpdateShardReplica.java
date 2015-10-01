/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import javax.annotation.Nonnull;

public class UpdateShardReplica {

    public static enum ShardReplicaAction {
        CREATE,
        REMOVE
    }

    private String shardName;
    private ShardReplicaAction action;

    public UpdateShardReplica (@Nonnull String shardName, ShardReplicaAction action){
        this.shardName = shardName;
        this.action = action;
    }

    public String getShardName(){
        return this.shardName;
    }

    public ShardReplicaAction getShardReplicateAction(){
        return this.action;
    }

    @Override
    public String toString(){
        final StringBuilder str = new StringBuilder("UpdateShardReplica[");
        str.append("shardName='").append(shardName);
        str.append("' action='").append(action.toString()).append("']");
        return str.toString();
    }
}

