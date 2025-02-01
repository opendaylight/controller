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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev250131.MemberName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ChangeMemberVotingStatesForShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ChangeMemberVotingStatesForShardInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ShardName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.member.voting.states.input.MemberVotingStateBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "cluster-admin", name = "change-member-voting-states-for-shard",
        description = "Run a change-member-voting-states-for-shard test")
public class ChangeMemberVotingStatesForShardCommand extends AbstractRpcAction {
    @Reference
    private RpcService rpcService;
    @Argument(index = 0, name = "shard-name", required = true)
    private String shardName;
    @Argument(index = 1, name = "data-store-type", required = true, description = "config / operational")
    private String dataStoreType;
    @Argument(index = 2, name = "member-name", required = true)
    private String memberName;
    @Argument(index = 3, name = "voting", required = true)
    private boolean voting;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpc(ChangeMemberVotingStatesForShard.class)
                .invoke(new ChangeMemberVotingStatesForShardInputBuilder()
                        .setShardName(new ShardName(shardName))
                        .setDataStoreType(DataStoreType.forName(dataStoreType))
                        .setMemberVotingState(BindingMap.of(new MemberVotingStateBuilder()
                            .setMemberName(new MemberName(memberName))
                            .setVoting(voting)
                            .build()))
                        .build());
    }
}
