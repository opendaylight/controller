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
import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer.StatCapabTypes;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatFlowNodeRegitrationImpl
 * FlowCapableNode Registration Implementation contains two method for registration/unregistration
 * FlowCapableNode for every connect/disconnect FlowCapableNode. Process of connection/disconnection
 * is substituted by listening Operation/DS for add/delete FlowCapableNode.
 * All statistic capabilities are reading from new Node directly without contacting device or DS.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public class StatFlowNodeRegistrationImpl extends StatAbstractListeningCommiter<FlowCapableNode> {

    private static final Logger LOG = LoggerFactory.getLogger(StatFlowNodeRegistrationImpl.class);

    public StatFlowNodeRegistrationImpl(final StatisticsManager manager, final DataBroker db) {
        super(manager, db, FlowCapableNode.class);
    }

    @Override
    protected InstanceIdentifier<FlowCapableNode> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<FlowCapableNode> keyIdent,
            final FlowCapableNode data) {
        Preconditions.checkNotNull(keyIdent, "InstanceIdentifier can not be null!");
        Preconditions.checkNotNull(data, "Node data for {} can not be null!", keyIdent);
        Preconditions.checkArgument(( ! keyIdent.isWildcarded()), "InstanceIdentifier is WildCarded!");
        final List<StatCapabTypes> statCapabTypes = new ArrayList<>();
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
                // TODO Meter statistics
            }
        }
        if (statCapabTypes.isEmpty()) {
            LOG.warn("FlowCapableNode {} is missing Statistic Capabilities!", keyIdent);
            statCapabTypes.add(StatCapabTypes.TABLE_STATS);
            statCapabTypes.add(StatCapabTypes.FLOW_STATS);
            statCapabTypes.add(StatCapabTypes.GROUP_STATS);
            statCapabTypes.add(StatCapabTypes.PORT_STATS);
            statCapabTypes.add(StatCapabTypes.QUEUE_STATS);
            // TODO Meter statistics
        }
        manager.registrateNewNode(keyIdent, Collections.unmodifiableList(statCapabTypes));
    }

    @Override
    public void removeStat(final InstanceIdentifier<FlowCapableNode> keyIdent) {
        Preconditions.checkArgument(keyIdent != null, "InstanceIdentifier can not be NULL!");
        Preconditions.checkArgument(( ! keyIdent.isWildcarded()),
                "InstanceIdentifier {} is WildCarded!", keyIdent);
        manager.unregistrateNode(keyIdent);
    }
}
