/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ServiceDependency;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.compatibility.DataPacketAdapter;
import org.opendaylight.controller.sal.compatibility.FlowProgrammerAdapter;
import org.opendaylight.controller.sal.compatibility.InventoryAndReadAdapter;
import org.opendaylight.controller.sal.compatibility.MDSalNodeConnectorFactory;
import org.opendaylight.controller.sal.compatibility.MDSalNodeFactory;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.compatibility.SalCompatibilityProvider;
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

@SuppressWarnings("all")
public class ComponentActivator extends ComponentActivatorAbstractBase {
  private BundleContext context;
  
  private FlowProgrammerAdapter _flow = new Function0<FlowProgrammerAdapter>() {
    public FlowProgrammerAdapter apply() {
      FlowProgrammerAdapter _flowProgrammerAdapter = new FlowProgrammerAdapter();
      return _flowProgrammerAdapter;
    }
  }.apply();
  
  public FlowProgrammerAdapter getFlow() {
    return this._flow;
  }
  
  public void setFlow(final FlowProgrammerAdapter flow) {
    this._flow = flow;
  }
  
  private InventoryAndReadAdapter _inventory = new Function0<InventoryAndReadAdapter>() {
    public InventoryAndReadAdapter apply() {
      InventoryAndReadAdapter _inventoryAndReadAdapter = new InventoryAndReadAdapter();
      return _inventoryAndReadAdapter;
    }
  }.apply();
  
  public InventoryAndReadAdapter getInventory() {
    return this._inventory;
  }
  
  public void setInventory(final InventoryAndReadAdapter inventory) {
    this._inventory = inventory;
  }
  
  private DataPacketAdapter _dataPacket = new Function0<DataPacketAdapter>() {
    public DataPacketAdapter apply() {
      DataPacketAdapter _dataPacketAdapter = new DataPacketAdapter();
      return _dataPacketAdapter;
    }
  }.apply();
  
  public DataPacketAdapter getDataPacket() {
    return this._dataPacket;
  }
  
  public void setDataPacket(final DataPacketAdapter dataPacket) {
    this._dataPacket = dataPacket;
  }
  
  private INodeFactory _nodeFactory = new Function0<INodeFactory>() {
    public INodeFactory apply() {
      MDSalNodeFactory _mDSalNodeFactory = new MDSalNodeFactory();
      return _mDSalNodeFactory;
    }
  }.apply();
  
  public INodeFactory getNodeFactory() {
    return this._nodeFactory;
  }
  
  public void setNodeFactory(final INodeFactory nodeFactory) {
    this._nodeFactory = nodeFactory;
  }
  
  private INodeConnectorFactory _nodeConnectorFactory = new Function0<INodeConnectorFactory>() {
    public INodeConnectorFactory apply() {
      MDSalNodeConnectorFactory _mDSalNodeConnectorFactory = new MDSalNodeConnectorFactory();
      return _mDSalNodeConnectorFactory;
    }
  }.apply();
  
  public INodeConnectorFactory getNodeConnectorFactory() {
    return this._nodeConnectorFactory;
  }
  
  public void setNodeConnectorFactory(final INodeConnectorFactory nodeConnectorFactory) {
    this._nodeConnectorFactory = nodeConnectorFactory;
  }
  
  private TopologyAdapter _topology = new Function0<TopologyAdapter>() {
    public TopologyAdapter apply() {
      TopologyAdapter _topologyAdapter = new TopologyAdapter();
      return _topologyAdapter;
    }
  }.apply();
  
  public TopologyAdapter getTopology() {
    return this._topology;
  }
  
  public void setTopology(final TopologyAdapter topology) {
    this._topology = topology;
  }
  
  private TopologyProvider _tpProvider = new Function0<TopologyProvider>() {
    public TopologyProvider apply() {
      TopologyProvider _topologyProvider = new TopologyProvider();
      return _topologyProvider;
    }
  }.apply();
  
  public TopologyProvider getTpProvider() {
    return this._tpProvider;
  }
  
  public void setTpProvider(final TopologyProvider tpProvider) {
    this._tpProvider = tpProvider;
  }
  
  private DataPacketServiceAdapter _dataPacketService = new Function0<DataPacketServiceAdapter>() {
    public DataPacketServiceAdapter apply() {
      DataPacketServiceAdapter _dataPacketServiceAdapter = new DataPacketServiceAdapter();
      return _dataPacketServiceAdapter;
    }
  }.apply();
  
  public DataPacketServiceAdapter getDataPacketService() {
    return this._dataPacketService;
  }
  
  public void setDataPacketService(final DataPacketServiceAdapter dataPacketService) {
    this._dataPacketService = dataPacketService;
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
    SalCompatibilityProvider _salCompatibilityProvider = new SalCompatibilityProvider(this);
    ProviderContext _registerProvider = broker.registerProvider(_salCompatibilityProvider, this.context);
    return _registerProvider;
  }
  
  protected Object[] getGlobalImplementations() {
    FlowProgrammerAdapter _flow = this.getFlow();
    InventoryAndReadAdapter _inventory = this.getInventory();
    DataPacketAdapter _dataPacket = this.getDataPacket();
    INodeFactory _nodeFactory = this.getNodeFactory();
    INodeConnectorFactory _nodeConnectorFactory = this.getNodeConnectorFactory();
    TopologyAdapter _topology = this.getTopology();
    TopologyProvider _tpProvider = this.getTpProvider();
    return ((Object[])Conversions.unwrapArray(Arrays.<Object>asList(this, _flow, _inventory, _dataPacket, _nodeFactory, _nodeConnectorFactory, _topology, _tpProvider), Object.class));
  }
  
  protected void configureGlobalInstance(final Component c, final Object imp) {
    this.configure(imp, c);
  }
  
  protected Object[] getImplementations() {
    DataPacketServiceAdapter _dataPacketService = this.getDataPacketService();
    return ((Object[])Conversions.unwrapArray(Arrays.<Object>asList(_dataPacketService), Object.class));
  }
  
  protected void configureInstance(final Component c, final Object imp, final String containerName) {
    this.instanceConfigure(imp, c, containerName);
  }
  
  private Component _configure(final MDSalNodeFactory imp, final Component it) {
    String _name = INodeFactory.class.getName();
    Dictionary<String,Object> _properties = this.properties();
    Component _setInterface = it.setInterface(_name, _properties);
    return _setInterface;
  }
  
  private Component _configure(final MDSalNodeConnectorFactory imp, final Component it) {
    String _name = INodeConnectorFactory.class.getName();
    Dictionary<String,Object> _properties = this.properties();
    Component _setInterface = it.setInterface(_name, _properties);
    return _setInterface;
  }
  
  private Component _configure(final ComponentActivator imp, final Component it) {
    ServiceDependency _createServiceDependency = this.createServiceDependency();
    ServiceDependency _setService = _createServiceDependency.setService(BindingAwareBroker.class);
    ServiceDependency _setCallbacks = _setService.setCallbacks("setBroker", "setBroker");
    ServiceDependency _setRequired = _setCallbacks.setRequired(true);
    Component _add = it.add(_setRequired);
    return _add;
  }
  
  private Component _configure(final DataPacketAdapter imp, final Component it) {
    ServiceDependency _createServiceDependency = this.createServiceDependency();
    ServiceDependency _setService = _createServiceDependency.setService(IPluginOutDataPacketService.class);
    ServiceDependency _setCallbacks = _setService.setCallbacks("setDataPacketPublisher", "setDataPacketPublisher");
    ServiceDependency _setRequired = _setCallbacks.setRequired(false);
    Component _add = it.add(_setRequired);
    return _add;
  }
  
  private Component _configure(final FlowProgrammerAdapter imp, final Component it) {
    Component _xblockexpression = null;
    {
      String _name = IPluginInFlowProgrammerService.class.getName();
      Dictionary<String,Object> _properties = this.properties();
      it.setInterface(_name, _properties);
      ServiceDependency _createServiceDependency = this.createServiceDependency();
      ServiceDependency _setService = _createServiceDependency.setService(IPluginOutFlowProgrammerService.class);
      ServiceDependency _setCallbacks = _setService.setCallbacks("setFlowProgrammerPublisher", "setFlowProgrammerPublisher");
      ServiceDependency _setRequired = _setCallbacks.setRequired(false);
      it.add(_setRequired);
      ServiceDependency _createServiceDependency_1 = this.createServiceDependency();
      ServiceDependency _setService_1 = _createServiceDependency_1.setService(IClusterGlobalServices.class);
      ServiceDependency _setCallbacks_1 = _setService_1.setCallbacks("setClusterGlobalServices", "unsetClusterGlobalServices");
      ServiceDependency _setRequired_1 = _setCallbacks_1.setRequired(false);
      Component _add = it.add(_setRequired_1);
      _xblockexpression = (_add);
    }
    return _xblockexpression;
  }
  
  private Component _instanceConfigure(final DataPacketServiceAdapter imp, final Component it, final String containerName) {
    String _name = IPluginInDataPacketService.class.getName();
    Dictionary<String,Object> _properties = this.properties();
    Component _setInterface = it.setInterface(_name, _properties);
    return _setInterface;
  }
  
  private Component _instanceConfigure(final ComponentActivator imp, final Component it, final String containerName) {
    return null;
  }
  
  private Component _configure(final InventoryAndReadAdapter imp, final Component it) {
    Component _xblockexpression = null;
    {
      String _name = IPluginInInventoryService.class.getName();
      String _name_1 = IPluginInReadService.class.getName();
      List<String> _asList = Arrays.<String>asList(_name, _name_1);
      Dictionary<String,Object> _properties = this.properties();
      it.setInterface(((String[])Conversions.unwrapArray(_asList, String.class)), _properties);
      ServiceDependency _createServiceDependency = this.createServiceDependency();
      ServiceDependency _setService = _createServiceDependency.setService(IPluginOutReadService.class);
      ServiceDependency _setCallbacks = _setService.setCallbacks("setReadPublisher", "unsetReadPublisher");
      ServiceDependency _setRequired = _setCallbacks.setRequired(false);
      it.add(_setRequired);
      ServiceDependency _createServiceDependency_1 = this.createServiceDependency();
      ServiceDependency _setService_1 = _createServiceDependency_1.setService(IPluginOutInventoryService.class);
      ServiceDependency _setCallbacks_1 = _setService_1.setCallbacks("setInventoryPublisher", "unsetInventoryPublisher");
      ServiceDependency _setRequired_1 = _setCallbacks_1.setRequired(false);
      it.add(_setRequired_1);
      ServiceDependency _createServiceDependency_2 = this.createServiceDependency();
      ServiceDependency _setService_2 = _createServiceDependency_2.setService(IDiscoveryService.class);
      ServiceDependency _setCallbacks_2 = _setService_2.setCallbacks("setDiscoveryPublisher", "setDiscoveryPublisher");
      ServiceDependency _setRequired_2 = _setCallbacks_2.setRequired(false);
      Component _add = it.add(_setRequired_2);
      _xblockexpression = (_add);
    }
    return _xblockexpression;
  }
  
  private Component _configure(final TopologyAdapter imp, final Component it) {
    Component _xblockexpression = null;
    {
      String _name = IPluginInTopologyService.class.getName();
      List<String> _asList = Arrays.<String>asList(_name);
      Dictionary<String,Object> _properties = this.properties();
      it.setInterface(((String[])Conversions.unwrapArray(_asList, String.class)), _properties);
      ServiceDependency _createServiceDependency = this.createServiceDependency();
      ServiceDependency _setService = _createServiceDependency.setService(IPluginOutTopologyService.class);
      ServiceDependency _setCallbacks = _setService.setCallbacks("setTopologyPublisher", "setTopologyPublisher");
      ServiceDependency _setRequired = _setCallbacks.setRequired(false);
      Component _add = it.add(_setRequired);
      _xblockexpression = (_add);
    }
    return _xblockexpression;
  }
  
  private Component _configure(final TopologyProvider imp, final Component it) {
    ServiceDependency _createServiceDependency = this.createServiceDependency();
    ServiceDependency _setService = _createServiceDependency.setService(IPluginOutTopologyService.class);
    ServiceDependency _setCallbacks = _setService.setCallbacks("setTopologyPublisher", "setTopologyPublisher");
    ServiceDependency _setRequired = _setCallbacks.setRequired(false);
    Component _add = it.add(_setRequired);
    return _add;
  }
  
  private Dictionary<String,Object> properties() {
    Hashtable<String,Object> _hashtable = new Hashtable<String, Object>();
    final Hashtable<String,Object> props = _hashtable;
    String _string = GlobalConstants.PROTOCOLPLUGINTYPE.toString();
    props.put(_string, NodeMapping.MD_SAL_TYPE);
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
