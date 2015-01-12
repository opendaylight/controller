/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.compatibility.adsal.DataPacketServiceAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;

import com.google.common.base.Preconditions;

class SalCompatibilityProvider implements BindingAwareProvider {
    private final ComponentActivator activator;

    public SalCompatibilityProvider(final ComponentActivator cmpAct) {
        this.activator = Preconditions.checkNotNull(cmpAct);
    }

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        final NotificationService subscribe = session.getSALService(NotificationService.class);

        final FlowProgrammerAdapter flow = activator.getFlow();
        flow.setDelegate(session.getRpcService(SalFlowService.class));
        flow.setDataBrokerService(session.getSALService(DataBrokerService.class));
        // FIXME: remember registration for clean shutdown
        subscribe.registerNotificationListener(flow);

        final InventoryAndReadAdapter inv = activator.getInventory();
        inv.setDataService(session.getSALService(DataBrokerService.class));
        inv.setFlowStatisticsService(session.getRpcService(OpendaylightFlowStatisticsService.class));
        inv.setFlowTableStatisticsService(session.getRpcService(OpendaylightFlowTableStatisticsService.class));
        inv.setNodeConnectorStatisticsService(session.getRpcService(OpendaylightPortStatisticsService.class));
        inv.setTopologyDiscovery(session.getRpcService(FlowTopologyDiscoveryService.class));
        inv.setDataProviderService(session.getSALService(DataProviderService.class));

        final NodeDataChangeListener ndcl = new NodeDataChangeListener(inv,session.getSALService(DataBroker.class));
        final NCDataChangeListener ncdcl = new NCDataChangeListener(inv,session.getSALService(DataBroker.class));

        // FIXME: remember registration for clean shutdown
        subscribe.registerNotificationListener(inv);

        final DataPacketServiceAdapter dps = activator.getDataPacketService();
        dps.setDelegate(session.getRpcService(PacketProcessingService.class));

        final TopologyAdapter topo = activator.getTopology();
        topo.setDataService(session.getSALService(DataProviderService.class));

        final TopologyProvider tpp = activator.getTpProvider();
        tpp.setDataService(session.getSALService(DataProviderService.class));

        inv.startAdapter();
        tpp.startAdapter();

        subscribe.registerNotificationListener(activator.getDataPacket());
    }
}
