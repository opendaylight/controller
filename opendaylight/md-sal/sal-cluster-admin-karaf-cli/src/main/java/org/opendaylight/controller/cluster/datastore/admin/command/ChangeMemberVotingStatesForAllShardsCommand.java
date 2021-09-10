/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.member.voting.states.input.MemberVotingState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.member.voting.states.input.MemberVotingStateBuilder;


@Service
@Command(scope = "cluster-admin", name = "change-member-voting-states-for-all-shards",
        description = "Run a change-member-voting-states-for-all-shards test")
public class ChangeMemberVotingStatesForAllShardsCommand implements Action {

    @Reference
    private ClusterAdminService clusterAdminService;

    @Argument(index = 0, name = "member-name", required = true)
    String memberName;

    @Argument(index = 1, name = "voting", required = true)
    boolean voting;

    @Override
    public Object execute() throws Exception {
        final MemberVotingState memberVotingState = new MemberVotingStateBuilder()
                .setMemberName(memberName)
                .setVoting(voting)
                .build();

        return clusterAdminService
                .changeMemberVotingStatesForAllShards(new ChangeMemberVotingStatesForAllShardsInputBuilder()
                    .setMemberVotingState(List.of(memberVotingState))
                    .build())
                .get()
                .getResult();
    }
}
