/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;

public class EventSourceTopology implements EventAggregatorService, AutoCloseable {
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

    private final Map<DataChangeListener, ListenerRegistration<DataChangeListener>> registrations =
            new ConcurrentHashMap<>();

    private final DataBroker dataBroker;
    private final RpcRegistration<EventAggregatorService> aggregatorRpcReg;
    private final EventSourceService eventSourceService;
    private final RpcProviderRegistry rpcRegistry;

    private final ListenableFutureTask<List<Node>> nodeSnapShot =  ListenableFutureTask.create(new Callable<List<Node>>(){

        @Override
        public List<Node> call() throws Exception {
            try (ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();) {

                final Optional<Topology> data = tx.read(OPERATIONAL, EVENT_SOURCE_TOPOLOGY_PATH).checkedGet();

                if(data.isPresent()) {
                    final List<Node> nodeList = data.get().getNode();
                    if(nodeList != null) {
                        return nodeList;
                    }
                }
                return Collections.emptyList();
            } catch (final ReadFailedException e) {
                throw new ExecutionException("Unable to retrieve node list.",e);
            }
        }

    });

    public EventSourceTopology(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry) {

        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        aggregatorRpcReg = rpcRegistry.addRpcImplementation(EventAggregatorService.class, this);
        eventSourceService = rpcRegistry.getRpcService(EventSourceService.class);

        final TopologyEventSource topologySource = new TopologyEventSourceBuilder().build();
        final TopologyTypes1 topologyTypeAugment = new TopologyTypes1Builder().setTopologyEventSource(topologySource).build();
        putData(OPERATIONAL, TOPOLOGY_TYPE_PATH, topologyTypeAugment);

    }

    private <T extends DataObject>  void putData(final LogicalDatastoreType store,
                                                 final InstanceIdentifier<T> path,
                                                 final T data){

        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(store, path, data, true);
        tx.submit();

    }

    private void insert(final KeyedInstanceIdentifier<Node, NodeKey> sourcePath, final Node node) {
        final NodeKey nodeKey = node.getKey();
        final InstanceIdentifier<Node1> augmentPath = sourcePath.augmentation(Node1.class);
        final Node1 nodeAgument = new Node1Builder().setEventSourceNode(new NodeId(nodeKey.getNodeId().getValue())).build();
        putData(OPERATIONAL, augmentPath, nodeAgument);
    }

    private void notifyExistingNodes(final Pattern nodeIdPatternRegex, final EventSourceTopic eventSourceTopic){

        Futures.addCallback(nodeSnapShot, new FutureCallback<List<Node>>(){

            @Override
            public void onSuccess(List<Node> nodes) {
                for (final Node node : nodes) {
                    if (nodeIdPatternRegex.matcher(node.getNodeId().getValue()).matches()) {
                        eventSourceTopic.notifyNode(EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.getKey()));
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Can not notify existing nodes {}", t);
            }

        });

        nodeSnapShot.run();

    }

    @Override
    public Future<RpcResult<CreateTopicOutput>> createTopic(final CreateTopicInput input) {
        LOG.info("Received Topic creation request: NotificationPattern -> {}, NodeIdPattern -> {}",
                input.getNotificationPattern(),
                input.getNodeIdPattern());

        final NotificationPattern notificationPattern = new NotificationPattern(input.getNotificationPattern());
        final String nodeIdPattern = input.getNodeIdPattern().getValue();
        final Pattern nodeIdPatternRegex = Pattern.compile(Util.wildcardToRegex(nodeIdPattern));
        final EventSourceTopic eventSourceTopic = new EventSourceTopic(notificationPattern, input.getNodeIdPattern().getValue(), eventSourceService);

        registerTopic(eventSourceTopic);

        notifyExistingNodes(nodeIdPatternRegex, eventSourceTopic);

        final CreateTopicOutput cto = new CreateTopicOutputBuilder()
                .setTopicId(eventSourceTopic.getTopicId())
                .build();

        return Util.resultFor(cto);
    }

    @Override
    public Future<RpcResult<Void>> destroyTopic(final DestroyTopicInput input) {
        return Futures.immediateFailedFuture(new UnsupportedOperationException("Not Implemented"));
    }

    @Override
    public void close() {
        aggregatorRpcReg.close();
    }

    public void registerTopic(final EventSourceTopic listener) {
        final ListenerRegistration<DataChangeListener> listenerRegistration = dataBroker.registerDataChangeListener(OPERATIONAL,
                EVENT_SOURCE_TOPOLOGY_PATH,
                listener,
                DataBroker.DataChangeScope.SUBTREE);

        registrations.put(listener, listenerRegistration);
    }

    public void register(final Node node, final NetconfEventSource netconfEventSource) {
        final KeyedInstanceIdentifier<Node, NodeKey> sourcePath = EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.getKey());
        rpcRegistry.addRoutedRpcImplementation(EventSourceService.class, netconfEventSource)
            .registerPath(NodeContext.class, sourcePath);
        insert(sourcePath,node);
        // FIXME: Return registration object.
    }

}
