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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatNodeRegistration;
import org.opendaylight.controller.md.statistics.manager.StatPermCollector.StatCapabTypes;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeatures;
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
 * {@link FeatureCapability} for every connect/disconnect {@link FlowCapableNode}. Process of connection/disconnection
 * is substituted by listening Operation/DS for add/delete {@link FeatureCapability}.
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

    @Override
    public void connectFlowCapableNode(final InstanceIdentifier<SwitchFeatures> keyIdent,
            final SwitchFeatures data, final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkNotNull(keyIdent, "InstanceIdentifier can not be null!");
        Preconditions.checkNotNull(data, "SwitchFeatures data for {} can not be null!", keyIdent);
        Preconditions.checkArgument(( ! keyIdent.isWildcarded()), "InstanceIdentifier is WildCarded!");

        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {

                final List<StatCapabTypes> statCapabTypes = new ArrayList<>();
                Short maxCapTables = Short.valueOf("1");

                final List<Class<? extends FeatureCapability>> capabilities = data.getCapabilities() != null
                        ? data.getCapabilities() : Collections.<Class<? extends FeatureCapability>> emptyList();
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
                maxCapTables = data.getMaxTables();

                final Optional<Short> maxTables = Optional.<Short> of(maxCapTables);

                /* Meters management */
                final InstanceIdentifier<NodeMeterFeatures> meterFeaturesIdent = nodeIdent.augmentation(NodeMeterFeatures.class);


                Optional<NodeMeterFeatures> meterFeatures = Optional.absent();
                try {
                    meterFeatures = tx.read(LogicalDatastoreType.OPERATIONAL, meterFeaturesIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.warn("Read NodeMeterFeatures {} fail!", meterFeaturesIdent, e);
                }
                if (meterFeatures.isPresent()) {
                    statCapabTypes.add(StatCapabTypes.METER_STATS);
                }
                manager.connectedNodeRegistration(nodeIdent,
                        Collections.unmodifiableList(statCapabTypes), maxTables.get());
            }
        });
    }

    @Override
    public void disconnectFlowCapableNode(final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkArgument(nodeIdent != null, "InstanceIdentifier can not be NULL!");
        Preconditions.checkArgument(( ! nodeIdent.isWildcarded()),
                "InstanceIdentifier {} is WildCarded!", nodeIdent);
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                manager.disconnectedNodeUnregistration(nodeIdent);
            }
        });
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
        final InstanceIdentifier<Node> nodeIdent =
                nodeRefIdent.firstIdentifierOf(Node.class);
        if (nodeIdent != null) {
            disconnectFlowCapableNode(nodeIdent);
        }
    }

    @Override
    public void onNodeUpdated(final NodeUpdated notification) {
        final FlowCapableNodeUpdated newFlowNode =
                notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (newFlowNode != null && newFlowNode.getSwitchFeatures() != null) {
            final NodeRef nodeRef = notification.getNodeRef();
            final InstanceIdentifier<?> nodeRefIdent = nodeRef.getValue();
            final InstanceIdentifier<Node> nodeIdent =
                    nodeRefIdent.firstIdentifierOf(Node.class);

            final InstanceIdentifier<SwitchFeatures> swichFeaturesIdent =
                    nodeIdent.augmentation(FlowCapableNode.class).child(SwitchFeatures.class);
            final SwitchFeatures switchFeatures = newFlowNode.getSwitchFeatures();
            connectFlowCapableNode(swichFeaturesIdent, switchFeatures, nodeIdent);
        }
    }
}

