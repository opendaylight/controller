/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Following are two main responsibilities of the class
 * 1) Listen for the create changes in config data store for tree nodes (Flow,Group,Meter,Queue)
 * and send statistics request to the switch to fetch the statistics
 *
 * 2)Listen for the remove changes in config data store for tree nodes (Flow,Group,Meter,Queue)
 * and remove the relative statistics data from operational data store.
 *
 * @author avishnoi@in.ibm.com
 *
 */
public class StatisticsUpdateHandler implements DataChangeListener {

    private static final Logger suhLogger = LoggerFactory.getLogger(StatisticsUpdateHandler.class);
    private final StatisticsProvider statisticsManager;

    public StatisticsUpdateHandler(final StatisticsProvider manager){
        this.statisticsManager = manager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> additions = change.getCreatedConfigurationData();
        for (InstanceIdentifier<? extends DataObject> dataObjectInstance : additions.keySet()) {
            DataObject dataObject = additions.get(dataObjectInstance);
            InstanceIdentifier<Node> nodeII = dataObjectInstance.firstIdentifierOf(Node.class);
            NodeRef nodeRef = new NodeRef(nodeII);
            if(dataObject instanceof Flow){
                Flow flow = (Flow) dataObject;
                try {
                    this.statisticsManager.sendFlowStatsFromTableRequest(nodeRef, flow);
                } catch (InterruptedException | ExecutionException e) {
                    suhLogger.warn("Following exception occured while sending flow statistics request newly added flow: {}", e);
                }
            }
            if(dataObject instanceof Meter){
                try {
                    this.statisticsManager.sendMeterConfigStatisticsRequest(nodeRef);
                } catch (InterruptedException | ExecutionException e) {
                    suhLogger.warn("Following exception occured while sending meter statistics request for newly added meter: {}", e);
                }
            }
            if(dataObject instanceof Group){
                try {
                    this.statisticsManager.sendGroupDescriptionRequest(nodeRef);
                } catch (InterruptedException | ExecutionException e) {
                    suhLogger.warn("Following exception occured while sending group description request for newly added group: {}", e);
                }
            }
            if(dataObject instanceof Queue){
                Queue queue = (Queue) dataObject;
                InstanceIdentifier<NodeConnector> nodeConnectorII = dataObjectInstance.firstIdentifierOf(NodeConnector.class);
                NodeConnectorKey nodeConnectorKey = InstanceIdentifier.keyOf(nodeConnectorII);
                try {
                    this.statisticsManager.sendQueueStatsFromGivenNodeConnector(nodeRef, nodeConnectorKey.getId(), queue.getQueueId());
                } catch (InterruptedException | ExecutionException e) {
                    suhLogger.warn("Following exception occured while sending queue statistics request for newly added group: {}", e);
                }
            }
        }

        DataModificationTransaction it = this.statisticsManager.startChange();
        Set<InstanceIdentifier<? extends DataObject>> removals = change.getRemovedConfigurationData();
        for (InstanceIdentifier<? extends DataObject> dataObjectInstance : removals) {
            DataObject dataObject = change.getOriginalConfigurationData().get(dataObjectInstance);

            if(dataObject instanceof Flow){
                InstanceIdentifier<Flow> flowII = (InstanceIdentifier<Flow>)dataObjectInstance;
                InstanceIdentifier<?> flowAugmentation =
                        InstanceIdentifier.builder(flowII).augmentation(FlowStatisticsData.class).toInstance();
                it.removeOperationalData(flowAugmentation);
            }
            if(dataObject instanceof Meter){
                InstanceIdentifier<Meter> meterII = (InstanceIdentifier<Meter>)dataObjectInstance;

                InstanceIdentifier<?> nodeMeterConfigStatsAugmentation =
                        InstanceIdentifier.builder(meterII).augmentation(NodeMeterConfigStats.class).toInstance();
                it.removeOperationalData(nodeMeterConfigStatsAugmentation);

                InstanceIdentifier<?> nodeMeterStatisticsAugmentation =
                        InstanceIdentifier.builder(meterII).augmentation(NodeMeterStatistics.class).toInstance();
                it.removeOperationalData(nodeMeterStatisticsAugmentation);
            }

            if(dataObject instanceof Group){
                InstanceIdentifier<Group> groupII = (InstanceIdentifier<Group>)dataObjectInstance;

                InstanceIdentifier<?> nodeGroupDescStatsAugmentation =
                        InstanceIdentifier.builder(groupII).augmentation(NodeGroupDescStats.class).toInstance();
                it.removeOperationalData(nodeGroupDescStatsAugmentation);

                InstanceIdentifier<?> nodeGroupStatisticsAugmentation =
                        InstanceIdentifier.builder(groupII).augmentation(NodeGroupStatistics.class).toInstance();
                it.removeOperationalData(nodeGroupStatisticsAugmentation);
            }

            if(dataObject instanceof Queue){
                InstanceIdentifier<Queue> queueII = (InstanceIdentifier<Queue>)dataObjectInstance;

                InstanceIdentifier<?> nodeConnectorQueueStatisticsDataAugmentation =
                        InstanceIdentifier.builder(queueII).augmentation(FlowCapableNodeConnectorQueueStatisticsData.class).toInstance();
                it.removeOperationalData(nodeConnectorQueueStatisticsDataAugmentation);
            }
        }
        it.commit();
    }
}
