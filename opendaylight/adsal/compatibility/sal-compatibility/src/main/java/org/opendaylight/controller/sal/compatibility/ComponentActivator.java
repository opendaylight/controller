/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Preconditions;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.compatibility.adsal.DataPacketServiceAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyAdapter;
import org.opendaylight.controller.sal.compatibility.topology.TopologyProvider;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.discovery.IDiscoveryService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IPluginOutReadService;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

public class ComponentActivator extends ComponentActivatorAbstractBase {
    private final INodeConnectorFactory nodeConnectorFactory = new MDSalNodeConnectorFactory();
    private final DataPacketServiceAdapter dataPacketService = new DataPacketServiceAdapter();
    private final InventoryAndReadAdapter inventory = new InventoryAndReadAdapter();
    private final FlowProgrammerAdapter flow = new FlowProgrammerAdapter();
    private final DataPacketAdapter dataPacket = new DataPacketAdapter();
    private final TopologyProvider tpProvider = new TopologyProvider();
    private final INodeFactory nodeFactory = new MDSalNodeFactory();
    private final TopologyAdapter topology = new TopologyAdapter();
    private BundleContext context;

    public INodeConnectorFactory getNodeConnectorFactory() {
        return nodeConnectorFactory;
    }

    public DataPacketServiceAdapter getDataPacketService() {
        return dataPacketService;
    }

    public InventoryAndReadAdapter getInventory() {
        return inventory;
    }

    public FlowProgrammerAdapter getFlow() {
        return flow;
    }

    public DataPacketAdapter getDataPacket() {
        return dataPacket;
    }

    public TopologyProvider getTpProvider() {
        return tpProvider;
    }

    public INodeFactory getNodeFactory() {
        return nodeFactory;
    }

    public TopologyAdapter getTopology() {
        return topology;
    }

    @Override
    protected void init() {
        // TODO: deprecated, should be removed soon
        NodeIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class);
        NodeConnectorIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class, NodeMapping.MD_SAL_TYPE);
    }

    @Override
    public void start(final BundleContext context) {
        this.context = Preconditions.checkNotNull(context);
        super.start(context);
    }

    public ProviderContext setBroker(final BindingAwareBroker broker) {
        return broker.registerProvider(new SalCompatibilityProvider(this), context);
    }

    @Override
    protected Object[] getGlobalImplementations() {
        return new Object[] {
                this, // Used for setBroker callback
                flow,
                inventory,
                dataPacket,
                nodeFactory,
                nodeConnectorFactory,
                topology,
                tpProvider
        };
    }

    @Override
    protected void configureGlobalInstance(final Component c, final Object imp) {
        if (imp instanceof DataPacketAdapter) {
            _configure((DataPacketAdapter)imp, c);
        } else if (imp instanceof FlowProgrammerAdapter) {
            _configure((FlowProgrammerAdapter)imp, c);
        } else if (imp instanceof InventoryAndReadAdapter) {
            _configure((InventoryAndReadAdapter)imp, c);
        } else if (imp instanceof ComponentActivator) {
            _configure((ComponentActivator)imp, c);
        } else if (imp instanceof MDSalNodeConnectorFactory) {
            _configure((MDSalNodeConnectorFactory)imp, c);
        } else if (imp instanceof MDSalNodeFactory) {
            _configure((MDSalNodeFactory)imp, c);
        } else if (imp instanceof TopologyAdapter) {
            _configure((TopologyAdapter)imp, c);
        } else if (imp instanceof TopologyProvider) {
            _configure((TopologyProvider)imp, c);
        } else {
            throw new IllegalArgumentException(String.format("Unhandled implementation class %s", imp.getClass()));
        }
    }

    @Override
    protected Object[] getImplementations() {
        return new Object[] {
                dataPacketService,
                inventory,
        };
    }

    @Override
    protected void configureInstance(final Component c, final Object imp, final String containerName) {
        if (imp instanceof ComponentActivator) {
            _instanceConfigure((ComponentActivator)imp, c, containerName);
        } else if (imp instanceof DataPacketServiceAdapter) {
            _instanceConfigure((DataPacketServiceAdapter)imp, c, containerName);
        } else if (imp instanceof InventoryAndReadAdapter) {
            _instanceConfigure((InventoryAndReadAdapter)imp, c, containerName);
        } else {
            throw new IllegalArgumentException(String.format("Unhandled implementation class %s", imp.getClass()));
        }
    }

    private void _configure(final MDSalNodeFactory imp, final Component it) {
        it.setInterface(INodeFactory.class.getName(), properties());
    }

    private void _configure(final MDSalNodeConnectorFactory imp, final Component it) {
        it.setInterface(INodeConnectorFactory.class.getName(), properties());
    }

    private void _configure(final ComponentActivator imp, final Component it) {
        it.add(createServiceDependency()
                .setService(BindingAwareBroker.class)
                .setCallbacks("setBroker", "setBroker")
                .setRequired(true));
    }

    private void _configure(final DataPacketAdapter imp, final Component it) {
        it.add(createServiceDependency()
                .setService(IPluginOutDataPacketService.class)
                .setCallbacks("setDataPacketPublisher", "setDataPacketPublisher")
                .setRequired(false));
    }

    private void _configure(final FlowProgrammerAdapter imp, final Component it) {
        it.setInterface(IPluginInFlowProgrammerService.class.getName(), properties());
        it.add(createServiceDependency()
                .setService(IPluginOutFlowProgrammerService.class)
                .setCallbacks("setFlowProgrammerPublisher", "setFlowProgrammerPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(IClusterGlobalServices.class)
                .setCallbacks("setClusterGlobalServices", "unsetClusterGlobalServices")
                .setRequired(false));
    }

    private void _instanceConfigure(final DataPacketServiceAdapter imp, final Component it, final String containerName) {
        it.setInterface(IPluginInDataPacketService.class.getName(), properties());
    }

    private void _instanceConfigure(final ComponentActivator imp, final Component it, final String containerName) {
        // No-op
    }

    private void _configure(final InventoryAndReadAdapter imp, final Component it) {
        it.setInterface(new String[] {
                IPluginInInventoryService.class.getName(),
                IPluginInReadService.class.getName(),
        }, properties());

        it.add(createServiceDependency()
                .setService(IPluginOutReadService.class)
                .setCallbacks("setReadPublisher", "unsetReadPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(IPluginOutInventoryService.class)
                .setCallbacks("setInventoryPublisher", "unsetInventoryPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(IDiscoveryService.class)
                .setCallbacks("setDiscoveryPublisher", "setDiscoveryPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(BindingAwareBroker.class)
                .setRequired(true));
    }

    private void _instanceConfigure(final InventoryAndReadAdapter imp, final Component it, String containerName) {
        it.setInterface(new String[] {
                IPluginInInventoryService.class.getName(),
                IPluginInReadService.class.getName(),
        }, properties());

        it.add(createServiceDependency()
                .setService(IPluginOutReadService.class)
                .setCallbacks("setReadPublisher", "unsetReadPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(IPluginOutInventoryService.class)
                .setCallbacks("setInventoryPublisher", "unsetInventoryPublisher")
                .setRequired(false));
        it.add(createServiceDependency()
                .setService(BindingAwareBroker.class)
                .setRequired(true));
    }

    private void _configure(final TopologyAdapter imp, final Component it) {
        it.setInterface(IPluginInTopologyService.class.getName(), properties());

        it.add(createServiceDependency()
                .setService(IPluginOutTopologyService.class)
                .setCallbacks("setTopologyPublisher", "setTopologyPublisher")
                .setRequired(false));
    }

    private void _configure(final TopologyProvider imp, final Component it) {
        it.add(createServiceDependency()
                .setService(IPluginOutTopologyService.class)
                .setCallbacks("setTopologyPublisher", "setTopologyPublisher")
                .setRequired(false));
    }

    private Dictionary<String,Object> properties() {
        final Hashtable<String,Object> props = new Hashtable<String, Object>();
        props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), NodeIDType.OPENFLOW);
        props.put("protocolName", NodeIDType.OPENFLOW);
        return props;
    }
}
