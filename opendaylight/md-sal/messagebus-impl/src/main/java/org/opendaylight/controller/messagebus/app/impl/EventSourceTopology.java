/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.messagebus.app.util.Util;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventSourceTopology implements EventAggregatorService, EventSourceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceTopology.class);

    private static final String TOPOLOGY_ID = "EVENT-SOURCE-TOPOLOGY" ;
    private static final TopologyKey EVENT_SOURCE_TOPOLOGY_KEY = new TopologyKey(new TopologyId(TOPOLOGY_ID));
    private static final LogicalDatastoreType OPERATIONAL = LogicalDatastoreType.OPERATIONAL;

    static final InstanceIdentifier<Topology> EVENT_SOURCE_TOPOLOGY_PATH =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, EVENT_SOURCE_TOPOLOGY_KEY);

    private static final InstanceIdentifier<TopologyTypes1> TOPOLOGY_TYPE_PATH =
            EVENT_SOURCE_TOPOLOGY_PATH
                    .child(TopologyTypes.class)
                    .augmentation(TopologyTypes1.class);

    private final Map<TopicId,EventSourceTopic> eventSourceTopicMap = new ConcurrentHashMap<>();
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

        final WriteTransaction tx = getDataBroker().newWriteOnlyTransaction();
        tx.put(store, path, data, true);
        Futures.addCallback( tx.submit(), new FutureCallback<Void>(){

            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Data has put into datastore {} {}", store, path);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Can not put data into datastore [store: {}] [path: {}] [exception: {}]",store,path, t);
            }
        });

    }

    private <T extends DataObject>  void deleteData(final LogicalDatastoreType store, final InstanceIdentifier<T> path){
        final WriteTransaction tx = getDataBroker().newWriteOnlyTransaction();
        tx.delete(OPERATIONAL, path);
        Futures.addCallback( tx.submit(), new FutureCallback<Void>(){

            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Data has deleted from datastore {} {}", store, path);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Can not delete data from datastore [store: {}] [path: {}] [exception: {}]",store,path, t);
            }

        });
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

    @Override
    public Future<RpcResult<CreateTopicOutput>> createTopic(final CreateTopicInput input) {
        LOG.debug("Received Topic creation request: NotificationPattern -> {}, NodeIdPattern -> {}",
                input.getNotificationPattern(),
                input.getNodeIdPattern());

        final NotificationPattern notificationPattern = new NotificationPattern(input.getNotificationPattern());
        //FIXME: do not use Util.wildcardToRegex - NodeIdPatter should be regex
        final String nodeIdPattern = input.getNodeIdPattern().getValue();
        final EventSourceTopic eventSourceTopic = EventSourceTopic.create(notificationPattern, nodeIdPattern, this);

        eventSourceTopicMap.put(eventSourceTopic.getTopicId(), eventSourceTopic);

        final CreateTopicOutput cto = new CreateTopicOutputBuilder()
                .setTopicId(eventSourceTopic.getTopicId())
                .build();

        LOG.info("Topic has been created: NotificationPattern -> {}, NodeIdPattern -> {}",
                input.getNotificationPattern(),
                input.getNodeIdPattern());

        return Util.resultRpcSuccessFor(cto);
    }

    @Override
    public Future<RpcResult<Void>> destroyTopic(final DestroyTopicInput input) {
        final EventSourceTopic topicToDestroy = eventSourceTopicMap.remove(input.getTopicId());
        if(topicToDestroy != null){
            topicToDestroy.close();
        }
        return Util.resultRpcSuccessFor((Void) null);
    }

    @Override
    public void close() {
        aggregatorRpcReg.close();
        for(final EventSourceTopic est : eventSourceTopicMap.values()){
            est.close();
        }
    }

    public void register(final EventSource eventSource){

        final NodeKey nodeKey = eventSource.getSourceNodeKey();
        final KeyedInstanceIdentifier<Node, NodeKey> sourcePath = EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, nodeKey);
        final RoutedRpcRegistration<EventSourceService> reg = rpcRegistry.addRoutedRpcImplementation(EventSourceService.class, eventSource);
        reg.registerPath(NodeContext.class, sourcePath);
        routedRpcRegistrations.put(nodeKey,reg);
        insert(sourcePath);

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
    public <T extends EventSource> EventSourceRegistration<T> registerEventSource(final T eventSource) {
        final EventSourceRegistrationImpl<T> esr = new EventSourceRegistrationImpl<>(eventSource, this);
        register(eventSource);
        return esr;
    }

    DataBroker getDataBroker() {
        return dataBroker;
    }

    EventSourceService getEventSourceService() {
        return eventSourceService;
    }
}

