/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
    private static final Logger LOG = LoggerFactory.getLogger(GroupStatsTracker.class);
    private final OpendaylightGroupStatisticsService groupStatsService;

    GroupStatsTracker(OpendaylightGroupStatisticsService groupStatsService, FlowCapableContext context) {
        super(context);
        this.groupStatsService = Preconditions.checkNotNull(groupStatsService);
    }

    @Override
    protected void cleanupSingleStat(ReadWriteTransaction trans, GroupStats item) {
        InstanceIdentifier<NodeGroupStatistics> groupRef = getNodeIdentifier().augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(item.getGroupId())).augmentation(NodeGroupStatistics.class);
        trans.delete(LogicalDatastoreType.OPERATIONAL, groupRef);
    }

    @Override
    protected GroupStats updateSingleStat(ReadWriteTransaction trans, GroupStats item) {
        GroupBuilder groupBuilder = new GroupBuilder();
        GroupKey groupKey = new GroupKey(item.getGroupId());
        groupBuilder.setKey(groupKey);

        InstanceIdentifier<Group> groupRef = getNodeIdentifier()
                .augmentation(FlowCapableNode.class).child(Group.class,groupKey);

        NodeGroupStatisticsBuilder groupStatisticsBuilder= new NodeGroupStatisticsBuilder();
        groupStatisticsBuilder.setGroupStatistics(new GroupStatisticsBuilder(item).build());

        //Update augmented data
        groupBuilder.addAugmentation(NodeGroupStatistics.class, groupStatisticsBuilder.build());
        trans.merge(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build(), true);
        return item;
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifier().augmentation(FlowCapableNode.class).child(Group.class);
    }

    @Override
    protected String statName() {
        return "Group";
    }

    @Override
    public void request() {
        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        input.setNode(getNodeRef());

        requestHelper(groupStatsService.getAllGroupStatistics(input.build()));
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final ReadWriteTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedPaths()) {
            if (Group.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Group> group = (InstanceIdentifier<Group>)key;
                InstanceIdentifier<?> del = group.augmentation(NodeGroupStatistics.class);
                LOG.debug("Key {} triggered remove of augmentation {}", key, del);

                trans.delete(LogicalDatastoreType.OPERATIONAL, del);
            }
        }
        trans.submit();
    }

    @Override
    public void start(final DataBroker dbs) {
        if (groupStatsService == null) {
            LOG.debug("No Group Statistics service, not subscribing to groups on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
