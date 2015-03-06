/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfEventSourceManager implements DataChangeListener, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetconfEventSourceManager.class);
    private static final TopologyKey NETCONF_TOPOLOGY_KEY = new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()));
    private static final InstanceIdentifier<Node> NETCONF_DEVICE_PATH = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, NETCONF_TOPOLOGY_KEY)
                .child(Node.class);

    private static final YangInstanceIdentifier NETCONF_DEVICE_DOM_PATH = YangInstanceIdentifier.builder()
            .node(NetworkTopology.QNAME)
            .node(Topology.QNAME)
            .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"),TopologyNetconf.QNAME.getLocalName())
            .node(Node.QNAME)
            .build();
    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME,"node-id");


    private final EventSourceTopology eventSourceTopology;
    private final Map<String, String> streamMap;

    private final ConcurrentHashMap<InstanceIdentifier<?>, NetconfEventSource> netconfSources = new ConcurrentHashMap<>();
    private final ListenerRegistration<DataChangeListener> listenerReg;
    private final DOMNotificationPublishService publishService;
    private final DOMMountPointService domMounts;
    private final MountPointService bindingMounts;

    public NetconfEventSourceManager(final DataBroker dataStore,
                              final DOMNotificationPublishService domPublish,
                              final DOMMountPointService domMount,
                              final MountPointService bindingMount,
                              final EventSourceTopology eventSourceTopology,
                              final List<NamespaceToStream> namespaceMapping) {

        listenerReg = dataStore.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, NETCONF_DEVICE_PATH, this, DataChangeScope.SUBTREE);
        this.eventSourceTopology = eventSourceTopology;
        this.streamMap = namespaceToStreamMapping(namespaceMapping);
        this.domMounts = domMount;
        this.bindingMounts = bindingMount;
        this.publishService = domPublish;
        LOGGER.info("EventSourceManager initialized.");
    }

    private Map<String,String> namespaceToStreamMapping(final List<NamespaceToStream> namespaceMapping) {
        final Map<String, String> streamMap = new HashMap<>(namespaceMapping.size());

        for (final NamespaceToStream nToS  : namespaceMapping) {
            streamMap.put(nToS.getUrnPrefix(), nToS.getStreamName());
        }

        return streamMap;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        //FIXME: Prevent creating new event source on subsequent changes in inventory, like disconnect.
        LOGGER.debug("[DataChangeEvent<InstanceIdentifier<?>, DataObject>: {}]", event);
        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getCreatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                nodeUpdated(changeEntry.getKey(),(Node) changeEntry.getValue());
            }
        }


        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                nodeUpdated(changeEntry.getKey(),(Node) changeEntry.getValue());
            }
        }


    }

    private void nodeUpdated(final InstanceIdentifier<?> key, final Node node) {

        // we listen on node tree, therefore we should rather throw IllegalStateException when node is null
        if ( node == null ) {
            LOGGER.debug("OnDataChanged Event. Node is null.");
            return;
        }
        if ( isNetconfNode(node) == false ) {
            LOGGER.debug("OnDataChanged Event. Not a Netconf node.");
            return;
        }
        if ( isEventSource(node) == false ) {
            LOGGER.debug("OnDataChanged Event. Node an EventSource node.");
            return;
        }
        if(node.getAugmentation(NetconfNode.class).getConnectionStatus() != ConnectionStatus.Connected ) {
            return;
        }

        if(!netconfSources.containsKey(key)) {
            createEventSource(key,node);
        }
    }

    private void createEventSource(final InstanceIdentifier<?> key, final Node node) {
        final Optional<DOMMountPoint> netconfMount = domMounts.getMountPoint(domMountPath(node.getNodeId()));
        final Optional<MountPoint> bindingMount = bindingMounts.getMountPoint(key);

        if(netconfMount.isPresent() && bindingMount.isPresent()) {
            final String nodeId = node.getNodeId().getValue();
            final NetconfEventSource netconfEventSource = new NetconfEventSource(nodeId, streamMap, netconfMount.get(), publishService, bindingMount.get());
            eventSourceTopology.register(node,netconfEventSource);
            netconfSources.putIfAbsent(key, netconfEventSource);
        }
    }

    private YangInstanceIdentifier domMountPath(final NodeId nodeId) {
        return YangInstanceIdentifier.builder(NETCONF_DEVICE_DOM_PATH).nodeWithKey(Node.QNAME, NODE_ID_QNAME, nodeId.getValue()).build();
    }

    private boolean isNetconfNode(final Node node)  {
        return node.getAugmentation(NetconfNode.class) != null ;
    }

    public boolean isEventSource(final Node node) {
        final NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);

        return isEventSource(netconfNode);
    }

    private boolean isEventSource(final NetconfNode node) {
        for (final String capability : node.getAvailableCapabilities().getAvailableCapability()) {
            if(capability.startsWith("(urn:ietf:params:xml:ns:netconf:notification")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() {
        listenerReg.close();
    }
}