/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "cluster-admin", name = "add-shard-replica", description = "Run an add-shard-replica test")
public class AddShardReplicaCommand extends AbstractRpcAction {
    @Reference
    private RpcConsumerRegistry rpcConsumerRegistry;
    @Argument(index = 0, name = "shard-name", required = true)
    private String shardName;
    @Argument(index = 1, name = "data-store-type", required = true, description = "config / operational")
    private String dataStoreType;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcConsumerRegistry.getRpcService(ClusterAdminService.class)
            .addShardReplica(new AddShardReplicaInputBuilder()
                .setShardName(shardName)
                .setDataStoreType(DataStoreType.forName(dataStoreType).orElse(null))
                .build());
    }
}
