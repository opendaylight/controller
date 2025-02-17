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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ChangeMemberVotingStatesForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ChangeMemberVotingStatesForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.member.voting.states.input.MemberVotingStateBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;


@Service
@Command(scope = "cluster-admin", name = "change-member-voting-states-for-all-shards",
        description = "Run a change-member-voting-states-for-all-shards test")
public class ChangeMemberVotingStatesForAllShardsCommand extends AbstractRpcAction {
    @Reference
    private RpcService rpcService;
    @Argument(index = 0, name = "member-name", required = true)
    private String memberName;
    @Argument(index = 1, name = "voting", required = true)
    private boolean voting;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpc(ChangeMemberVotingStatesForAllShards.class)
                .invoke(new ChangeMemberVotingStatesForAllShardsInputBuilder()
                        .setMemberVotingState(BindingMap.of(new MemberVotingStateBuilder()
                            .setMemberName(new MemberName(memberName))
                            .setVoting(voting)
                            .build()))
                        .build());
    }
}
