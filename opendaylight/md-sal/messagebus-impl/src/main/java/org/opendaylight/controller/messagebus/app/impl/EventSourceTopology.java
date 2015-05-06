/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.DestroyTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.EventAggregatorService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.Node1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.topology.event.source.type.TopologyEventSource;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.topology.event.source.type.TopologyEventSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;


public class EventSourceTopology implements EventAggregatorService, EventSourceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceTopology.class);

    private static final String TOPOLOGY_ID = "EVENT-SOURCE-TOPOLOGY" ;
    private static final TopologyKey EVENT_SOURCE_TOPOLOGY_KEY = new TopologyKey(new TopologyId(TOPOLOGY_ID));
    private static final LogicalDatastoreType OPERATIONAL = LogicalDatastoreType.OPERATIONAL;

    private static final InstanceIdentifier<Topology> EVENT_SOURCE_TOPOLOGY_PATH =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, EVENT_SOURCE_TOPOLOGY_KEY);

    private static final InstanceIdentifier<TopologyTypes1> TOPOLOGY_TYPE_PATH =
            EVENT_SOURCE_TOPOLOGY_PATH
                    .child(TopologyTypes.class)
                    .augmentation(TopologyTypes1.class);

    private final Map<EventSourceTopic, ListenerRegistration<DataChangeListener>> topicListenerRegistrations =
            new ConcurrentHashMap<>();
    private final Map<NodeKey, RoutedRpcRegistration<EventSourceService>> routedRpcRegistrations =
            new ConcurrentHashMap<>();

    private final DataBroker dataBroker;
    private final RpcRegistration<EventAggregatorService> aggregatorRpcReg;
    private final EventSourceService eventSourceService;
    private final RpcProviderRegistry rpcRegistry;

    public EventSourceTopology(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry) {

        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        aggregatorRpcReg = rpcRegistry.addRpcImplementation(EventAggregatorService.class, this);
        eventSourceService = rpcRegistry.getRpcService(EventSourceService.class);

        final TopologyEventSource topologySource = new TopologyEventSourceBuilder().build();
        final TopologyTypes1 topologyTypeAugment = new TopologyTypes1Builder().setTopologyEventSource(topologySource).build();
        putData(OPERATIONAL, TOPOLOGY_TYPE_PATH, topologyTypeAugment);
        LOG.info("EventSourceRegistry has been initialized");
    }

    private <T extends DataObject>  void putData(final LogicalDatastoreType store,
                                                 final InstanceIdentifier<T> path,
                                                 final T data){

        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(store, path, data, true);
        tx.submit();

    }

    private <T extends DataObject>  void deleteData(final LogicalDatastoreType store, final InstanceIdentifier<T> path){
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(OPERATIONAL, path);
        tx.submit();
    }

    private void insert(final KeyedInstanceIdentifier<Node, NodeKey> sourcePath) {
        final NodeKey nodeKey = sourcePath.getKey();
        final InstanceIdentifier<Node1> augmentPath = sourcePath.augmentation(Node1.class);
        final Node1 nodeAgument = new Node1Builder().setEventSourceNode(new NodeId(nodeKey.getNodeId().getValue())).build();
        putData(OPERATIONAL, augmentPath, nodeAgument);
    }

    private void remove(final KeyedInstanceIdentifier<Node, NodeKey> sourcePath){
        final InstanceIdentifier<Node1> augmentPath = sourcePath.augmentation(Node1.class);
        deleteData(OPERATIONAL, augmentPath);
    }

    private void notifyExistingNodes(final EventSourceTopic eventSourceTopic){

        final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();

        final CheckedFuture<Optional<Topology>, ReadFailedException> future = tx.read(OPERATIONAL, EVENT_SOURCE_TOPOLOGY_PATH);

        Futures.addCallback(future, new FutureCallback<Optional<Topology>>(){

            @Override
            public void onSuccess(Optional<Topology> data) {
                if(data.isPresent()) {
                    LOG.info("Topology data are present...");
                     final List<Node> nodes = data.get().getNode();
                     if(nodes != null){
                         LOG.info("List of nodes is not null...");
                         final Pattern nodeIdPatternRegex = eventSourceTopic.getNodeIdRegexPattern();
                     for (final Node node : nodes) {
                         if (nodeIdPatternRegex.matcher(node.getNodeId().getValue()).matches()) {
                             eventSourceTopic.notifyNode(EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.getKey()));
                         }
                     }
                     } else {
                         LOG.info("List of nodes is NULL...");
                     }
                }
                tx.close();
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Can not notify existing nodes {}", t);
                tx.close();
            }

        });

    }

    @Override
    public Future<RpcResult<CreateTopicOutput>> createTopic(final CreateTopicInput input) {
        LOG.info("Received Topic creation request: NotificationPattern -> {}, NodeIdPattern -> {}",
                input.getNotificationPattern(),
                input.getNodeIdPattern());

        final NotificationPattern notificationPattern = new NotificationPattern(input.getNotificationPattern());
        final String nodeIdPattern = input.getNodeIdPattern().getValue();
        final EventSourceTopic eventSourceTopic = new EventSourceTopic(notificationPattern, nodeIdPattern, eventSourceService);

        registerTopic(eventSourceTopic);

        notifyExistingNodes(eventSourceTopic);

        final CreateTopicOutput cto = new CreateTopicOutputBuilder()
                .setTopicId(eventSourceTopic.getTopicId())
                .build();

        return Util.resultRpcSuccessFor(cto);
    }

    @Override
    public Future<RpcResult<Void>> destroyTopic(final DestroyTopicInput input) {
        return Futures.immediateFailedFuture(new UnsupportedOperationException("Not Implemented"));
    }

    @Override
    public void close() {
        aggregatorRpcReg.close();
        for(ListenerRegistration<DataChangeListener> reg : topicListenerRegistrations.values()){
            reg.close();
        }
    }

    private void registerTopic(final EventSourceTopic listener) {
        final ListenerRegistration<DataChangeListener> listenerRegistration = dataBroker.registerDataChangeListener(OPERATIONAL,
                EVENT_SOURCE_TOPOLOGY_PATH,
                listener,
                DataBroker.DataChangeScope.SUBTREE);

        topicListenerRegistrations.put(listener, listenerRegistration);
    }

    public void register(final EventSource eventSource){
        NodeKey nodeKey = eventSource.getSourceNodeKey();
        final KeyedInstanceIdentifier<Node, NodeKey> sourcePath = EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, nodeKey);
        RoutedRpcRegistration<EventSourceService> reg = rpcRegistry.addRoutedRpcImplementation(EventSourceService.class, eventSource);
        reg.registerPath(NodeContext.class, sourcePath);
        routedRpcRegistrations.put(nodeKey,reg);
        insert(sourcePath);

        for(EventSourceTopic est : topicListenerRegistrations.keySet()){
            if(est.getNodeIdRegexPattern().matcher(nodeKey.getNodeId().getValue()).matches()){
                est.notifyNode(EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, nodeKey));
            }
        }
    }

    public void unRegister(final EventSource eventSource){
        final NodeKey nodeKey = eventSource.getSourceNodeKey();
        final KeyedInstanceIdentifier<Node, NodeKey> sourcePath = EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, nodeKey);
        final RoutedRpcRegistration<EventSourceService> removeRegistration = routedRpcRegistrations.remove(nodeKey);
        if(removeRegistration != null){
            removeRegistration.close();
        remove(sourcePath);
        }
    }

    @Override
    public <T extends EventSource> EventSourceRegistration<T> registerEventSource(
            T eventSource) {
        EventSourceRegistrationImpl<T> esr = new EventSourceRegistrationImpl<>(eventSource, this);
        register(eventSource);
        return esr;
    }
}

