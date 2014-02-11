/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility

import java.util.Arrays
import java.util.Dictionary
import java.util.Hashtable
import org.apache.felix.dm.Component
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer
import org.opendaylight.controller.sal.binding.api.NotificationService
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.compatibility.topology.TopologyAdapter
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.discovery.IDiscoveryService
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService
import org.opendaylight.controller.sal.reader.IPluginInReadService
import org.opendaylight.controller.sal.reader.IPluginOutReadService
import org.opendaylight.controller.sal.topology.IPluginInTopologyService
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.controller.sal.utils.GlobalConstants
import org.opendaylight.controller.sal.utils.INodeConnectorFactory
import org.opendaylight.controller.sal.utils.INodeFactory
import org.opendaylight.controller.clustering.services.IClusterGlobalServices
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService
import org.osgi.framework.BundleContext

import static org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.controller.sal.compatibility.topology.TopologyProvider
import org.opendaylight.controller.sal.compatibility.adsal.DataPacketServiceAdapter
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext

class ComponentActivator extends ComponentActivatorAbstractBase {

    private BundleContext context;

    @Property
    FlowProgrammerAdapter flow = new FlowProgrammerAdapter;

    @Property
    InventoryAndReadAdapter inventory = new InventoryAndReadAdapter;

    @Property
    DataPacketAdapter dataPacket = new DataPacketAdapter;

    @Property
    INodeFactory nodeFactory = new MDSalNodeFactory

    @Property
    INodeConnectorFactory nodeConnectorFactory = new MDSalNodeConnectorFactory
    
    @Property
    TopologyAdapter topology = new TopologyAdapter
    
    @Property
    TopologyProvider tpProvider = new TopologyProvider()

    @Property
    DataPacketServiceAdapter dataPacketService = new DataPacketServiceAdapter()



    override protected init() {
        Node.NodeIDType.registerIDType(MD_SAL_TYPE, String);
        NodeConnector.NodeConnectorIDType.registerIDType(MD_SAL_TYPE, String, MD_SAL_TYPE);
    }

    override start(BundleContext context) {
        super.start(context)
        this.context = context;
    }

    def setBroker(BindingAwareBroker broker) {
        broker.registerProvider(new SalCompatibilityProvider(this), context)
    }


    override protected getGlobalImplementations() {
        return Arrays.asList(this, flow, inventory, dataPacket, nodeFactory, nodeConnectorFactory,topology,tpProvider)
    }

    override protected configureGlobalInstance(Component c, Object imp) {
        configure(imp, c);
    }

    override protected getImplementations() {
        return Arrays.asList(dataPacketService)
    }

    override protected configureInstance(Component c, Object imp, String containerName) {
        instanceConfigure(imp, c, containerName);
    }

    private def dispatch configure(MDSalNodeFactory imp, Component it) {
        setInterface(INodeFactory.name, properties);
    }

    private def dispatch configure(MDSalNodeConnectorFactory imp, Component it) {
        setInterface(INodeConnectorFactory.name, properties);
    }

    private def dispatch configure(ComponentActivator imp, Component it) {
        add(
            createServiceDependency().setService(BindingAwareBroker) //
            .setCallbacks("setBroker", "setBroker") //
            .setRequired(true))


    }

    private def dispatch configure(DataPacketAdapter imp, Component it) {
        add(
            createServiceDependency() //
            .setService(IPluginOutDataPacketService) //
            .setCallbacks("setDataPacketPublisher", "setDataPacketPublisher") //
            .setRequired(false))
    }

    private def dispatch configure(FlowProgrammerAdapter imp, Component it) {
        setInterface(IPluginInFlowProgrammerService.name, properties)
        add(
            createServiceDependency() //
            .setService(IPluginOutFlowProgrammerService) //
            .setCallbacks("setFlowProgrammerPublisher", "setFlowProgrammerPublisher") //
            .setRequired(false))

        add(
            createServiceDependency() //
            .setService(IClusterGlobalServices) //
            .setCallbacks("setClusterGlobalServices", "unsetClusterGlobalServices") //
            .setRequired(false))

    }

    private def dispatch instanceConfigure(DataPacketServiceAdapter imp, Component it, String containerName) {
        setInterface(IPluginInDataPacketService.name, properties)
    }

    private def dispatch instanceConfigure(ComponentActivator imp, Component it, String containerName) {
    }


    private def dispatch configure(InventoryAndReadAdapter imp, Component it) {
        setInterface(Arrays.asList(IPluginInInventoryService.name, IPluginInReadService.name), properties)
        add(
            createServiceDependency() //
            .setService(IPluginOutReadService) //
            .setCallbacks("setReadPublisher", "unsetReadPublisher") //
            .setRequired(false))
        add(
            createServiceDependency() //
            .setService(IPluginOutInventoryService) //
            .setCallbacks("setInventoryPublisher", "unsetInventoryPublisher") //
            .setRequired(false))
        add(
            createServiceDependency() //
            .setService(IDiscoveryService) //
            .setCallbacks("setDiscoveryPublisher", "setDiscoveryPublisher") //
            .setRequired(false))

        
    }
    
    private def dispatch configure (TopologyAdapter imp, Component it) {
        setInterface(Arrays.asList(IPluginInTopologyService.name), properties)
        add(
            createServiceDependency() //
            .setService(IPluginOutTopologyService) //
            .setCallbacks("setTopologyPublisher", "setTopologyPublisher") //
            .setRequired(false))
    }
    
    private def dispatch configure (TopologyProvider imp, Component it) {
        add(
            createServiceDependency() //
            .setService(IPluginOutTopologyService) //
            .setCallbacks("setTopologyPublisher", "setTopologyPublisher") //
            .setRequired(false))
    }

    private def Dictionary<String, Object> properties() {
        val props = new Hashtable<String, Object>();
        props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString, MD_SAL_TYPE)
        props.put("protocolName", MD_SAL_TYPE);
        return props;
    }
}
package class SalCompatibilityProvider implements BindingAwareProvider {
    
    private val ComponentActivator activator;
    
    new(ComponentActivator cmpAct) {
        activator = cmpAct;
    }
    
    override getFunctionality() {
        // Noop
    }
    
    override getImplementations() {
        // Noop
    }
    
    
    override onSessionInitialized(ConsumerContext session) {
        // Noop
    }
    
    
    override onSessionInitiated(ProviderContext session) {
        val it = activator
                val subscribe = session.getSALService(NotificationService)

        // Registration of Flow Service
        flow.delegate = session.getRpcService(SalFlowService)
        flow.dataBrokerService = session.getSALService(DataBrokerService);
        subscribe.registerNotificationListener(flow);

        // Data Packet Service
        subscribe.registerNotificationListener(inventory);
        dataPacketService.delegate = session.getRpcService(PacketProcessingService)

        // Inventory Service
        inventory.dataService = session.getSALService(DataBrokerService);
        inventory.flowStatisticsService = session.getRpcService(OpendaylightFlowStatisticsService);
        inventory.flowTableStatisticsService = session.getRpcService(OpendaylightFlowTableStatisticsService);
        inventory.nodeConnectorStatisticsService = session.getRpcService(OpendaylightPortStatisticsService);
        inventory.topologyDiscovery = session.getRpcService(FlowTopologyDiscoveryService);
        inventory.dataProviderService = session.getSALService(DataProviderService)
        topology.dataService = session.getSALService(DataProviderService)
        tpProvider.dataService = session.getSALService(DataProviderService)

        inventory.start();

        tpProvider.start();

        subscribe.registerNotificationListener(dataPacket)
    }
}
