/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Objects;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.common.util.Arguments;
import org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
import org.opendaylight.controller.sal.compatibility.InventoryNotificationProvider;
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IPluginOutReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.Duration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Packets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class InventoryAndReadAdapter implements IPluginInReadService, IPluginInInventoryService, OpendaylightInventoryListener, OpendaylightFlowStatisticsListener, OpendaylightFlowTableStatisticsListener, OpendaylightPortStatisticsListener {
  private final static Logger LOG = new Function0<Logger>() {
    public Logger apply() {
      Logger _logger = LoggerFactory.getLogger(InventoryAndReadAdapter.class);
      return _logger;
    }
  }.apply();
  
  private final static short OPENFLOWV10_TABLE_ID = new Function0<Short>() {
    public Short apply() {
      Integer _integer = new Integer(0);
      short _shortValue = _integer.shortValue();
      return _shortValue;
    }
  }.apply();
  
  private DataBrokerService _dataService;
  
  public DataBrokerService getDataService() {
    return this._dataService;
  }
  
  public void setDataService(final DataBrokerService dataService) {
    this._dataService = dataService;
  }
  
  private DataProviderService _dataProviderService;
  
  public DataProviderService getDataProviderService() {
    return this._dataProviderService;
  }
  
  public void setDataProviderService(final DataProviderService dataProviderService) {
    this._dataProviderService = dataProviderService;
  }
  
  private OpendaylightFlowStatisticsService _flowStatisticsService;
  
  public OpendaylightFlowStatisticsService getFlowStatisticsService() {
    return this._flowStatisticsService;
  }
  
  public void setFlowStatisticsService(final OpendaylightFlowStatisticsService flowStatisticsService) {
    this._flowStatisticsService = flowStatisticsService;
  }
  
  private OpendaylightPortStatisticsService _nodeConnectorStatisticsService;
  
  public OpendaylightPortStatisticsService getNodeConnectorStatisticsService() {
    return this._nodeConnectorStatisticsService;
  }
  
  public void setNodeConnectorStatisticsService(final OpendaylightPortStatisticsService nodeConnectorStatisticsService) {
    this._nodeConnectorStatisticsService = nodeConnectorStatisticsService;
  }
  
  private OpendaylightFlowTableStatisticsService _flowTableStatisticsService;
  
  public OpendaylightFlowTableStatisticsService getFlowTableStatisticsService() {
    return this._flowTableStatisticsService;
  }
  
  public void setFlowTableStatisticsService(final OpendaylightFlowTableStatisticsService flowTableStatisticsService) {
    this._flowTableStatisticsService = flowTableStatisticsService;
  }
  
  private FlowTopologyDiscoveryService _topologyDiscovery;
  
  public FlowTopologyDiscoveryService getTopologyDiscovery() {
    return this._topologyDiscovery;
  }
  
  public void setTopologyDiscovery(final FlowTopologyDiscoveryService topologyDiscovery) {
    this._topologyDiscovery = topologyDiscovery;
  }
  
  private List<IPluginOutReadService> _statisticsPublisher = new Function0<List<IPluginOutReadService>>() {
    public List<IPluginOutReadService> apply() {
      CopyOnWriteArrayList<IPluginOutReadService> _copyOnWriteArrayList = new CopyOnWriteArrayList<IPluginOutReadService>();
      return _copyOnWriteArrayList;
    }
  }.apply();
  
  public List<IPluginOutReadService> getStatisticsPublisher() {
    return this._statisticsPublisher;
  }
  
  public void setStatisticsPublisher(final List<IPluginOutReadService> statisticsPublisher) {
    this._statisticsPublisher = statisticsPublisher;
  }
  
  private List<IPluginOutInventoryService> _inventoryPublisher = new Function0<List<IPluginOutInventoryService>>() {
    public List<IPluginOutInventoryService> apply() {
      CopyOnWriteArrayList<IPluginOutInventoryService> _copyOnWriteArrayList = new CopyOnWriteArrayList<IPluginOutInventoryService>();
      return _copyOnWriteArrayList;
    }
  }.apply();
  
  public List<IPluginOutInventoryService> getInventoryPublisher() {
    return this._inventoryPublisher;
  }
  
  public void setInventoryPublisher(final List<IPluginOutInventoryService> inventoryPublisher) {
    this._inventoryPublisher = inventoryPublisher;
  }
  
  private final InventoryNotificationProvider inventoryNotificationProvider = new Function0<InventoryNotificationProvider>() {
    public InventoryNotificationProvider apply() {
      InventoryNotificationProvider _inventoryNotificationProvider = new InventoryNotificationProvider();
      return _inventoryNotificationProvider;
    }
  }.apply();
  
  private final Map<PathArgument,List<PathArgument>> nodeToNodeConnectorsMap = new Function0<Map<PathArgument,List<PathArgument>>>() {
    public Map<PathArgument,List<PathArgument>> apply() {
      ConcurrentHashMap<PathArgument,List<PathArgument>> _concurrentHashMap = new ConcurrentHashMap<PathArgument, List<PathArgument>>();
      return _concurrentHashMap;
    }
  }.apply();
  
  private final Lock nodeToNodeConnectorsLock = new Function0<Lock>() {
    public Lock apply() {
      ReentrantLock _reentrantLock = new ReentrantLock();
      return _reentrantLock;
    }
  }.apply();
  
  public void startAdapter() {
    DataProviderService _dataProviderService = this.getDataProviderService();
    this.inventoryNotificationProvider.setDataProviderService(_dataProviderService);
    List<IPluginOutInventoryService> _inventoryPublisher = this.getInventoryPublisher();
    this.inventoryNotificationProvider.setInventoryPublisher(_inventoryPublisher);
  }
  
  public Object start() {
    return null;
  }
  
  public boolean setInventoryPublisher(final IPluginOutInventoryService listener) {
    List<IPluginOutInventoryService> _inventoryPublisher = this.getInventoryPublisher();
    boolean _add = _inventoryPublisher.add(listener);
    return _add;
  }
  
  public boolean unsetInventoryPublisher(final IPluginOutInventoryService listener) {
    List<IPluginOutInventoryService> _inventoryPublisher = this.getInventoryPublisher();
    boolean _remove = _inventoryPublisher.remove(listener);
    return _remove;
  }
  
  public boolean setReadPublisher(final IPluginOutReadService listener) {
    List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
    boolean _add = _statisticsPublisher.add(listener);
    return _add;
  }
  
  public Boolean unsetReadPublisher(final IPluginOutReadService listener) {
    Boolean _xifexpression = null;
    boolean _notEquals = (!Objects.equal(listener, null));
    if (_notEquals) {
      List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
      boolean _remove = _statisticsPublisher.remove(listener);
      _xifexpression = Boolean.valueOf(_remove);
    }
    return _xifexpression;
  }
  
  protected DataModificationTransaction startChange() {
    DataProviderService _dataProviderService = this.getDataProviderService();
    return _dataProviderService.beginTransaction();
  }
  
  public long getTransmitRate(final NodeConnector connector) {
    NodeConnectorRef _nodeConnectorRef = NodeMapping.toNodeConnectorRef(connector);
    final FlowCapableNodeConnector nodeConnector = this.readFlowCapableNodeConnector(_nodeConnectorRef);
    return (nodeConnector.getCurrentSpeed()).longValue();
  }
  
  public List<FlowOnNode> readAllFlow(final Node node, final boolean cached) {
    ArrayList<FlowOnNode> _arrayList = new ArrayList<FlowOnNode>();
    final ArrayList<FlowOnNode> output = _arrayList;
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    NodeKey _nodeKey = InventoryMapping.toNodeKey(node);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    InstanceIdentifierBuilder<FlowCapableNode> _augmentation = _child.<FlowCapableNode>augmentation(FlowCapableNode.class);
    TableKey _tableKey = new TableKey(Short.valueOf(InventoryAndReadAdapter.OPENFLOWV10_TABLE_ID));
    InstanceIdentifierBuilder<Table> _child_1 = _augmentation.<Table, TableKey>child(Table.class, _tableKey);
    final InstanceIdentifier<Table> tableRef = _child_1.toInstance();
    final DataModificationTransaction it = this.startChange();
    DataObject _readConfigurationData = it.readConfigurationData(tableRef);
    final Table table = ((Table) _readConfigurationData);
    boolean _notEquals = (!Objects.equal(table, null));
    if (_notEquals) {
      List<Flow> _flow = table.getFlow();
      int _size = _flow.size();
      InventoryAndReadAdapter.LOG.trace("Number of flows installed in table 0 of node {} : {}", node, Integer.valueOf(_size));
      List<Flow> _flow_1 = table.getFlow();
      for (final Flow flow : _flow_1) {
        {
          final org.opendaylight.controller.sal.flowprogrammer.Flow adsalFlow = ToSalConversionsUtils.toFlow(flow, node);
          final FlowStatisticsData statsFromDataStore = flow.<FlowStatisticsData>getAugmentation(FlowStatisticsData.class);
          boolean _notEquals_1 = (!Objects.equal(statsFromDataStore, null));
          if (_notEquals_1) {
            FlowOnNode _flowOnNode = new FlowOnNode(adsalFlow);
            final FlowOnNode it_1 = _flowOnNode;
            FlowStatistics _flowStatistics = statsFromDataStore.getFlowStatistics();
            Counter64 _byteCount = _flowStatistics.getByteCount();
            BigInteger _value = _byteCount.getValue();
            long _longValue = _value.longValue();
            it_1.setByteCount(_longValue);
            FlowStatistics _flowStatistics_1 = statsFromDataStore.getFlowStatistics();
            Counter64 _packetCount = _flowStatistics_1.getPacketCount();
            BigInteger _value_1 = _packetCount.getValue();
            long _longValue_1 = _value_1.longValue();
            it_1.setPacketCount(_longValue_1);
            FlowStatistics _flowStatistics_2 = statsFromDataStore.getFlowStatistics();
            Duration _duration = _flowStatistics_2.getDuration();
            Counter32 _second = _duration.getSecond();
            Long _value_2 = _second.getValue();
            int _intValue = _value_2.intValue();
            it_1.setDurationSeconds(_intValue);
            FlowStatistics _flowStatistics_3 = statsFromDataStore.getFlowStatistics();
            Duration _duration_1 = _flowStatistics_3.getDuration();
            Counter32 _nanosecond = _duration_1.getNanosecond();
            Long _value_3 = _nanosecond.getValue();
            int _intValue_1 = _value_3.intValue();
            it_1.setDurationNanoseconds(_intValue_1);
            output.add(it_1);
          }
        }
      }
    }
    GetAllFlowsStatisticsFromAllFlowTablesInputBuilder _getAllFlowsStatisticsFromAllFlowTablesInputBuilder = new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
    final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input = _getAllFlowsStatisticsFromAllFlowTablesInputBuilder;
    NodeRef _nodeRef = NodeMapping.toNodeRef(node);
    input.setNode(_nodeRef);
    OpendaylightFlowStatisticsService _flowStatisticsService = this.getFlowStatisticsService();
    GetAllFlowsStatisticsFromAllFlowTablesInput _build = input.build();
    _flowStatisticsService.getAllFlowsStatisticsFromAllFlowTables(_build);
    return output;
  }
  
  public List<NodeConnectorStatistics> readAllNodeConnector(final Node node, final boolean cached) {
    ArrayList<NodeConnectorStatistics> _arrayList = new ArrayList<NodeConnectorStatistics>();
    final ArrayList<NodeConnectorStatistics> ret = _arrayList;
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    NodeKey _nodeKey = InventoryMapping.toNodeKey(node);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
    final DataModificationTransaction provider = this.startChange();
    DataObject _readConfigurationData = provider.readConfigurationData(nodeRef);
    final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dsNode = ((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) _readConfigurationData);
    boolean _notEquals = (!Objects.equal(dsNode, null));
    if (_notEquals) {
      List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> _nodeConnector = dsNode.getNodeConnector();
      for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector dsNodeConnector : _nodeConnector) {
        {
          InstanceIdentifierBuilder<Nodes> _builder_1 = InstanceIdentifier.<Nodes>builder(Nodes.class);
          NodeKey _nodeKey_1 = InventoryMapping.toNodeKey(node);
          InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child_1 = _builder_1.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey_1);
          NodeConnectorKey _key = dsNodeConnector.getKey();
          InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> _child_2 = _child_1.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, NodeConnectorKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, _key);
          final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> nodeConnectorRef = _child_2.toInstance();
          DataObject _readConfigurationData_1 = provider.readConfigurationData(nodeConnectorRef);
          final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nodeConnectorFromDS = ((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector) _readConfigurationData_1);
          boolean _notEquals_1 = (!Objects.equal(nodeConnectorFromDS, null));
          if (_notEquals_1) {
            FlowCapableNodeConnectorStatisticsData _augmentation = nodeConnectorFromDS.<FlowCapableNodeConnectorStatisticsData>getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
            final FlowCapableNodeConnectorStatistics nodeConnectorStatsFromDs = ((FlowCapableNodeConnectorStatistics) _augmentation);
            org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics _flowCapableNodeConnectorStatistics = nodeConnectorStatsFromDs.getFlowCapableNodeConnectorStatistics();
            NodeId _id = dsNode.getId();
            NodeConnectorId _id_1 = dsNodeConnector.getId();
            NodeConnectorStatistics _nodeConnectorStatistics = this.toNodeConnectorStatistics(_flowCapableNodeConnectorStatistics, _id, _id_1);
            ret.add(_nodeConnectorStatistics);
          }
        }
      }
    }
    GetAllNodeConnectorsStatisticsInputBuilder _getAllNodeConnectorsStatisticsInputBuilder = new GetAllNodeConnectorsStatisticsInputBuilder();
    final GetAllNodeConnectorsStatisticsInputBuilder input = _getAllNodeConnectorsStatisticsInputBuilder;
    NodeRef _nodeRef = NodeMapping.toNodeRef(node);
    input.setNode(_nodeRef);
    OpendaylightPortStatisticsService _nodeConnectorStatisticsService = this.getNodeConnectorStatisticsService();
    GetAllNodeConnectorsStatisticsInput _build = input.build();
    _nodeConnectorStatisticsService.getAllNodeConnectorsStatistics(_build);
    return ret;
  }
  
  public List<NodeTableStatistics> readAllNodeTable(final Node node, final boolean cached) {
    ArrayList<NodeTableStatistics> _arrayList = new ArrayList<NodeTableStatistics>();
    final ArrayList<NodeTableStatistics> ret = _arrayList;
    NodeRef _nodeRef = NodeMapping.toNodeRef(node);
    final FlowCapableNode dsFlowCapableNode = this.readFlowCapableNode(_nodeRef);
    boolean _notEquals = (!Objects.equal(dsFlowCapableNode, null));
    if (_notEquals) {
      List<Table> _table = dsFlowCapableNode.getTable();
      for (final Table table : _table) {
        {
          final FlowTableStatisticsData tableStats = table.<FlowTableStatisticsData>getAugmentation(FlowTableStatisticsData.class);
          boolean _notEquals_1 = (!Objects.equal(tableStats, null));
          if (_notEquals_1) {
            FlowTableStatistics _flowTableStatistics = tableStats.getFlowTableStatistics();
            Short _id = table.getId();
            NodeTableStatistics _nodeTableStatistics = this.toNodeTableStatistics(_flowTableStatistics, _id, node);
            ret.add(_nodeTableStatistics);
          }
        }
      }
    }
    GetFlowTablesStatisticsInputBuilder _getFlowTablesStatisticsInputBuilder = new GetFlowTablesStatisticsInputBuilder();
    final GetFlowTablesStatisticsInputBuilder input = _getFlowTablesStatisticsInputBuilder;
    NodeRef _nodeRef_1 = NodeMapping.toNodeRef(node);
    input.setNode(_nodeRef_1);
    OpendaylightFlowTableStatisticsService _flowTableStatisticsService = this.getFlowTableStatisticsService();
    GetFlowTablesStatisticsInput _build = input.build();
    _flowTableStatisticsService.getFlowTablesStatistics(_build);
    return ret;
  }
  
  public NodeDescription readDescription(final Node node, final boolean cached) {
    NodeRef _nodeRef = NodeMapping.toNodeRef(node);
    return this.toNodeDescription(_nodeRef);
  }
  
  public FlowOnNode readFlow(final Node node, final org.opendaylight.controller.sal.flowprogrammer.Flow targetFlow, final boolean cached) {
    FlowOnNode ret = null;
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    NodeKey _nodeKey = InventoryMapping.toNodeKey(node);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    InstanceIdentifierBuilder<FlowCapableNode> _augmentation = _child.<FlowCapableNode>augmentation(FlowCapableNode.class);
    TableKey _tableKey = new TableKey(Short.valueOf(InventoryAndReadAdapter.OPENFLOWV10_TABLE_ID));
    InstanceIdentifierBuilder<Table> _child_1 = _augmentation.<Table, TableKey>child(Table.class, _tableKey);
    final InstanceIdentifier<Table> tableRef = _child_1.toInstance();
    final DataModificationTransaction it = this.startChange();
    DataObject _readConfigurationData = it.readConfigurationData(tableRef);
    final Table table = ((Table) _readConfigurationData);
    boolean _notEquals = (!Objects.equal(table, null));
    if (_notEquals) {
      List<Flow> _flow = table.getFlow();
      int _size = _flow.size();
      InventoryAndReadAdapter.LOG.trace("Number of flows installed in table 0 of node {} : {}", node, Integer.valueOf(_size));
      List<Flow> _flow_1 = table.getFlow();
      for (final Flow mdsalFlow : _flow_1) {
        Flow _mDSalflow = MDFlowMapping.toMDSalflow(targetFlow);
        boolean _flowEquals = FromSalConversionsUtils.flowEquals(mdsalFlow, _mDSalflow);
        if (_flowEquals) {
          final FlowStatisticsData statsFromDataStore = mdsalFlow.<FlowStatisticsData>getAugmentation(FlowStatisticsData.class);
          boolean _notEquals_1 = (!Objects.equal(statsFromDataStore, null));
          if (_notEquals_1) {
            InventoryAndReadAdapter.LOG.debug("Found matching flow in the data store flow table ");
            FlowOnNode _flowOnNode = new FlowOnNode(targetFlow);
            final FlowOnNode it_1 = _flowOnNode;
            FlowStatistics _flowStatistics = statsFromDataStore.getFlowStatistics();
            Counter64 _byteCount = _flowStatistics.getByteCount();
            BigInteger _value = _byteCount.getValue();
            long _longValue = _value.longValue();
            it_1.setByteCount(_longValue);
            FlowStatistics _flowStatistics_1 = statsFromDataStore.getFlowStatistics();
            Counter64 _packetCount = _flowStatistics_1.getPacketCount();
            BigInteger _value_1 = _packetCount.getValue();
            long _longValue_1 = _value_1.longValue();
            it_1.setPacketCount(_longValue_1);
            FlowStatistics _flowStatistics_2 = statsFromDataStore.getFlowStatistics();
            Duration _duration = _flowStatistics_2.getDuration();
            Counter32 _second = _duration.getSecond();
            Long _value_2 = _second.getValue();
            int _intValue = _value_2.intValue();
            it_1.setDurationSeconds(_intValue);
            FlowStatistics _flowStatistics_3 = statsFromDataStore.getFlowStatistics();
            Duration _duration_1 = _flowStatistics_3.getDuration();
            Counter32 _nanosecond = _duration_1.getNanosecond();
            Long _value_3 = _nanosecond.getValue();
            int _intValue_1 = _value_3.intValue();
            it_1.setDurationNanoseconds(_intValue_1);
            ret = it_1;
          }
        }
      }
    }
    GetFlowStatisticsFromFlowTableInputBuilder _getFlowStatisticsFromFlowTableInputBuilder = new GetFlowStatisticsFromFlowTableInputBuilder();
    final GetFlowStatisticsFromFlowTableInputBuilder input = _getFlowStatisticsFromFlowTableInputBuilder;
    NodeRef _nodeRef = NodeMapping.toNodeRef(node);
    input.setNode(_nodeRef);
    Flow _mDSalflow_1 = MDFlowMapping.toMDSalflow(targetFlow);
    input.fieldsFrom(_mDSalflow_1);
    OpendaylightFlowStatisticsService _flowStatisticsService = this.getFlowStatisticsService();
    GetFlowStatisticsFromFlowTableInput _build = input.build();
    _flowStatisticsService.getFlowStatisticsFromFlowTable(_build);
    return ret;
  }
  
  public NodeConnectorStatistics readNodeConnector(final NodeConnector connector, final boolean cached) {
    NodeConnectorStatistics nodeConnectorStatistics = null;
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    Node _node = connector.getNode();
    NodeKey _nodeKey = InventoryMapping.toNodeKey(_node);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    NodeConnectorKey _nodeConnectorKey = InventoryMapping.toNodeConnectorKey(connector);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> _child_1 = _child.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, NodeConnectorKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, _nodeConnectorKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> nodeConnectorRef = _child_1.toInstance();
    final DataModificationTransaction provider = this.startChange();
    DataObject _readConfigurationData = provider.readConfigurationData(nodeConnectorRef);
    final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nodeConnectorFromDS = ((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector) _readConfigurationData);
    boolean _notEquals = (!Objects.equal(nodeConnectorFromDS, null));
    if (_notEquals) {
      FlowCapableNodeConnectorStatisticsData _augmentation = nodeConnectorFromDS.<FlowCapableNodeConnectorStatisticsData>getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
      final FlowCapableNodeConnectorStatistics nodeConnectorStatsFromDs = ((FlowCapableNodeConnectorStatistics) _augmentation);
      boolean _notEquals_1 = (!Objects.equal(nodeConnectorStatsFromDs, null));
      if (_notEquals_1) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics _flowCapableNodeConnectorStatistics = nodeConnectorStatsFromDs.getFlowCapableNodeConnectorStatistics();
        Node _node_1 = connector.getNode();
        NodeKey _nodeKey_1 = InventoryMapping.toNodeKey(_node_1);
        NodeId _id = _nodeKey_1.getId();
        NodeConnectorKey _nodeConnectorKey_1 = InventoryMapping.toNodeConnectorKey(connector);
        NodeConnectorId _id_1 = _nodeConnectorKey_1.getId();
        NodeConnectorStatistics _nodeConnectorStatistics = this.toNodeConnectorStatistics(_flowCapableNodeConnectorStatistics, _id, _id_1);
        nodeConnectorStatistics = _nodeConnectorStatistics;
      }
    }
    GetNodeConnectorStatisticsInputBuilder _getNodeConnectorStatisticsInputBuilder = new GetNodeConnectorStatisticsInputBuilder();
    final GetNodeConnectorStatisticsInputBuilder input = _getNodeConnectorStatisticsInputBuilder;
    Node _node_2 = connector.getNode();
    NodeRef _nodeRef = NodeMapping.toNodeRef(_node_2);
    input.setNode(_nodeRef);
    NodeConnectorKey _nodeConnectorKey_2 = InventoryMapping.toNodeConnectorKey(connector);
    NodeConnectorId _id_2 = _nodeConnectorKey_2.getId();
    input.setNodeConnectorId(_id_2);
    OpendaylightPortStatisticsService _nodeConnectorStatisticsService = this.getNodeConnectorStatisticsService();
    GetNodeConnectorStatisticsInput _build = input.build();
    _nodeConnectorStatisticsService.getNodeConnectorStatistics(_build);
    return nodeConnectorStatistics;
  }
  
  public NodeTableStatistics readNodeTable(final NodeTable nodeTable, final boolean cached) {
    NodeTableStatistics nodeStats = null;
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    Node _node = nodeTable.getNode();
    NodeKey _nodeKey = InventoryMapping.toNodeKey(_node);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    InstanceIdentifierBuilder<FlowCapableNode> _augmentation = _child.<FlowCapableNode>augmentation(FlowCapableNode.class);
    Object _iD = nodeTable.getID();
    TableKey _tableKey = new TableKey(((Short) _iD));
    InstanceIdentifierBuilder<Table> _child_1 = _augmentation.<Table, TableKey>child(Table.class, _tableKey);
    final InstanceIdentifier<Table> tableRef = _child_1.toInstance();
    final DataModificationTransaction it = this.startChange();
    DataObject _readConfigurationData = it.readConfigurationData(tableRef);
    final Table table = ((Table) _readConfigurationData);
    boolean _notEquals = (!Objects.equal(table, null));
    if (_notEquals) {
      final FlowTableStatisticsData tableStats = table.<FlowTableStatisticsData>getAugmentation(FlowTableStatisticsData.class);
      boolean _notEquals_1 = (!Objects.equal(tableStats, null));
      if (_notEquals_1) {
        FlowTableStatistics _flowTableStatistics = tableStats.getFlowTableStatistics();
        Short _id = table.getId();
        Node _node_1 = nodeTable.getNode();
        NodeTableStatistics _nodeTableStatistics = this.toNodeTableStatistics(_flowTableStatistics, _id, _node_1);
        nodeStats = _nodeTableStatistics;
      }
    }
    GetFlowTablesStatisticsInputBuilder _getFlowTablesStatisticsInputBuilder = new GetFlowTablesStatisticsInputBuilder();
    final GetFlowTablesStatisticsInputBuilder input = _getFlowTablesStatisticsInputBuilder;
    Node _node_2 = nodeTable.getNode();
    NodeRef _nodeRef = NodeMapping.toNodeRef(_node_2);
    input.setNode(_nodeRef);
    OpendaylightFlowTableStatisticsService _flowTableStatisticsService = this.getFlowTableStatisticsService();
    GetFlowTablesStatisticsInput _build = input.build();
    _flowTableStatisticsService.getFlowTablesStatistics(_build);
    return nodeStats;
  }
  
  public void onNodeConnectorRemoved(final NodeConnectorRemoved update) {
  }
  
  public void onNodeRemoved(final NodeRemoved notification) {
    try {
      final Set<Property> properties = Collections.<Property>emptySet();
      NodeRef _nodeRef = notification.getNodeRef();
      InstanceIdentifier<? extends Object> _value = _nodeRef.getValue();
      this.removeNodeConnectors(_value);
      NodeRef _nodeRef_1 = notification.getNodeRef();
      Node _aDNode = NodeMapping.toADNode(_nodeRef_1);
      this.publishNodeUpdate(_aDNode, UpdateType.REMOVED, properties);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void onNodeConnectorUpdated(final NodeConnectorUpdated update) {
    try {
      UpdateType updateType = UpdateType.CHANGED;
      NodeConnectorRef _nodeConnectorRef = update.getNodeConnectorRef();
      InstanceIdentifier<? extends Object> _value = _nodeConnectorRef.getValue();
      boolean _isKnownNodeConnector = this.isKnownNodeConnector(_value);
      boolean _not = (!_isKnownNodeConnector);
      if (_not) {
        updateType = UpdateType.ADDED;
        NodeConnectorRef _nodeConnectorRef_1 = update.getNodeConnectorRef();
        InstanceIdentifier<? extends Object> _value_1 = _nodeConnectorRef_1.getValue();
        this.recordNodeConnector(_value_1);
      }
      NodeConnectorRef _nodeConnectorRef_2 = update.getNodeConnectorRef();
      NodeConnector nodeConnector = NodeMapping.toADNodeConnector(_nodeConnectorRef_2);
      HashSet<Property> _aDNodeConnectorProperties = NodeMapping.toADNodeConnectorProperties(update);
      this.publishNodeConnectorUpdate(nodeConnector, updateType, _aDNodeConnectorProperties);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void onNodeUpdated(final NodeUpdated notification) {
    try {
      NodeRef _nodeRef = notification.getNodeRef();
      InstanceIdentifier<? extends Object> _value = _nodeRef.getValue();
      final InstanceIdentifier<? extends DataObject> identifier = ((InstanceIdentifier<? extends DataObject>) _value);
      UpdateType updateType = UpdateType.CHANGED;
      DataObject _readOperationalData = this._dataService.readOperationalData(identifier);
      boolean _equals = Objects.equal(_readOperationalData, null);
      if (_equals) {
        updateType = UpdateType.ADDED;
      }
      NodeRef _nodeRef_1 = notification.getNodeRef();
      Node _aDNode = NodeMapping.toADNode(_nodeRef_1);
      HashSet<Property> _aDNodeProperties = NodeMapping.toADNodeProperties(notification);
      this.publishNodeUpdate(_aDNode, updateType, _aDNodeProperties);
      List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
      for (final IPluginOutReadService statsPublisher : _statisticsPublisher) {
        {
          InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
          NodeId _id = notification.getId();
          NodeKey _nodeKey = new NodeKey(_id);
          InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
          final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
          NodeRef _nodeRef_2 = notification.getNodeRef();
          final NodeDescription description = this.toNodeDescription(_nodeRef_2);
          boolean _notEquals = (!Objects.equal(description, null));
          if (_notEquals) {
            Node _aDNode_1 = NodeMapping.toADNode(nodeRef);
            statsPublisher.descriptionStatisticsUpdated(_aDNode_1, description);
          }
        }
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public ConcurrentMap<Node,Map<String,Property>> getNodeProps() {
    try {
      ConcurrentHashMap<Node,Map<String,Property>> _concurrentHashMap = new ConcurrentHashMap<Node, Map<String, Property>>();
      final ConcurrentHashMap<Node,Map<String,Property>> props = _concurrentHashMap;
      final Nodes nodes = this.readAllMDNodes();
      List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _node = nodes.getNode();
      for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node : _node) {
        {
          final FlowCapableNode fcn = node.<FlowCapableNode>getAugmentation(FlowCapableNode.class);
          boolean _notEquals = (!Objects.equal(fcn, null));
          if (_notEquals) {
            NodeId _id = node.getId();
            final HashSet<Property> perNodeProps = NodeMapping.toADNodeProperties(fcn, _id);
            ConcurrentHashMap<String,Property> _concurrentHashMap_1 = new ConcurrentHashMap<String, Property>();
            final ConcurrentHashMap<String,Property> perNodePropMap = _concurrentHashMap_1;
            boolean _notEquals_1 = (!Objects.equal(perNodeProps, null));
            if (_notEquals_1) {
              for (final Property perNodeProp : perNodeProps) {
                String _name = perNodeProp.getName();
                perNodePropMap.put(_name, perNodeProp);
              }
            }
            NodeId _id_1 = node.getId();
            String _aDNodeId = NodeMapping.toADNodeId(_id_1);
            Node _node_1 = new Node(NodeMapping.MD_SAL_TYPE, _aDNodeId);
            props.put(_node_1, perNodePropMap);
          }
        }
      }
      return props;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private Nodes readAllMDNodes() {
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    final InstanceIdentifier<Nodes> nodesRef = _builder.toInstance();
    DataBrokerService _dataService = this.getDataService();
    final TypeSafeDataReader reader = TypeSafeDataReader.forReader(_dataService);
    return reader.<Nodes>readOperationalData(nodesRef);
  }
  
  private List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> readAllMDNodeConnectors(final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node) {
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    NodeId _id = node.getId();
    NodeKey _nodeKey = new NodeKey(_id);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
    DataBrokerService _dataService = this.getDataService();
    final TypeSafeDataReader reader = TypeSafeDataReader.forReader(_dataService);
    org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node _readOperationalData = reader.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>readOperationalData(nodeRef);
    return _readOperationalData.getNodeConnector();
  }
  
  public ConcurrentMap<NodeConnector,Map<String,Property>> getNodeConnectorProps(final Boolean refresh) {
    ConcurrentHashMap<NodeConnector,Map<String,Property>> _concurrentHashMap = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
    final ConcurrentHashMap<NodeConnector,Map<String,Property>> props = _concurrentHashMap;
    final Nodes nodes = this.readAllMDNodes();
    List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _node = nodes.getNode();
    for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node : _node) {
      {
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> ncs = this.readAllMDNodeConnectors(node);
        boolean _notEquals = (!Objects.equal(ncs, null));
        if (_notEquals) {
          for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc : ncs) {
            {
              final FlowCapableNodeConnector fcnc = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
              boolean _notEquals_1 = (!Objects.equal(fcnc, null));
              if (_notEquals_1) {
                final HashSet<Property> ncps = NodeMapping.toADNodeConnectorProperties(fcnc);
                ConcurrentHashMap<String,Property> _concurrentHashMap_1 = new ConcurrentHashMap<String, Property>();
                final ConcurrentHashMap<String,Property> ncpsm = _concurrentHashMap_1;
                boolean _notEquals_2 = (!Objects.equal(ncps, null));
                if (_notEquals_2) {
                  for (final Property p : ncps) {
                    String _name = p.getName();
                    ncpsm.put(_name, p);
                  }
                }
                NodeConnectorId _id = nc.getId();
                NodeId _id_1 = node.getId();
                NodeConnector _aDNodeConnector = NodeMapping.toADNodeConnector(_id, _id_1);
                props.put(_aDNodeConnector, ncpsm);
              }
            }
          }
        }
      }
    }
    return props;
  }
  
  private FlowCapableNode readFlowCapableNode(final NodeRef ref) {
    DataBrokerService _dataService = this.getDataService();
    InstanceIdentifier<? extends Object> _value = ref.getValue();
    final DataObject dataObject = _dataService.readOperationalData(((InstanceIdentifier<? extends DataObject>) _value));
    boolean _notEquals = (!Objects.equal(dataObject, null));
    if (_notEquals) {
      final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node = Arguments.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>checkInstanceOf(dataObject, 
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);
      return node.<FlowCapableNode>getAugmentation(FlowCapableNode.class);
    }
    return null;
  }
  
  private FlowCapableNodeConnector readFlowCapableNodeConnector(final NodeConnectorRef ref) {
    DataBrokerService _dataService = this.getDataService();
    InstanceIdentifier<? extends Object> _value = ref.getValue();
    final DataObject dataObject = _dataService.readOperationalData(((InstanceIdentifier<? extends DataObject>) _value));
    final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector node = Arguments.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>checkInstanceOf(dataObject, 
      org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class);
    return node.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
  }
  
  private NodeConnectorStatistics toNodeConnectorStatistics(final org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.NodeConnectorStatistics nodeConnectorStatistics, final NodeId nodeId, final NodeConnectorId nodeConnectorId) {
    try {
      NodeConnectorStatistics _nodeConnectorStatistics = new NodeConnectorStatistics();
      final NodeConnectorStatistics it = _nodeConnectorStatistics;
      Packets _packets = nodeConnectorStatistics.getPackets();
      BigInteger _received = _packets.getReceived();
      long _longValue = _received.longValue();
      it.setReceivePacketCount(_longValue);
      Packets _packets_1 = nodeConnectorStatistics.getPackets();
      BigInteger _transmitted = _packets_1.getTransmitted();
      long _longValue_1 = _transmitted.longValue();
      it.setTransmitPacketCount(_longValue_1);
      Bytes _bytes = nodeConnectorStatistics.getBytes();
      BigInteger _received_1 = _bytes.getReceived();
      long _longValue_2 = _received_1.longValue();
      it.setReceiveByteCount(_longValue_2);
      Bytes _bytes_1 = nodeConnectorStatistics.getBytes();
      BigInteger _transmitted_1 = _bytes_1.getTransmitted();
      long _longValue_3 = _transmitted_1.longValue();
      it.setTransmitByteCount(_longValue_3);
      BigInteger _receiveDrops = nodeConnectorStatistics.getReceiveDrops();
      long _longValue_4 = _receiveDrops.longValue();
      it.setReceiveDropCount(_longValue_4);
      BigInteger _transmitDrops = nodeConnectorStatistics.getTransmitDrops();
      long _longValue_5 = _transmitDrops.longValue();
      it.setTransmitDropCount(_longValue_5);
      BigInteger _receiveErrors = nodeConnectorStatistics.getReceiveErrors();
      long _longValue_6 = _receiveErrors.longValue();
      it.setReceiveErrorCount(_longValue_6);
      BigInteger _transmitErrors = nodeConnectorStatistics.getTransmitErrors();
      long _longValue_7 = _transmitErrors.longValue();
      it.setTransmitErrorCount(_longValue_7);
      BigInteger _receiveFrameError = nodeConnectorStatistics.getReceiveFrameError();
      long _longValue_8 = _receiveFrameError.longValue();
      it.setReceiveFrameErrorCount(_longValue_8);
      BigInteger _receiveOverRunError = nodeConnectorStatistics.getReceiveOverRunError();
      long _longValue_9 = _receiveOverRunError.longValue();
      it.setReceiveOverRunErrorCount(_longValue_9);
      BigInteger _receiveCrcError = nodeConnectorStatistics.getReceiveCrcError();
      long _longValue_10 = _receiveCrcError.longValue();
      it.setReceiveCRCErrorCount(_longValue_10);
      BigInteger _collisionCount = nodeConnectorStatistics.getCollisionCount();
      long _longValue_11 = _collisionCount.longValue();
      it.setCollisionCount(_longValue_11);
      InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
      NodeKey _nodeKey = new NodeKey(nodeId);
      InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
      NodeConnectorKey _nodeConnectorKey = new NodeConnectorKey(nodeConnectorId);
      InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> _child_1 = _child.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, NodeConnectorKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, _nodeConnectorKey);
      final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> nodeConnectorRef = _child_1.toInstance();
      NodeConnectorRef _nodeConnectorRef = new NodeConnectorRef(nodeConnectorRef);
      NodeConnector _aDNodeConnector = NodeMapping.toADNodeConnector(_nodeConnectorRef);
      it.setNodeConnector(_aDNodeConnector);
      return it;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private NodeTableStatistics toNodeTableStatistics(final FlowTableStatistics tableStats, final Short tableId, final Node node) {
    try {
      NodeTableStatistics _nodeTableStatistics = new NodeTableStatistics();
      NodeTableStatistics it = _nodeTableStatistics;
      Counter32 _activeFlows = tableStats.getActiveFlows();
      Long _value = _activeFlows.getValue();
      int _intValue = _value.intValue();
      it.setActiveCount(_intValue);
      Counter64 _packetsLookedUp = tableStats.getPacketsLookedUp();
      BigInteger _value_1 = _packetsLookedUp.getValue();
      int _intValue_1 = _value_1.intValue();
      it.setLookupCount(_intValue_1);
      Counter64 _packetsMatched = tableStats.getPacketsMatched();
      BigInteger _value_2 = _packetsMatched.getValue();
      int _intValue_2 = _value_2.intValue();
      it.setMatchedCount(_intValue_2);
      String _string = tableId.toString();
      it.setName(_string);
      NodeTable _nodeTable = new NodeTable(NodeMapping.MD_SAL_TYPE, tableId, node);
      it.setNodeTable(_nodeTable);
      return it;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private NodeDescription toNodeDescription(final NodeRef nodeRef) {
    final FlowCapableNode capableNode = this.readFlowCapableNode(nodeRef);
    boolean _notEquals = (!Objects.equal(capableNode, null));
    if (_notEquals) {
      NodeDescription _nodeDescription = new NodeDescription();
      final NodeDescription it = _nodeDescription;
      String _manufacturer = capableNode.getManufacturer();
      it.setManufacturer(_manufacturer);
      String _serialNumber = capableNode.getSerialNumber();
      it.setSerialNumber(_serialNumber);
      String _software = capableNode.getSoftware();
      it.setSoftware(_software);
      String _description = capableNode.getDescription();
      it.setDescription(_description);
      return it;
    }
    return null;
  }
  
  public Edge toADEdge(final Link link) {
    try {
      NodeConnectorRef _source = link.getSource();
      NodeConnector _aDNodeConnector = NodeMapping.toADNodeConnector(_source);
      NodeConnectorRef _destination = link.getDestination();
      NodeConnector _aDNodeConnector_1 = NodeMapping.toADNodeConnector(_destination);
      Edge _edge = new Edge(_aDNodeConnector, _aDNodeConnector_1);
      return _edge;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * OpendaylightFlowStatisticsListener interface implementation
   */
  public void onAggregateFlowStatisticsUpdate(final AggregateFlowStatisticsUpdate notification) {
  }
  
  public void onFlowsStatisticsUpdate(final FlowsStatisticsUpdate notification) {
    try {
      ArrayList<FlowOnNode> _arrayList = new ArrayList<FlowOnNode>();
      final ArrayList<FlowOnNode> adsalFlowsStatistics = _arrayList;
      InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
      NodeId _id = notification.getId();
      NodeKey _nodeKey = new NodeKey(_id);
      InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
      final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
      List<FlowAndStatisticsMapList> _flowAndStatisticsMapList = notification.getFlowAndStatisticsMapList();
      for (final FlowAndStatisticsMapList flowStats : _flowAndStatisticsMapList) {
        Short _tableId = flowStats.getTableId();
        boolean _equals = ((_tableId).shortValue() == 0);
        if (_equals) {
          Node _aDNode = NodeMapping.toADNode(nodeRef);
          FlowOnNode _flowOnNode = InventoryAndReadAdapter.toFlowOnNode(flowStats, _aDNode);
          adsalFlowsStatistics.add(_flowOnNode);
        }
      }
      List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
      for (final IPluginOutReadService statsPublisher : _statisticsPublisher) {
        Node _aDNode_1 = NodeMapping.toADNode(nodeRef);
        statsPublisher.nodeFlowStatisticsUpdated(_aDNode_1, adsalFlowsStatistics);
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * OpendaylightFlowTableStatisticsListener interface implementation
   */
  public void onFlowTableStatisticsUpdate(final FlowTableStatisticsUpdate notification) {
    try {
      ArrayList<NodeTableStatistics> _arrayList = new ArrayList<NodeTableStatistics>();
      ArrayList<NodeTableStatistics> adsalFlowTableStatistics = _arrayList;
      List<FlowTableAndStatisticsMap> _flowTableAndStatisticsMap = notification.getFlowTableAndStatisticsMap();
      for (final FlowTableAndStatisticsMap stats : _flowTableAndStatisticsMap) {
        TableId _tableId = stats.getTableId();
        Short _value = _tableId.getValue();
        boolean _equals = ((_value).shortValue() == 0);
        if (_equals) {
          NodeTableStatistics _nodeTableStatistics = new NodeTableStatistics();
          final NodeTableStatistics it = _nodeTableStatistics;
          Counter32 _activeFlows = stats.getActiveFlows();
          Long _value_1 = _activeFlows.getValue();
          int _intValue = _value_1.intValue();
          it.setActiveCount(_intValue);
          Counter64 _packetsLookedUp = stats.getPacketsLookedUp();
          BigInteger _value_2 = _packetsLookedUp.getValue();
          long _longValue = _value_2.longValue();
          it.setLookupCount(_longValue);
          Counter64 _packetsMatched = stats.getPacketsMatched();
          BigInteger _value_3 = _packetsMatched.getValue();
          long _longValue_1 = _value_3.longValue();
          it.setMatchedCount(_longValue_1);
          adsalFlowTableStatistics.add(it);
        }
      }
      List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
      for (final IPluginOutReadService statsPublisher : _statisticsPublisher) {
        {
          InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
          NodeId _id = notification.getId();
          NodeKey _nodeKey = new NodeKey(_id);
          InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
          final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
          Node _aDNode = NodeMapping.toADNode(nodeRef);
          statsPublisher.nodeTableStatisticsUpdated(_aDNode, adsalFlowTableStatistics);
        }
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * OpendaylightPortStatisticsUpdate interface implementation
   */
  public void onNodeConnectorStatisticsUpdate(final NodeConnectorStatisticsUpdate notification) {
    try {
      ArrayList<NodeConnectorStatistics> _arrayList = new ArrayList<NodeConnectorStatistics>();
      final ArrayList<NodeConnectorStatistics> adsalPortStatistics = _arrayList;
      List<NodeConnectorStatisticsAndPortNumberMap> _nodeConnectorStatisticsAndPortNumberMap = notification.getNodeConnectorStatisticsAndPortNumberMap();
      for (final NodeConnectorStatisticsAndPortNumberMap nodeConnectorStatistics : _nodeConnectorStatisticsAndPortNumberMap) {
        NodeId _id = notification.getId();
        NodeConnectorId _nodeConnectorId = nodeConnectorStatistics.getNodeConnectorId();
        NodeConnectorStatistics _nodeConnectorStatistics = this.toNodeConnectorStatistics(nodeConnectorStatistics, _id, _nodeConnectorId);
        adsalPortStatistics.add(_nodeConnectorStatistics);
      }
      List<IPluginOutReadService> _statisticsPublisher = this.getStatisticsPublisher();
      for (final IPluginOutReadService statsPublisher : _statisticsPublisher) {
        {
          InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
          NodeId _id_1 = notification.getId();
          NodeKey _nodeKey = new NodeKey(_id_1);
          InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
          final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef = _child.toInstance();
          Node _aDNode = NodeMapping.toADNode(nodeRef);
          statsPublisher.nodeConnectorStatisticsUpdated(_aDNode, adsalPortStatistics);
        }
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private static FlowOnNode toFlowOnNode(final FlowAndStatisticsMapList flowAndStatsMap, final Node node) {
    org.opendaylight.controller.sal.flowprogrammer.Flow _flow = ToSalConversionsUtils.toFlow(flowAndStatsMap, node);
    FlowOnNode _flowOnNode = new FlowOnNode(_flow);
    final FlowOnNode it = _flowOnNode;
    Counter64 _byteCount = flowAndStatsMap.getByteCount();
    BigInteger _value = _byteCount.getValue();
    long _longValue = _value.longValue();
    it.setByteCount(_longValue);
    Counter64 _packetCount = flowAndStatsMap.getPacketCount();
    BigInteger _value_1 = _packetCount.getValue();
    long _longValue_1 = _value_1.longValue();
    it.setPacketCount(_longValue_1);
    Duration _duration = flowAndStatsMap.getDuration();
    Counter32 _second = _duration.getSecond();
    Long _value_2 = _second.getValue();
    int _intValue = _value_2.intValue();
    it.setDurationSeconds(_intValue);
    Duration _duration_1 = flowAndStatsMap.getDuration();
    Counter32 _nanosecond = _duration_1.getNanosecond();
    Long _value_3 = _nanosecond.getValue();
    int _intValue_1 = _value_3.intValue();
    it.setDurationNanoseconds(_intValue_1);
    return it;
  }
  
  public Set<Node> getConfiguredNotConnectedNodes() {
    return Collections.<Node>emptySet();
  }
  
  private void publishNodeUpdate(final Node node, final UpdateType updateType, final Set<Property> properties) {
    List<IPluginOutInventoryService> _inventoryPublisher = this.getInventoryPublisher();
    for (final IPluginOutInventoryService publisher : _inventoryPublisher) {
      publisher.updateNode(node, updateType, properties);
    }
  }
  
  private void publishNodeConnectorUpdate(final NodeConnector nodeConnector, final UpdateType updateType, final Set<Property> properties) {
    List<IPluginOutInventoryService> _inventoryPublisher = this.getInventoryPublisher();
    for (final IPluginOutInventoryService publisher : _inventoryPublisher) {
      publisher.updateNodeConnector(nodeConnector, updateType, properties);
    }
  }
  
  private boolean isKnownNodeConnector(final InstanceIdentifier<? extends Object> nodeConnectorIdentifier) {
    List<PathArgument> _path = nodeConnectorIdentifier.getPath();
    int _size = _path.size();
    boolean _lessThan = (_size < 3);
    if (_lessThan) {
      return false;
    }
    List<PathArgument> _path_1 = nodeConnectorIdentifier.getPath();
    final PathArgument nodePath = _path_1.get(1);
    List<PathArgument> _path_2 = nodeConnectorIdentifier.getPath();
    final PathArgument nodeConnectorPath = _path_2.get(2);
    final List<PathArgument> nodeConnectors = this.nodeToNodeConnectorsMap.get(nodePath);
    boolean _equals = Objects.equal(nodeConnectors, null);
    if (_equals) {
      return false;
    }
    return nodeConnectors.contains(nodeConnectorPath);
  }
  
  private boolean recordNodeConnector(final InstanceIdentifier<? extends Object> nodeConnectorIdentifier) {
    boolean _xblockexpression = false;
    {
      List<PathArgument> _path = nodeConnectorIdentifier.getPath();
      int _size = _path.size();
      boolean _lessThan = (_size < 3);
      if (_lessThan) {
        return false;
      }
      List<PathArgument> _path_1 = nodeConnectorIdentifier.getPath();
      final PathArgument nodePath = _path_1.get(1);
      List<PathArgument> _path_2 = nodeConnectorIdentifier.getPath();
      final PathArgument nodeConnectorPath = _path_2.get(2);
      this.nodeToNodeConnectorsLock.lock();
      boolean _xtrycatchfinallyexpression = false;
      try {
        boolean _xblockexpression_1 = false;
        {
          List<PathArgument> nodeConnectors = this.nodeToNodeConnectorsMap.get(nodePath);
          boolean _equals = Objects.equal(nodeConnectors, null);
          if (_equals) {
            ArrayList<PathArgument> _arrayList = new ArrayList<PathArgument>();
            nodeConnectors = _arrayList;
            this.nodeToNodeConnectorsMap.put(nodePath, nodeConnectors);
          }
          boolean _add = nodeConnectors.add(nodeConnectorPath);
          _xblockexpression_1 = (_add);
        }
        _xtrycatchfinallyexpression = _xblockexpression_1;
      } finally {
        this.nodeToNodeConnectorsLock.unlock();
      }
      _xblockexpression = (_xtrycatchfinallyexpression);
    }
    return _xblockexpression;
  }
  
  private List<PathArgument> removeNodeConnectors(final InstanceIdentifier<? extends Object> nodeIdentifier) {
    List<PathArgument> _xblockexpression = null;
    {
      List<PathArgument> _path = nodeIdentifier.getPath();
      final PathArgument nodePath = _path.get(1);
      List<PathArgument> _remove = this.nodeToNodeConnectorsMap.remove(nodePath);
      _xblockexpression = (_remove);
    }
    return _xblockexpression;
  }
}
