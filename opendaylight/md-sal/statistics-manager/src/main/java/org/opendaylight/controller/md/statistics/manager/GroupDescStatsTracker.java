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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GroupDescStatsTracker extends AbstractListeningStatsTracker<GroupDescStats, GroupDescStats> {
    private static final Logger LOG = LoggerFactory.getLogger(GroupDescStatsTracker.class);
    private final OpendaylightGroupStatisticsService groupStatsService;

    public GroupDescStatsTracker(OpendaylightGroupStatisticsService groupStatsService, final FlowCapableContext context) {
        super(context);
        this.groupStatsService = groupStatsService;
    }

    @Override
    protected GroupDescStats updateSingleStat(ReadWriteTransaction trans, GroupDescStats item) {
        GroupBuilder groupBuilder = new GroupBuilder();
        GroupKey groupKey = new GroupKey(item.getGroupId());
        groupBuilder.setKey(groupKey);

        InstanceIdentifier<Group> groupRef = getNodeIdentifier()
                .augmentation(FlowCapableNode.class).child(Group.class,groupKey);

        NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
        groupDesc.setGroupDesc(new GroupDescBuilder(item).build());

        //Update augmented data
        groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());

        trans.merge(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build(), true);
        return item;
    }

    @Override
    protected void cleanupSingleStat(ReadWriteTransaction trans, GroupDescStats item) {
        InstanceIdentifier<NodeGroupDescStats> groupRef = getNodeIdentifier().augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(item.getGroupId())).augmentation(NodeGroupDescStats.class);
        trans.delete(LogicalDatastoreType.OPERATIONAL, groupRef);
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifier().augmentation(FlowCapableNode.class).child(Group.class);
    }

    @Override
    protected String statName() {
        return "Group Descriptor";
    }

    @Override
    public void request() {
        if (groupStatsService != null) {
            final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(groupStatsService.getGroupDescription(input.build()));
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (InstanceIdentifier<?> key : change.getCreatedData().keySet()) {
            if (Group.class.equals(key.getTargetType())) {
                LOG.debug("Key {} triggered request", key);
                request();
            } else {
                LOG.debug("Ignoring key {}", key);
            }
        }

        final ReadWriteTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedPaths()) {
            if (Group.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Group> group = (InstanceIdentifier<Group>)key;
                InstanceIdentifier<?> del = group.augmentation(NodeGroupDescStats.class);
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
