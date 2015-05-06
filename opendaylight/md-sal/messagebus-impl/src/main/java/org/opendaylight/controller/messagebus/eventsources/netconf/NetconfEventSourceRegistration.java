/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Helper class to keep connection status of netconf node  and event source registration object
 *
 */
public class NetconfEventSourceRegistration implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEventSourceRegistration.class);
    private static final YangInstanceIdentifier NETCONF_DEVICE_DOM_PATH = YangInstanceIdentifier.builder()
            .node(NetworkTopology.QNAME)
            .node(Topology.QNAME)
            .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"),TopologyNetconf.QNAME.getLocalName())
            .node(Node.QNAME)
            .build();
    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME,"node-id");
    private static final String NotificationCapabilityPrefix = "(urn:ietf:params:xml:ns:netconf:notification";

    private final Node node;
    private final InstanceIdentifier<?> instanceIdent;
    private final NetconfEventSourceManager netconfEventSourceManager;
    private ConnectionStatus currentNetconfConnStatus;
    private EventSourceRegistration<NetconfEventSource> eventSourceRegistration;

    public static NetconfEventSourceRegistration create(final InstanceIdentifier<?> instanceIdent, final Node node,
                final NetconfEventSourceManager netconfEventSourceManager){
        Preconditions.checkNotNull(instanceIdent);
        Preconditions.checkNotNull(node);
        Preconditions.checkNotNull(netconfEventSourceManager);
        if(isEventSource(node) == false){
            return null;
        }
        NetconfEventSourceRegistration nesr = new NetconfEventSourceRegistration(instanceIdent, node, netconfEventSourceManager);
        nesr.updateStatus();
        LOG.info("NetconfEventSourceRegistration for node {} has been initialized...",node.getNodeId().getValue());
        return nesr;
    }

    private static boolean isEventSource(final Node node) {
        final NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        if(netconfNode == null){
            return false;
        }
        if (netconfNode.getAvailableCapabilities() == null) {
            return false;
        }
        final List<String> capabilities = netconfNode.getAvailableCapabilities().getAvailableCapability();
        if(capabilities == null || capabilities.isEmpty()) {
             return false;
        }
        for (final String capability : netconfNode.getAvailableCapabilities().getAvailableCapability()) {
            if(capability.startsWith(NotificationCapabilityPrefix)) {
                return true;
            }
        }

        return false;
    }

    private NetconfEventSourceRegistration(final InstanceIdentifier<?> instanceIdent, final Node node, final NetconfEventSourceManager netconfEventSourceManager) {
        this.instanceIdent = instanceIdent;
        this.node = node;
        this.netconfEventSourceManager = netconfEventSourceManager;
        this.eventSourceRegistration =null;
    }

    public Node getNode() {
        return node;
    }

    Optional<EventSourceRegistration<NetconfEventSource>> getEventSourceRegistration() {
        return Optional.fromNullable(eventSourceRegistration);
    }

    NetconfNode getNetconfNode(){
        return node.getAugmentation(NetconfNode.class);
    }

    void updateStatus(){
        ConnectionStatus netconfConnStatus = getNetconfNode().getConnectionStatus();
        LOG.info("Change status on node {}, new status is {}",this.node.getNodeId().getValue(),netconfConnStatus);
        if(netconfConnStatus.equals(currentNetconfConnStatus)){
            return;
        }
        changeStatus(netconfConnStatus);
    }

    private boolean checkConnectionStatusType(ConnectionStatus status){
        if(    status == ConnectionStatus.Connected
            || status == ConnectionStatus.Connecting
            || status == ConnectionStatus.UnableToConnect){
            return true;
        }
        return false;
    }

    private void changeStatus(ConnectionStatus newStatus){
        Preconditions.checkNotNull(newStatus);
        if(checkConnectionStatusType(newStatus) == false){
            throw new IllegalStateException("Unknown new Netconf Connection Status");
        }
        if(this.currentNetconfConnStatus == null){
            if (newStatus == ConnectionStatus.Connected){
                registrationEventSource();
            }
        } else if (this.currentNetconfConnStatus == ConnectionStatus.Connecting){
            if (newStatus == ConnectionStatus.Connected){
                if(this.eventSourceRegistration == null){
                    registrationEventSource();
                } else {
                    // reactivate stream on registered event source (invoke publish notification about connection)
                    this.eventSourceRegistration.getInstance().reActivateStreams();
                }
            }
        } else if (this.currentNetconfConnStatus == ConnectionStatus.Connected) {

            if(newStatus == ConnectionStatus.Connecting || newStatus == ConnectionStatus.UnableToConnect){
                // deactivate streams on registered event source (invoke publish notification about connection)
                this.eventSourceRegistration.getInstance().deActivateStreams();
            }
        } else if (this.currentNetconfConnStatus == ConnectionStatus.UnableToConnect){
            if(newStatus == ConnectionStatus.Connected){
                if(this.eventSourceRegistration == null){
                    registrationEventSource();
                } else {
                    // reactivate stream on registered event source (invoke publish notification about connection)
                    this.eventSourceRegistration.getInstance().reActivateStreams();
                }
            }
        } else {
            throw new IllegalStateException("Unknown current Netconf Connection Status");
        }
        this.currentNetconfConnStatus = newStatus;
    }

    private void registrationEventSource(){
        final Optional<MountPoint> mountPoint = netconfEventSourceManager.getMountPointService().getMountPoint(instanceIdent);
        final Optional<DOMMountPoint> domMountPoint = netconfEventSourceManager.getDomMounts().getMountPoint(domMountPath(node.getNodeId()));
        EventSourceRegistration<NetconfEventSource> registration = null;
        if(domMountPoint.isPresent() && mountPoint.isPresent()) {
            final NetconfEventSource netconfEventSource = new NetconfEventSource(
                    node,
                    netconfEventSourceManager.getStreamMap(),
                    domMountPoint.get(),
                    mountPoint.get(),
                    netconfEventSourceManager.getPublishService());
            registration = netconfEventSourceManager.getEventSourceRegistry().registerEventSource(netconfEventSource);
            LOG.info("Event source {} has been registered",node.getNodeId().getValue());
        }
        this.eventSourceRegistration = registration;
      }

    private YangInstanceIdentifier domMountPath(final NodeId nodeId) {
        return YangInstanceIdentifier.builder(NETCONF_DEVICE_DOM_PATH).nodeWithKey(Node.QNAME, NODE_ID_QNAME, nodeId.getValue()).build();
    }

    private void closeEventSourceRegistration(){
        if(getEventSourceRegistration().isPresent()){
            getEventSourceRegistration().get().close();
        }
    }

    @Override
    public void close() {
        closeEventSourceRegistration();
    }

}
