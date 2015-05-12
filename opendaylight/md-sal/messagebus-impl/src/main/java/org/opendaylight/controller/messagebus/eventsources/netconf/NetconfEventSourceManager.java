/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.eventsources.netconf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class NetconfEventSourceManager implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEventSourceManager.class);
    private static final TopologyKey NETCONF_TOPOLOGY_KEY = new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()));
    private static final InstanceIdentifier<Node> NETCONF_DEVICE_PATH = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, NETCONF_TOPOLOGY_KEY)
                .child(Node.class);

    private final Map<String, String> streamMap;
    private final ConcurrentHashMap<InstanceIdentifier<?>, NetconfEventSourceRegistration> registrationMap = new ConcurrentHashMap<>();
    private final DOMNotificationPublishService publishService;
    private final DOMMountPointService domMounts;
    private final MountPointService mountPointService;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final EventSourceRegistry eventSourceRegistry;

    public static NetconfEventSourceManager create(final DataBroker dataBroker,
            final DOMNotificationPublishService domPublish,
            final DOMMountPointService domMount,
            final MountPointService bindingMount,
            final EventSourceRegistry eventSourceRegistry,
            final List<NamespaceToStream> namespaceMapping){

        final NetconfEventSourceManager eventSourceManager =
                new NetconfEventSourceManager(domPublish, domMount,bindingMount, eventSourceRegistry, namespaceMapping);

        eventSourceManager.initialize(dataBroker);

        return eventSourceManager;

    }

    private NetconfEventSourceManager(final DOMNotificationPublishService domPublish,
                              final DOMMountPointService domMount,
                              final MountPointService bindingMount,
                              final EventSourceRegistry eventSourceRegistry,
                              final List<NamespaceToStream> namespaceMapping) {

        Preconditions.checkNotNull(domPublish);
        Preconditions.checkNotNull(domMount);
        Preconditions.checkNotNull(bindingMount);
        Preconditions.checkNotNull(eventSourceRegistry);
        Preconditions.checkNotNull(namespaceMapping);
        this.streamMap = namespaceToStreamMapping(namespaceMapping);
        this.domMounts = domMount;
        this.mountPointService = bindingMount;
        this.publishService = domPublish;
        this.eventSourceRegistry = eventSourceRegistry;
    }

    private void initialize(final DataBroker dataBroker){
        Preconditions.checkNotNull(dataBroker);
        listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, NETCONF_DEVICE_PATH, this, DataChangeScope.SUBTREE);
        LOG.info("NetconfEventSourceManager initialized.");
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

        LOG.debug("[DataChangeEvent<InstanceIdentifier<?>, DataObject>: {}]", event);
        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getCreatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                nodeCreated(changeEntry.getKey(),(Node) changeEntry.getValue());
            }
        }

        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                nodeUpdated(changeEntry.getKey(),(Node) changeEntry.getValue());
            }
        }

        for(InstanceIdentifier<?> removePath : event.getRemovedPaths()){
            DataObject removeObject = event.getOriginalData().get(removePath);
            if(removeObject instanceof Node){
                nodeRemoved(removePath);
            }
        }

    }

    private void nodeCreated(final InstanceIdentifier<?> key, final Node node){
        Preconditions.checkNotNull(key);
        if(validateNode(node) == false){
            LOG.warn("NodeCreated event : Node [{}] is null or not valid.", key.toString());
            return;
        }
        LOG.info("Netconf event source [{}] is creating...", key.toString());
        NetconfEventSourceRegistration nesr = NetconfEventSourceRegistration.create(key, node, this);
        if(nesr != null){
            NetconfEventSourceRegistration nesrOld = registrationMap.put(key, nesr);
            if(nesrOld != null){
                nesrOld.close();
            }
        }
    }

    private void nodeUpdated(final InstanceIdentifier<?> key, final Node node){
        Preconditions.checkNotNull(key);
        if(validateNode(node) == false){
            LOG.warn("NodeUpdated event : Node [{}] is null or not valid.", key.toString());
            return;
        }

        LOG.info("Netconf event source [{}] is updating...", key.toString());
        NetconfEventSourceRegistration nesr = registrationMap.get(key);
        if(nesr != null){
            nesr.updateStatus();
        } else {
            nodeCreated(key, node);
        }
    }

    private void nodeRemoved(final InstanceIdentifier<?> key){
        Preconditions.checkNotNull(key);
        LOG.info("Netconf event source [{}] is removing...", key.toString());
        NetconfEventSourceRegistration nesr = registrationMap.remove(key);
        if(nesr != null){
            nesr.close();
        }
    }

    private boolean validateNode(final Node node){
        if(node == null){
            return false;
        }
        return isNetconfNode(node);
    }

    Map<String, String> getStreamMap() {
        return streamMap;
    }

    DOMNotificationPublishService getPublishService() {
        return publishService;
    }

    DOMMountPointService getDomMounts() {
        return domMounts;
    }

    EventSourceRegistry getEventSourceRegistry() {
        return eventSourceRegistry;
    }

    MountPointService getMountPointService() {
        return mountPointService;
    }

    private boolean isNetconfNode(final Node node)  {
        return node.getAugmentation(NetconfNode.class) != null ;
    }

    @Override
    public void close() {
        listenerRegistration.close();
        for(final NetconfEventSourceRegistration reg : registrationMap.values()){
            reg.close();
        }
        registrationMap.clear();
    }

}