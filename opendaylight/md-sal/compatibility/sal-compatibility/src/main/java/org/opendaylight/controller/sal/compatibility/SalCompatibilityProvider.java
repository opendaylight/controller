/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import java.util.Collection;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.compatibility.ComponentActivator;
import org.opendaylight.controller.sal.compatibility.DataPacketAdapter;
import org.opendaylight.controller.sal.compatibility.FlowProgrammerAdapter;
import org.opendaylight.controller.sal.compatibility.InventoryAndReadAdapter;
import org.opendaylight.controller.sal.compatibility.adsal.DataPacketServiceAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yangtools.yang.binding.RpcService;

@SuppressWarnings("all")
class SalCompatibilityProvider implements BindingAwareProvider {
  private final ComponentActivator activator;
  
  public SalCompatibilityProvider(final ComponentActivator cmpAct) {
    this.activator = cmpAct;
  }
  
  public Collection<? extends ProviderFunctionality> getFunctionality() {
    return null;
  }
  
  public Collection<? extends RpcService> getImplementations() {
    return null;
  }
  
  public void onSessionInitialized(final ConsumerContext session) {
  }
  
  public void onSessionInitiated(final ProviderContext session) {
    final ComponentActivator it = this.activator;
    final NotificationService subscribe = session.<NotificationService>getSALService(NotificationService.class);
    FlowProgrammerAdapter _flow = it.getFlow();
    SalFlowService _rpcService = session.<SalFlowService>getRpcService(SalFlowService.class);
    _flow.setDelegate(_rpcService);
    FlowProgrammerAdapter _flow_1 = it.getFlow();
    DataBrokerService _sALService = session.<DataBrokerService>getSALService(DataBrokerService.class);
    _flow_1.setDataBrokerService(_sALService);
    FlowProgrammerAdapter _flow_2 = it.getFlow();
    subscribe.registerNotificationListener(_flow_2);
    InventoryAndReadAdapter _inventory = it.getInventory();
    subscribe.registerNotificationListener(_inventory);
    DataPacketServiceAdapter _dataPacketService = it.getDataPacketService();
    PacketProcessingService _rpcService_1 = session.<PacketProcessingService>getRpcService(PacketProcessingService.class);
    _dataPacketService.setDelegate(_rpcService_1);
    InventoryAndReadAdapter _inventory_1 = it.getInventory();
    DataBrokerService _sALService_1 = session.<DataBrokerService>getSALService(DataBrokerService.class);
    _inventory_1.setDataService(_sALService_1);
    InventoryAndReadAdapter _inventory_2 = it.getInventory();
    OpendaylightFlowStatisticsService _rpcService_2 = session.<OpendaylightFlowStatisticsService>getRpcService(OpendaylightFlowStatisticsService.class);
    _inventory_2.setFlowStatisticsService(_rpcService_2);
    InventoryAndReadAdapter _inventory_3 = it.getInventory();
    OpendaylightFlowTableStatisticsService _rpcService_3 = session.<OpendaylightFlowTableStatisticsService>getRpcService(OpendaylightFlowTableStatisticsService.class);
    _inventory_3.setFlowTableStatisticsService(_rpcService_3);
    InventoryAndReadAdapter _inventory_4 = it.getInventory();
    OpendaylightPortStatisticsService _rpcService_4 = session.<OpendaylightPortStatisticsService>getRpcService(OpendaylightPortStatisticsService.class);
    _inventory_4.setNodeConnectorStatisticsService(_rpcService_4);
    InventoryAndReadAdapter _inventory_5 = it.getInventory();
    FlowTopologyDiscoveryService _rpcService_5 = session.<FlowTopologyDiscoveryService>getRpcService(FlowTopologyDiscoveryService.class);
    _inventory_5.setTopologyDiscovery(_rpcService_5);
    InventoryAndReadAdapter _inventory_6 = it.getInventory();
    DataProviderService _sALService_2 = session.<DataProviderService>getSALService(DataProviderService.class);
    _inventory_6.setDataProviderService(_sALService_2);
    TopologyAdapter _topology = it.getTopology();
    DataProviderService _sALService_3 = session.<DataProviderService>getSALService(DataProviderService.class);
    _topology.setDataService(_sALService_3);
    TopologyProvider _tpProvider = it.getTpProvider();
    DataProviderService _sALService_4 = session.<DataProviderService>getSALService(DataProviderService.class);
    _tpProvider.setDataService(_sALService_4);
    InventoryAndReadAdapter _inventory_7 = it.getInventory();
    _inventory_7.startAdapter();
    TopologyProvider _tpProvider_1 = it.getTpProvider();
    _tpProvider_1.startAdapter();
    DataPacketAdapter _dataPacket = it.getDataPacket();
    subscribe.registerNotificationListener(_dataPacket);
  }
}
