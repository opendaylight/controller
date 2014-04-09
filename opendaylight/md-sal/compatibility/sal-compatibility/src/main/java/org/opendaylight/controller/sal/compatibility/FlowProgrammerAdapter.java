/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Objects;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class FlowProgrammerAdapter implements IPluginInFlowProgrammerService, SalFlowListener {
  private final static Logger LOG = new Function0<Logger>() {
    public Logger apply() {
      Logger _logger = LoggerFactory.getLogger(FlowProgrammerAdapter.class);
      return _logger;
    }
  }.apply();
  
  private final static String CACHE_NAME = "flowprogrammeradapter.flowtoid";
  
  private SalFlowService _delegate;
  
  public SalFlowService getDelegate() {
    return this._delegate;
  }
  
  public void setDelegate(final SalFlowService delegate) {
    this._delegate = delegate;
  }
  
  private DataBrokerService _dataBrokerService;
  
  public DataBrokerService getDataBrokerService() {
    return this._dataBrokerService;
  }
  
  public void setDataBrokerService(final DataBrokerService dataBrokerService) {
    this._dataBrokerService = dataBrokerService;
  }
  
  private IPluginOutFlowProgrammerService _flowProgrammerPublisher;
  
  public IPluginOutFlowProgrammerService getFlowProgrammerPublisher() {
    return this._flowProgrammerPublisher;
  }
  
  public void setFlowProgrammerPublisher(final IPluginOutFlowProgrammerService flowProgrammerPublisher) {
    this._flowProgrammerPublisher = flowProgrammerPublisher;
  }
  
  private IClusterGlobalServices _clusterGlobalServices;
  
  public IClusterGlobalServices getClusterGlobalServices() {
    return this._clusterGlobalServices;
  }
  
  public void setClusterGlobalServices(final IClusterGlobalServices clusterGlobalServices) {
    this._clusterGlobalServices = clusterGlobalServices;
  }
  
  private Map<Flow,UUID> _flowToFlowId = new Function0<Map<Flow,UUID>>() {
    public Map<Flow,UUID> apply() {
      ConcurrentHashMap<Flow,UUID> _concurrentHashMap = new ConcurrentHashMap<Flow, UUID>();
      return _concurrentHashMap;
    }
  }.apply();
  
  public Map<Flow,UUID> getFlowToFlowId() {
    return this._flowToFlowId;
  }
  
  public void setFlowToFlowId(final Map<Flow,UUID> flowToFlowId) {
    this._flowToFlowId = flowToFlowId;
  }
  
  public Status addFlow(final Node node, final Flow flow) {
    Future<RpcResult<TransactionStatus>> _internalAddFlowAsync = this.internalAddFlowAsync(node, flow, 0);
    return this.toFutureStatus(_internalAddFlowAsync);
  }
  
  public Status modifyFlow(final Node node, final Flow oldFlow, final Flow newFlow) {
    Future<RpcResult<TransactionStatus>> _internalModifyFlowAsync = this.internalModifyFlowAsync(node, oldFlow, newFlow, 0);
    return this.toFutureStatus(_internalModifyFlowAsync);
  }
  
  public Status removeFlow(final Node node, final Flow flow) {
    Future<RpcResult<TransactionStatus>> _internalRemoveFlowAsync = this.internalRemoveFlowAsync(node, flow, 0);
    return this.toFutureStatus(_internalRemoveFlowAsync);
  }
  
  public Status addFlowAsync(final Node node, final Flow flow, final long rid) {
    this.internalAddFlowAsync(node, flow, rid);
    return FlowProgrammerAdapter.toStatus(true);
  }
  
  public Status modifyFlowAsync(final Node node, final Flow oldFlow, final Flow newFlow, final long rid) {
    this.internalModifyFlowAsync(node, oldFlow, newFlow, rid);
    return FlowProgrammerAdapter.toStatus(true);
  }
  
  public Status removeFlowAsync(final Node node, final Flow flow, final long rid) {
    this.internalRemoveFlowAsync(node, flow, rid);
    return FlowProgrammerAdapter.toStatus(true);
  }
  
  public Status removeAllFlows(final Node node) {
    Status _status = new Status(StatusCode.SUCCESS);
    return _status;
  }
  
  public Status syncSendBarrierMessage(final Node node) {
    return null;
  }
  
  public Status asyncSendBarrierMessage(final Node node) {
    return null;
  }
  
  private static Status toStatus(final boolean successful) {
    if (successful) {
      Status _status = new Status(StatusCode.SUCCESS);
      return _status;
    } else {
      Status _status_1 = new Status(StatusCode.INTERNALERROR);
      return _status_1;
    }
  }
  
  public static Status toStatus(final RpcResult<? extends Object> result) {
    boolean _isSuccessful = result.isSuccessful();
    return FlowProgrammerAdapter.toStatus(_isSuccessful);
  }
  
  private static Status _processException(final InterruptedException e) {
    FlowProgrammerAdapter.LOG.error("Interruption occured during processing flow", e);
    Status _status = new Status(StatusCode.INTERNALERROR);
    return _status;
  }
  
  private static Status _processException(final ExecutionException e) {
    Throwable _cause = e.getCause();
    FlowProgrammerAdapter.LOG.error("Execution exception occured during processing flow", _cause);
    Status _status = new Status(StatusCode.INTERNALERROR);
    return _status;
  }
  
  private static Status _processException(final Exception e) {
    RuntimeException _runtimeException = new RuntimeException(e);
    throw _runtimeException;
  }
  
  public void onFlowAdded(final FlowAdded notification) {
  }
  
  public void onFlowRemoved(final FlowRemoved notification) {
    try {
      boolean _and = false;
      boolean _notEquals = (!Objects.equal(notification, null));
      if (!_notEquals) {
        _and = false;
      } else {
        NodeRef _node = notification.getNode();
        boolean _notEquals_1 = (!Objects.equal(_node, null));
        _and = (_notEquals && _notEquals_1);
      }
      if (_and) {
        NodeRef _node_1 = notification.getNode();
        final Node adNode = NodeMapping.toADNode(_node_1);
        boolean _notEquals_2 = (!Objects.equal(adNode, null));
        if (_notEquals_2) {
          IPluginOutFlowProgrammerService _flowProgrammerPublisher = this.getFlowProgrammerPublisher();
          Flow _flow = ToSalConversionsUtils.toFlow(notification, adNode);
          _flowProgrammerPublisher.flowRemoved(adNode, _flow);
        }
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void onFlowUpdated(final FlowUpdated notification) {
  }
  
  public void onSwitchFlowRemoved(final SwitchFlowRemoved notification) {
  }
  
  public void onNodeErrorNotification(final NodeErrorNotification notification) {
  }
  
  public void onNodeExperimenterErrorNotification(final NodeExperimenterErrorNotification notification) {
  }
  
  private Future<RpcResult<TransactionStatus>> writeFlowAsync(final org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow, final NodeKey nodeKey) {
    final DataModificationTransaction modification = this._dataBrokerService.beginTransaction();
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey);
    InstanceIdentifierBuilder<FlowCapableNode> _augmentation = _child.<FlowCapableNode>augmentation(FlowCapableNode.class);
    Short _tableId = flow.getTableId();
    TableKey _tableKey = new TableKey(_tableId);
    InstanceIdentifierBuilder<Table> _child_1 = _augmentation.<Table, TableKey>child(Table.class, _tableKey);
    FlowId _id = flow.getId();
    FlowKey _flowKey = new FlowKey(_id);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> _child_2 = _child_1.<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, FlowKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class, _flowKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flowPath = _child_2.build();
    modification.putConfigurationData(flowPath, flow);
    return modification.commit();
  }
  
  private Future<RpcResult<TransactionStatus>> internalAddFlowAsync(final Node node, final Flow flow, final long rid) {
    Map<Flow,UUID> _cache = this.getCache();
    UUID flowId = _cache.get(flow);
    boolean _notEquals = (!Objects.equal(flowId, null));
    if (_notEquals) {
      this.removeFlow(node, flow);
      return this.internalAddFlowAsync(node, flow, rid);
    }
    UUID _randomUUID = UUID.randomUUID();
    flowId = _randomUUID;
    Map<Flow,UUID> _cache_1 = this.getCache();
    _cache_1.put(flow, flowId);
    String _string = flowId.toString();
    org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow _mDFlow = MDFlowMapping.toMDFlow(flow, _string);
    String _nodeIDString = node.getNodeIDString();
    NodeId _nodeId = new NodeId(_nodeIDString);
    NodeKey _nodeKey = new NodeKey(_nodeId);
    return this.writeFlowAsync(_mDFlow, _nodeKey);
  }
  
  private Future<RpcResult<TransactionStatus>> internalModifyFlowAsync(final Node node, final Flow oldFlow, final Flow newFlow, final long rid) {
    Map<Flow,UUID> _cache = this.getCache();
    UUID flowId = _cache.remove(oldFlow);
    boolean _equals = Objects.equal(flowId, null);
    if (_equals) {
      int _hashCode = oldFlow.hashCode();
      String _plus = ("oldFlow not found in cache : " + Integer.valueOf(_hashCode));
      FlowProgrammerAdapter.LOG.error(_plus);
      UUID _randomUUID = UUID.randomUUID();
      flowId = _randomUUID;
      Map<Flow,UUID> _cache_1 = this.getCache();
      _cache_1.put(oldFlow, flowId);
    }
    Map<Flow,UUID> _cache_2 = this.getCache();
    _cache_2.put(newFlow, flowId);
    String _string = flowId.toString();
    org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow _mDFlow = MDFlowMapping.toMDFlow(newFlow, _string);
    String _nodeIDString = node.getNodeIDString();
    NodeId _nodeId = new NodeId(_nodeIDString);
    NodeKey _nodeKey = new NodeKey(_nodeId);
    return this.writeFlowAsync(_mDFlow, _nodeKey);
  }
  
  private Future<RpcResult<TransactionStatus>> internalRemoveFlowAsync(final Node node, final Flow adflow, final long rid) {
    Map<Flow,UUID> _cache = this.getCache();
    final UUID flowId = _cache.remove(adflow);
    boolean _equals = Objects.equal(flowId, null);
    if (_equals) {
      int _hashCode = adflow.hashCode();
      String _plus = ("adflow not found in cache : " + Integer.valueOf(_hashCode));
      FlowProgrammerAdapter.LOG.error(_plus);
      return null;
    }
    String _string = flowId.toString();
    final org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow = MDFlowMapping.toMDFlow(adflow, _string);
    final DataModificationTransaction modification = this._dataBrokerService.beginTransaction();
    InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    String _nodeIDString = node.getNodeIDString();
    NodeId _nodeId = new NodeId(_nodeIDString);
    NodeKey _nodeKey = new NodeKey(_nodeId);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> _child = _builder.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, _nodeKey);
    InstanceIdentifierBuilder<FlowCapableNode> _augmentation = _child.<FlowCapableNode>augmentation(FlowCapableNode.class);
    Short _tableId = flow.getTableId();
    TableKey _tableKey = new TableKey(_tableId);
    InstanceIdentifierBuilder<Table> _child_1 = _augmentation.<Table, TableKey>child(Table.class, _tableKey);
    FlowId _id = flow.getId();
    FlowKey _flowKey = new FlowKey(_id);
    InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> _child_2 = _child_1.<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, FlowKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class, _flowKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flowPath = _child_2.build();
    modification.removeConfigurationData(flowPath);
    return modification.commit();
  }
  
  private Status toFutureStatus(final Future<RpcResult<TransactionStatus>> future) {
    boolean _equals = Objects.equal(future, null);
    if (_equals) {
      return FlowProgrammerAdapter.toStatus(true);
    }
    try {
      final RpcResult<TransactionStatus> result = future.get();
      return FlowProgrammerAdapter.toStatus(result);
    } catch (final Throwable _t) {
      if (_t instanceof InterruptedException) {
        final InterruptedException e = (InterruptedException)_t;
        return FlowProgrammerAdapter.processException(e);
      } else if (_t instanceof ExecutionException) {
        final ExecutionException e_1 = (ExecutionException)_t;
        return FlowProgrammerAdapter.processException(e_1);
      } else if (_t instanceof Exception) {
        final Exception e_2 = (Exception)_t;
        FlowProgrammerAdapter.processException(e_2);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    return FlowProgrammerAdapter.toStatus(false);
  }
  
  private Map<Flow,UUID> getCache() {
    try {
      IClusterGlobalServices _clusterGlobalServices = this.getClusterGlobalServices();
      boolean _equals = Objects.equal(_clusterGlobalServices, null);
      if (_equals) {
        ConcurrentHashMap<Flow,UUID> _concurrentHashMap = new ConcurrentHashMap<Flow, UUID>();
        return _concurrentHashMap;
      }
      IClusterGlobalServices _clusterGlobalServices_1 = this.getClusterGlobalServices();
      ConcurrentMap<? extends Object,? extends Object> cache = _clusterGlobalServices_1.getCache(FlowProgrammerAdapter.CACHE_NAME);
      boolean _equals_1 = Objects.equal(cache, null);
      if (_equals_1) {
        try {
          IClusterGlobalServices _clusterGlobalServices_2 = this.getClusterGlobalServices();
          EnumSet<cacheMode> _of = EnumSet.<cacheMode>of(cacheMode.TRANSACTIONAL);
          ConcurrentMap<? extends Object,? extends Object> _createCache = _clusterGlobalServices_2.createCache(FlowProgrammerAdapter.CACHE_NAME, _of);
          cache = _createCache;
        } catch (final Throwable _t) {
          if (_t instanceof CacheExistException) {
            final CacheExistException e = (CacheExistException)_t;
            IClusterGlobalServices _clusterGlobalServices_3 = this.getClusterGlobalServices();
            ConcurrentMap<? extends Object,? extends Object> _cache = _clusterGlobalServices_3.getCache(FlowProgrammerAdapter.CACHE_NAME);
            cache = _cache;
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
      }
      return ((Map<Flow,UUID>) cache);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private static Status processException(final Exception e) {
    if (e instanceof InterruptedException) {
      return _processException((InterruptedException)e);
    } else if (e instanceof ExecutionException) {
      return _processException((ExecutionException)e);
    } else if (e != null) {
      return _processException(e);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(e).toString());
    }
  }
}
