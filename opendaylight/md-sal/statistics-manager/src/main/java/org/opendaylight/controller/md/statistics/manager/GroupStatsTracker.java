/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class GroupStatsTracker extends AbstractListeningStatsTracker<GroupStats, GroupStats> {
    private static final Logger logger = LoggerFactory.getLogger(GroupStatsTracker.class);
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

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifierBuilder().augmentation(FlowCapableNode.class).child(Group.class).build();
    }

    @Override
    protected String statName() {
        return "Group";
    }

    public void request() {
        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        input.setNode(getNodeRef());

        requestHelper(groupStatsService.getAllGroupStatistics(input.build()));
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Group.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Group> group = (InstanceIdentifier<Group>)key;
                InstanceIdentifier<?> del = InstanceIdentifier.builder(group).augmentation(NodeGroupStatistics.class).toInstance();
                logger.debug("Key {} triggered remove of augmentation {}", key, del);

                trans.removeOperationalData(del);
            }
        }
        trans.commit();
    }

    @Override
    public void start(final DataBrokerService dbs) {
        if (groupStatsService == null) {
            logger.debug("No Group Statistics service, not subscribing to groups on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
