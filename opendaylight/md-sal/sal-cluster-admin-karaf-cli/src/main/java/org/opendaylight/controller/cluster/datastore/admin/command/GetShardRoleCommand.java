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
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.GetShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.GetShardRoleInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ShardName;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "cluster-admin", name = "get-shard-role", description = "Run a get-shard-role test")
public class GetShardRoleCommand extends AbstractRpcAction {
    @Reference
    private RpcService rpcService;
    @Argument(index = 0, name = "shard-name", required = true)
    private String shardName;
    @Argument(index = 1, name = "data-store-type", required = true, description = "config / operational")
    private String dataStoreType;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpc(GetShardRole.class)
                .invoke(new GetShardRoleInputBuilder()
                        .setShardName(new ShardName(shardName))
                        .setDataStoreType(DataStoreType.forName(dataStoreType))
                        .build());
    }
}
