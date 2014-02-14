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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

final class GroupDescStatsTracker extends AbstractStatsTracker<GroupDescStats, GroupDescStats> {
    private final OpendaylightGroupStatisticsService groupStatsService;

    public GroupDescStatsTracker(OpendaylightGroupStatisticsService groupStatsService, final FlowCapableContext context, final long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.groupStatsService = Preconditions.checkNotNull(groupStatsService);
    }

    @Override
    protected GroupDescStats updateSingleStat(DataModificationTransaction trans, GroupDescStats item) {
        GroupBuilder groupBuilder = new GroupBuilder();
        GroupKey groupKey = new GroupKey(item.getGroupId());
        groupBuilder.setKey(groupKey);

        InstanceIdentifier<Group> groupRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class).child(Group.class,groupKey).build();

        NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
        groupDesc.setGroupDesc(new GroupDescBuilder(item).build());

        //Update augmented data
        groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());

        trans.putOperationalData(groupRef, groupBuilder.build());
        return item;
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, GroupDescStats item) {
        InstanceIdentifier<NodeGroupDescStats> groupRef = getNodeIdentifierBuilder().augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(item.getGroupId())).augmentation(NodeGroupDescStats.class).build();
        trans.removeOperationalData(groupRef);
    }

    public ListenableFuture<TransactionId> request() {
        final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();
        input.setNode(getNodeRef());

        return requestHelper(groupStatsService.getGroupDescription(input.build()));
    }
}
