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
    private static final Logger logger = LoggerFactory.getLogger(GroupDescStatsTracker.class);
    private final OpendaylightGroupStatisticsService groupStatsService;

    public GroupDescStatsTracker(OpendaylightGroupStatisticsService groupStatsService, final FlowCapableContext context, final long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.groupStatsService = groupStatsService;
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

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifierBuilder().augmentation(FlowCapableNode.class).child(Group.class).build();
    }

    @Override
    protected String statName() {
        return "Group Descriptor";
    }

    public void request() {
        if (groupStatsService != null) {
            final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(groupStatsService.getGroupDescription(input.build()));
        }
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (InstanceIdentifier<?> key : change.getCreatedConfigurationData().keySet()) {
            if (Group.class.equals(key.getTargetType())) {
                logger.debug("Key {} triggered request", key);
                request();
            } else {
                logger.debug("Ignoring key {}", key);
            }
        }

        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Group.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Group> group = (InstanceIdentifier<Group>)key;
                InstanceIdentifier<?> del = InstanceIdentifier.builder(group).augmentation(NodeGroupDescStats.class).toInstance();
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
