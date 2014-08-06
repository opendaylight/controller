/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatNodeRegistration;
import org.opendaylight.controller.md.statistics.manager.StatPermCollector.StatCapabTypes;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatNodeRegistrationImpl
 * {@link FlowCapableNode} Registration Implementation contains two method for registration/unregistration
 * {@link FlowCapableNode} for every connect/disconnect {@link FlowCapableNode}. Process of connection/disconnection
 * is substituted by listening Operation/DS for add/delete FlowCapableNode.
 * All statistic capabilities are reading from new Node directly without contacting device or DS.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public class StatNodeRegistrationImpl implements StatNodeRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(StatNodeRegistrationImpl.class);

    private final StatisticsManager manager;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private ListenerRegistration<?> notifListenerRegistration;

    public StatNodeRegistrationImpl(final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService notificationService) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticManager can not be null!");
        Preconditions.checkArgument(db != null, "DataBroker can not be null!");

        Preconditions.checkArgument(notificationService != null, "NotificationProviderService can not be null!");
        notifListenerRegistration = notificationService.registerNotificationListener(this);
     // TODO needs to change to DataChangeListener (uncomment)
//        final InstanceIdentifier<FlowCapableNode> wildCardedRegPath =
//                InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class);
//
//        listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
//                wildCardedRegPath, this, DataChangeScope.ONE);
    }

    @Override
    public void close() throws Exception {

        if (notifListenerRegistration != null) {
            try {
                notifListenerRegistration.close();
            }
            catch (final Exception e) {
                LOG.warn("Error by stop FlowCapableNode Notification StatNodeRegistration.");
            }
            notifListenerRegistration = null;
        }

        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FlowCapableNode DataChange StatListeningCommiter.", e);
            }
            listenerRegistration = null;
        }
    }

    // TODO needs to change to DataChangeListener (uncomment)
//    @SuppressWarnings("unchecked")
//    @Override
//    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
//        Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");
//        /* All DataObjects for create */
//        final Map<InstanceIdentifier<?>, DataObject> createdData = changeEvent.getCreatedData() != null
//                ? changeEvent.getCreatedData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
//        /* All DataObjects for remove */
//        final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
//                ? changeEvent.getRemovedPaths() : Collections.<InstanceIdentifier<?>> emptySet();
//
//        final Set<InstanceIdentifier<?>> keys = createdData.keySet() != null
//                ? createdData.keySet() : Collections.<InstanceIdentifier<?>> emptySet();
//        for (final InstanceIdentifier<?> key : keys) {
//            if (FlowCapableNode.class.equals(key.getTargetType())) {
//                final Optional<DataObject> value = Optional.of(createdData.get(key));
//                final InstanceIdentifier<Node> nodeKeyIdent = key.firstIdentifierOf(Node.class);
//                if (value.isPresent()) {
//                    connectFlowCapableNode((InstanceIdentifier<FlowCapableNode>)key, (FlowCapableNode)value.get(),
//                            nodeKeyIdent);
//                }
//            }
//        }
//        for (final InstanceIdentifier<?> key : removeData) {
//            if (FlowCapableNode.class.equals(key.getTargetType())) {
//                disconnectFlowCapableNode((InstanceIdentifier<FlowCapableNode>) key);
//            }
//        }
//    }

    @Override
    public void connectFlowCapableNode(final InstanceIdentifier<FlowCapableNode> keyIdent,
            final FlowCapableNode data, final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkNotNull(keyIdent, "InstanceIdentifier can not be null!");
        Preconditions.checkNotNull(data, "Node data for {} can not be null!", keyIdent);
        Preconditions.checkArgument(( ! keyIdent.isWildcarded()), "InstanceIdentifier is WildCarded!");

        final List<StatCapabTypes> statCapabTypes = new ArrayList<>();
        Short maxCapTables = Short.valueOf("1");

        if (data.getSwitchFeatures() != null) {
            final List<Class<? extends FeatureCapability>> capabilities = data.getSwitchFeatures()
                    .getCapabilities() != null ? data.getSwitchFeatures().getCapabilities()
                            : Collections.<Class<? extends FeatureCapability>> emptyList();
            for (final Class<? extends FeatureCapability> capability : capabilities) {
                if (capability == FlowFeatureCapabilityTableStats.class) {
                    statCapabTypes.add(StatCapabTypes.TABLE_STATS);
                } else if (capability == FlowFeatureCapabilityFlowStats.class) {
                    statCapabTypes.add(StatCapabTypes.FLOW_STATS);
                } else if (capability == FlowFeatureCapabilityGroupStats.class) {
                    statCapabTypes.add(StatCapabTypes.GROUP_STATS);
                } else if (capability == FlowFeatureCapabilityPortStats.class) {
                    statCapabTypes.add(StatCapabTypes.PORT_STATS);
                } else if (capability == FlowFeatureCapabilityQueueStats.class) {
                    statCapabTypes.add(StatCapabTypes.QUEUE_STATS);
                }
            }
            maxCapTables = data.getSwitchFeatures().getMaxTables();
        }
        final Optional<Short> maxTables = Optional.<Short> of(maxCapTables);

        /* Meters management */
        final InstanceIdentifier<NodeMeterFeatures> meterFeaturesIdent = nodeIdent.augmentation(NodeMeterFeatures.class);

        manager.enqueue(new StatDataStoreOperation() {

            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<NodeMeterFeatures> meterFeatures = Optional.absent();
                try {
                    meterFeatures = tx.read(LogicalDatastoreType.OPERATIONAL, meterFeaturesIdent).get();
                }
                catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Read NodeMeterFeatures {} fail!", meterFeaturesIdent, e);
                }
                if (meterFeatures.isPresent()) {
                    statCapabTypes.add(StatCapabTypes.METER_STATS);
                }
                manager.getStatCollector().connectedNodeRegistration(nodeIdent,
                        Collections.unmodifiableList(statCapabTypes), maxTables.get());
            }
        });
    }

    @Override
    public void disconnectFlowCapableNode(final InstanceIdentifier<FlowCapableNode> keyIdent) {
        Preconditions.checkArgument(keyIdent != null, "InstanceIdentifier can not be NULL!");
        Preconditions.checkArgument(( ! keyIdent.isWildcarded()),
                "InstanceIdentifier {} is WildCarded!", keyIdent);
        final InstanceIdentifier<Node> nodeIdent = keyIdent.firstIdentifierOf(Node.class);
        manager.getStatCollector().disconnectedNodeUnregistration(nodeIdent);
    }

    @Override
    public void onNodeConnectorRemoved(final NodeConnectorRemoved notification) {
        // NOOP
    }

    @Override
    public void onNodeConnectorUpdated(final NodeConnectorUpdated notification) {
        // NOOP
    }

    @Override
    public void onNodeRemoved(final NodeRemoved notification) {
        final NodeRef nodeRef = notification.getNodeRef();
        final InstanceIdentifier<?> nodeRefIdent = nodeRef.getValue();
        final Optional<InstanceIdentifier<Node>> optNodeIdent =
                Optional.fromNullable(nodeRefIdent.firstIdentifierOf(Node.class));
        if (optNodeIdent.isPresent()) {
            final InstanceIdentifier<FlowCapableNode> fNodeIdent =
                    optNodeIdent.get().augmentation(FlowCapableNode.class);
            disconnectFlowCapableNode(fNodeIdent);
        }
    }

    @Override
    public void onNodeUpdated(final NodeUpdated notification) {
        final Optional<FlowCapableNodeUpdated> optUpdateFlowCapNode =
                Optional.fromNullable(notification.getAugmentation(FlowCapableNodeUpdated.class));
        if (optUpdateFlowCapNode.isPresent()) {
            final NodeRef nodeRef = notification.getNodeRef();
            final InstanceIdentifier<?> nodeRefIdent = nodeRef.getValue();
            final InstanceIdentifier<Node> nodeIdent =
                    nodeRefIdent.firstIdentifierOf(Node.class);
            final InstanceIdentifier<FlowCapableNode> fNodeIdent =
                    nodeIdent.augmentation(FlowCapableNode.class);
            final FlowCapableNodeBuilder flowCapNodeBuilder =
                    new FlowCapableNodeBuilder(optUpdateFlowCapNode.get());
            connectFlowCapableNode(fNodeIdent, flowCapNodeBuilder.build(), nodeIdent);
        }
    }
}

