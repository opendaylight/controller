/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ServiceDependency;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("all")
public class ComponentActivator extends ComponentActivatorAbstractBase {
  private BundleContext context;
  
  private FlowProgrammerAdapter flowProgrammerAdapter = new Function0<FlowProgrammerAdapter>() {
    public FlowProgrammerAdapter apply() {
      return new FlowProgrammerAdapter();
    }
  }.apply();
  
  public FlowProgrammerAdapter getFlow() {
    return this.flowProgrammerAdapter;
  }
  
  public void setFlow(final FlowProgrammerAdapter flowProgrammerAdapter) {
    this.flowProgrammerAdapter = flowProgrammerAdapter;
  }
  
  private InventoryAndReadAdapter inventoryAndReadAdapter = new Function0<InventoryAndReadAdapter>() {
    public InventoryAndReadAdapter apply() {
      return new InventoryAndReadAdapter();
    }
  }.apply();
  
  public InventoryAndReadAdapter getInventory() {
    return this.inventoryAndReadAdapter;
  }
  
  public void setInventory(final InventoryAndReadAdapter inventory) {
    this.inventoryAndReadAdapter = inventory;
  }
  
  private DataPacketAdapter dataPacketAdapter = new Function0<DataPacketAdapter>() {
    public DataPacketAdapter apply() {
      return new DataPacketAdapter();
    }
  }.apply();
  
  public DataPacketAdapter getDataPacket() {
    return this.dataPacketAdapter;
  }
  
  public void setDataPacket(final DataPacketAdapter dataPacket) {
    this.dataPacketAdapter = dataPacket;
  }
  
  private INodeFactory nodeFactory = new Function0<INodeFactory>() {
    public INodeFactory apply() {
      return new MDSalNodeFactory();
    }
  }.apply();
  
  public INodeFactory getNodeFactory() {
    return this.nodeFactory;
  }
  
  public void setNodeFactory(final INodeFactory nodeFactory) {
    this.nodeFactory = nodeFactory;
  }
  
  private INodeConnectorFactory nodeConnectorFactory = new Function0<INodeConnectorFactory>() {
    public INodeConnectorFactory apply() {
      return new MDSalNodeConnectorFactory();
    }
  }.apply();
  
  public INodeConnectorFactory getNodeConnectorFactory() {
    return this.nodeConnectorFactory;
  }
  
  public void setNodeConnectorFactory(final INodeConnectorFactory nodeConnectorFactory) {
    this.nodeConnectorFactory = nodeConnectorFactory;
  }
  
  private TopologyAdapter topologyAdapter = new Function0<TopologyAdapter>() {
    public TopologyAdapter apply() {
      return new TopologyAdapter();
    }
  }.apply();
  
  public TopologyAdapter getTopology() {
    return this.topologyAdapter;
  }
  
  public void setTopology(final TopologyAdapter topology) {
    this.topologyAdapter = topology;
  }
  
  private TopologyProvider topologyProvider = new Function0<TopologyProvider>() {
    public TopologyProvider apply() {
      return new TopologyProvider();
    }
  }.apply();
  
  public TopologyProvider getTpProvider() {
    return this.topologyProvider;
  }
  
  public void setTpProvider(final TopologyProvider tpProvider) {
    this.topologyProvider = tpProvider;
  }
  
  private DataPacketServiceAdapter dataPacketServiceAdapter = new Function0<DataPacketServiceAdapter>() {
    public DataPacketServiceAdapter apply() {
      DataPacketServiceAdapter _dataPacketServiceAdapter = new DataPacketServiceAdapter();
      return _dataPacketServiceAdapter;
    }
  }.apply();
  
  public DataPacketServiceAdapter getDataPacketService() {
    return this.dataPacketServiceAdapter;
  }
  
  public void setDataPacketService(final DataPacketServiceAdapter dataPacketService) {
    this.dataPacketServiceAdapter = dataPacketService;
  }
  
  protected void init() {
    NodeIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class);
    NodeConnectorIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class, NodeMapping.MD_SAL_TYPE);
  }
  
  public void start(final BundleContext context) {
    super.start(context);
    this.context = context;
  }
  
  public ProviderContext setBroker(final BindingAwareBroker broker) {
    SalCompatibilityProvider salCompatibilityProvider = new SalCompatibilityProvider(this);
    return broker.registerProvider(salCompatibilityProvider, this.context);
  }
  
  protected Object[] getGlobalImplementations() {
    FlowProgrammerAdapter flow = this.getFlow();
    InventoryAndReadAdapter inventory = this.getInventory();
    DataPacketAdapter dataPacket = this.getDataPacket();
    INodeFactory nodeFactory = this.getNodeFactory();
    INodeConnectorFactory nodeConnectorFactory = this.getNodeConnectorFactory();
    TopologyAdapter topology = this.getTopology();
    TopologyProvider tpProvider = this.getTpProvider();
    return ((Object[])Conversions.unwrapArray(Arrays.<Object>asList(this, flow, inventory, dataPacket, nodeFactory, nodeConnectorFactory, topology, tpProvider), Object.class));
  }
  
  protected void configureGlobalInstance(final Component c, final Object imp) {
    this.configure(imp, c);
  }
  
  protected Object[] getImplementations() {
    DataPacketServiceAdapter dataPacketService = this.getDataPacketService();
    return ((Object[])Conversions.unwrapArray(Arrays.<Object>asList(dataPacketService), Object.class));
  }
  
  protected void configureInstance(final Component c, final Object imp, final String containerName) {
    this.instanceConfigure(imp, c, containerName);
  }
  
  private Component _configure(final MDSalNodeFactory imp, final Component it) {
    String name = INodeFactory.class.getName();
    Dictionary<String,Object> properties = this.properties();
    return it.setInterface(name, properties);
  }
  
  private Component _configure(final MDSalNodeConnectorFactory imp, final Component it) {
    String name = INodeConnectorFactory.class.getName();
    Dictionary<String,Object> properties = this.properties();
    return it.setInterface(name, properties);
  }
  
  private Component _configure(final ComponentActivator imp, final Component it) {
    ServiceDependency dependency = this.createServiceDependency()
            .setService(BindingAwareBroker.class)
            .setCallbacks("setBroker", "setBroker")
            .setRequired(true);
    return it.add(dependency);
  }
  
  private Component _configure(final DataPacketAdapter imp, final Component it) {
    ServiceDependency dependency = this.createServiceDependency()
        .setService(IPluginOutDataPacketService.class)
        .setCallbacks("setDataPacketPublisher", "setDataPacketPublisher")
        .setRequired(false);
    return it.add(dependency);
  }
  
  private Component _configure(final FlowProgrammerAdapter imp, final Component it) {
    String name = IPluginInFlowProgrammerService.class.getName();
    Dictionary<String,Object> properties = this.properties();
    it.setInterface(name, properties);
    ServiceDependency dependency = this.createServiceDependency()
        .setService(IPluginOutFlowProgrammerService.class)
        .setCallbacks("setFlowProgrammerPublisher", "setFlowProgrammerPublisher")
        .setRequired(false);
    it.add(dependency);
    dependency = this.createServiceDependency()
        .setService(IClusterGlobalServices.class)
        .setCallbacks("setClusterGlobalServices", "unsetClusterGlobalServices")
        .setRequired(false);
    return it.add(dependency);
  }
  
  private Component _instanceConfigure(final DataPacketServiceAdapter imp, final Component it, final String containerName) {
    String name = IPluginInDataPacketService.class.getName();
    Dictionary<String,Object> properties = this.properties();
    return it.setInterface(name, properties);
  }
  
  private Component _instanceConfigure(final ComponentActivator imp, final Component it, final String containerName) {
    return null;
  }
  
  private Component _configure(final InventoryAndReadAdapter imp, final Component it) {
    String name = IPluginInInventoryService.class.getName();
    String name_1 = IPluginInReadService.class.getName();
    List<String> _asList = Arrays.<String>asList(name, name_1);
    Dictionary<String,Object> _properties = this.properties();
    it.setInterface(((String[])Conversions.unwrapArray(_asList, String.class)), _properties);
    ServiceDependency dependency = this.createServiceDependency()
        .setService(IPluginOutReadService.class)
        .setCallbacks("setReadPublisher", "unsetReadPublisher")
        .setRequired(false);
    it.add(dependency);

    dependency = this.createServiceDependency()
        .setService(IPluginOutInventoryService.class)
        .setCallbacks("setInventoryPublisher", "unsetInventoryPublisher")
        .setRequired(false);
    it.add(dependency);

    dependency = this.createServiceDependency()
        .setService(IDiscoveryService.class)
        .setCallbacks("setDiscoveryPublisher", "setDiscoveryPublisher")
        .setRequired(false);
    return it.add(dependency);
  }
  
  private Component _configure(final TopologyAdapter imp, final Component it) {
    String _name = IPluginInTopologyService.class.getName();
    List<String> _asList = Arrays.<String>asList(_name);
    Dictionary<String,Object> _properties = this.properties();
    it.setInterface(((String[])Conversions.unwrapArray(_asList, String.class)), _properties);
    ServiceDependency dependency = this.createServiceDependency()
        .setService(IPluginOutTopologyService.class)
        .setCallbacks("setTopologyPublisher", "setTopologyPublisher")
        .setRequired(false);
    return it.add(dependency);
  }
  
  private Component _configure(final TopologyProvider imp, final Component it) {
    ServiceDependency dependency = this.createServiceDependency()
        .setService(IPluginOutTopologyService.class)
        .setCallbacks("setTopologyPublisher", "setTopologyPublisher")
        .setRequired(false);
    return it.add(dependency);
  }
  
  private Dictionary<String,Object> properties() {
    final Hashtable<String,Object> props = new Hashtable<String, Object>();
    props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), NodeMapping.MD_SAL_TYPE);
    props.put("protocolName", NodeMapping.MD_SAL_TYPE);
    return props;
  }
  
  private Component configure(final Object imp, final Component it) {
    if (imp instanceof DataPacketAdapter) {
      return _configure((DataPacketAdapter)imp, it);
    } else if (imp instanceof FlowProgrammerAdapter) {
      return _configure((FlowProgrammerAdapter)imp, it);
    } else if (imp instanceof InventoryAndReadAdapter) {
      return _configure((InventoryAndReadAdapter)imp, it);
    } else if (imp instanceof ComponentActivator) {
      return _configure((ComponentActivator)imp, it);
    } else if (imp instanceof MDSalNodeConnectorFactory) {
      return _configure((MDSalNodeConnectorFactory)imp, it);
    } else if (imp instanceof MDSalNodeFactory) {
      return _configure((MDSalNodeFactory)imp, it);
    } else if (imp instanceof TopologyAdapter) {
      return _configure((TopologyAdapter)imp, it);
    } else if (imp instanceof TopologyProvider) {
      return _configure((TopologyProvider)imp, it);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(imp, it).toString());
    }
  }
  
  private Object instanceConfigure(final Object imp, final Component it, final String containerName) {
    if (imp instanceof ComponentActivator) {
      return _instanceConfigure((ComponentActivator)imp, it, containerName);
    } else if (imp instanceof DataPacketServiceAdapter) {
      return _instanceConfigure((DataPacketServiceAdapter)imp, it, containerName);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(imp, it, containerName).toString());
    }
  }
}
