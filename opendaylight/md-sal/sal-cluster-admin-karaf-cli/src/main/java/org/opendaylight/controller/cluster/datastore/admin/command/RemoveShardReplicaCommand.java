/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaInputBuilder;

@Service
@Command(scope = "cluster-admin", name = "remove-shard-replica", description = "Run a remove-shard-replica")
public class RemoveShardReplicaCommand implements Action {

    @Reference
    private ClusterAdminService clusterAdminService;

    @Argument(index = 0, name = "shard-name", required = true)
    String shardName;

    @Argument(index = 1, name = "data-store-type", required = true)
    DataStoreType dataStoreType;

    @Argument(index = 2, name = "member-name", required = true)
    String memberName;

    @Override
    public Object execute() throws Exception {
        return clusterAdminService
                .removeShardReplica(new RemoveShardReplicaInputBuilder()
                    .setShardName(shardName)
                    .setDataStoreType(dataStoreType)
                    .setMemberName(memberName)
                    .build())
                .get()
                .getResult();
    }
}
