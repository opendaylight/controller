/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

final class GroupStatsTracker extends AbstractStatsTracker<GroupStats, GroupStats> {
    private final OpendaylightGroupStatisticsService groupStatsService;

    GroupStatsTracker(OpendaylightGroupStatisticsService groupStatsService, FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.groupStatsService = Preconditions.checkNotNull(groupStatsService);
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, GroupStats item) {
        InstanceIdentifier<NodeGroupStatistics> groupRef = getNodeIdentifierBuilder().augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(item.getGroupId())).augmentation(NodeGroupStatistics.class).build();
        trans.removeOperationalData(groupRef);
    }

    @Override
    protected GroupStats updateSingleStat(DataModificationTransaction trans,
            GroupStats item) {
        GroupBuilder groupBuilder = new GroupBuilder();
        GroupKey groupKey = new GroupKey(item.getGroupId());
        groupBuilder.setKey(groupKey);

        InstanceIdentifier<Group> groupRef = getNodeIdentifierBuilder().augmentation(FlowCapableNode.class)
                .child(Group.class,groupKey).build();

        NodeGroupStatisticsBuilder groupStatisticsBuilder= new NodeGroupStatisticsBuilder();
        groupStatisticsBuilder.setGroupStatistics(new GroupStatisticsBuilder(item).build());

        //Update augmented data
        groupBuilder.addAugmentation(NodeGroupStatistics.class, groupStatisticsBuilder.build());
        trans.putOperationalData(groupRef, groupBuilder.build());
        return item;
    }

    public ListenableFuture<TransactionId> request() {
        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        input.setNode(getNodeRef());

        return requestHelper(groupStatsService.getAllGroupStatistics(input.build()));
    }
}
