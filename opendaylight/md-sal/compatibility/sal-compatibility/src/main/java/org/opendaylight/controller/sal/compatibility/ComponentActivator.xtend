package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import static org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.apache.felix.dm.Component
import java.util.Arrays
import org.opendaylight.yangtools.yang.binding.NotificationListener
import java.util.Dictionary
import java.util.Hashtable
import org.opendaylight.controller.sal.utils.GlobalConstants
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService
import org.opendaylight.controller.sal.reader.IPluginInReadService
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.controller.sal.binding.api.NotificationService
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService
import org.osgi.framework.BundleContext
import org.opendaylight.controller.sal.reader.IPluginOutReadService
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService
import org.opendaylight.controller.sal.discovery.IDiscoveryService
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService

class ComponentActivator extends ComponentActivatorAbstractBase implements BindingAwareConsumer {

    private BundleContext context;

    @Property
    FlowProgrammerAdapter flow = new FlowProgrammerAdapter;

    @Property
    InventoryAndReadAdapter inventory = new InventoryAndReadAdapter;

    @Property
    DataPacketAdapter dataPacket = new DataPacketAdapter;

    override protected init() {
        Node.NodeIDType.registerIDType(MD_SAL_TYPE, NodeKey);
        NodeConnector.NodeConnectorIDType.registerIDType(MD_SAL_TYPE, NodeConnectorKey, MD_SAL_TYPE);
    }

    override start(BundleContext context) {
        super.start(context)
        this.context = context;
    }

    def setBroker(BindingAwareBroker broker) {
        broker.registerConsumer(this, context)
    }

    override onSessionInitialized(ConsumerContext session) {
        val subscribe = session.getSALService(NotificationService)

        // Registration of Flow Service
        flow.delegate = session.getRpcService(SalFlowService)
        subscribe.registerNotificationListener(flow);

        // Data Packet Service
        subscribe.registerNotificationListener(inventory);

        // Inventory Service
        inventory.dataService = session.getSALService(DataBrokerService);
        inventory.flowStatisticsService = session.getRpcService(OpendaylightFlowStatisticsService);
        inventory.topologyDiscovery = session.getRpcService(FlowTopologyDiscoveryService);

        subscribe.registerNotificationListener(dataPacket)

    }

    override protected getGlobalImplementations() {
        return Arrays.asList(this, flow, inventory, dataPacket)
    }

    override protected configureGlobalInstance(Component c, Object imp) {
        configure(imp, c);
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
    }

    private def dispatch configure(InventoryAndReadAdapter imp, Component it) {
        setInterface(Arrays.asList(IPluginInInventoryService.name, IPluginInReadService.name), properties)
        add(
            createServiceDependency() //
            .setService(IPluginOutReadService) //
            .setCallbacks("setReadPublisher", "setReadPublisher") //
            .setRequired(false))
        add(
            createServiceDependency() //
            .setService(IPluginOutInventoryService) //
            .setCallbacks("setInventoryPublisher", "setInventoryPublisher") //
            .setRequired(false))
        add(
            createServiceDependency() //
            .setService(IPluginOutTopologyService) //
            .setCallbacks("setTopologyPublisher", "setTopologyPublisher") //
            .setRequired(false))
        add(
            createServiceDependency() //
            .setService(IDiscoveryService) //
            .setCallbacks("setDiscoveryPublisher", "setDiscoveryPublisher") //
            .setRequired(false))
        
    }

    private def Dictionary<String, Object> properties() {
        val props = new Hashtable<String, Object>();
        props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString, MD_SAL_TYPE)
        return props;
    }
}
