/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.impl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.netconf.topology.NetconfTopology;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback;
import org.opendaylight.controller.netconf.topology.RoleChangeStrategy;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link NodeManagerCallback}
 */
public class NodeManagerCallbackImpl implements NodeManagerCallback, RemoteDeviceHandler<NetconfSessionPreferences> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeManagerCallbackImpl.class);

    private String nodeId;
    private boolean isMaster = false;

    private final String topologyId;
    final NetconfTopology topologyDispatcher;
    private final ActorSystem actorSystem;
    private final RoleChangeStrategy roleChangeStrategy;

    public NodeManagerCallbackImpl(final String nodeId, final String topologyId, final ActorSystem actorSystem,
            final NetconfTopology topologyDispatcher, final RoleChangeStrategy roleChangeStrategy) {
        this.nodeId = nodeId;
        this.topologyId = topologyId;
        this.topologyDispatcher = topologyDispatcher;
        this.actorSystem = Preconditions.checkNotNull(actorSystem);
        this.roleChangeStrategy = Preconditions.checkNotNull(roleChangeStrategy);
    }

    @Override
    public Node getInitialState(final NodeId nodeId, final Node configNode) {
        nodeProducerInputValidation(nodeId, configNode);
        return new NodeBuilder(configNode).setNodeId(nodeId).addAugmentation(NetconfNode.class,
                new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.Connecting).build()).build();
    }

    @Override
    public Node getFailedState(final NodeId nodeId, final Node configNode) {
        nodeProducerInputValidation(nodeId, configNode);
        return new NodeBuilder(configNode).setNodeId(nodeId).addAugmentation(NetconfNode.class,
                new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.UnableToConnect).build()).build();
    }

    @Override
    public ListenableFuture<Node> onNodeCreated(final NodeId nodeId, final Node configNode) {
        this.nodeId = Preconditions.checkNotNull(nodeId).getValue();

        final ListenableFuture<NetconfDeviceCapabilities> connectionFuture =
                topologyDispatcher.connectNode(nodeId, configNode);

        Futures.addCallback(connectionFuture, new FutureCallback<NetconfDeviceCapabilities>() {
            @Override
            public void onSuccess(final NetconfDeviceCapabilities result) {
//              roleChangeStrategy.registerRoleCandidate(parentNodeManager);
                topologyDispatcher.registerConnectionStatusListener(nodeId, NodeManagerCallbackImpl.this);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Connection to device failed", t);
            }
        });

        final NetconfNode netconfNode = configNode.getAugmentation(NetconfNode.class);

        return Futures.transform(connectionFuture, new Function<NetconfDeviceCapabilities, Node>() {
            @Nullable
            @Override
            public Node apply(final NetconfDeviceCapabilities input) {
                // build state data
                return new NodeBuilder().addAugmentation(NetconfNode.class,
                        new NetconfNodeBuilder()
                            .setConnectionStatus(ConnectionStatus.Connected)
                            .setHost(netconfNode.getHost())
                            .setPort(netconfNode.getPort())
                            .build()).build();
            }
        });
    }

    @Override
    public ListenableFuture<Node> onNodeUpdated(final NodeId nodeId, final Node configNode) {
        // first disconnect this node
        topologyDispatcher.unregisterMountPoint(nodeId);
        topologyDispatcher.disconnectNode(nodeId);

        // now reinit this connection with new settings
        // TODO add a listener into the device communicatator that will notify for connection changes
        final ListenableFuture<NetconfDeviceCapabilities> connectionFuture = topologyDispatcher.connectNode(nodeId, configNode);

        Futures.addCallback(connectionFuture, new FutureCallback<NetconfDeviceCapabilities>() {
            @Override
            public void onSuccess(final NetconfDeviceCapabilities result) {
//                roleChangeStrategy.registerRoleCandidate(parentNodeManager);
                topologyDispatcher.registerConnectionStatusListener(nodeId, NodeManagerCallbackImpl.this);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Connection to device failed", t);
            }
        });

        return Futures.transform(connectionFuture, new Function<NetconfDeviceCapabilities, Node>() {
            @Nullable
            @Override
            public Node apply(final NetconfDeviceCapabilities input) {
                // build state data
                return new NodeBuilder().addAugmentation(NetconfNode.class,
                        new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.Connected).build()).build();
            }
        });
    }

    @Override
    public ListenableFuture<Void> onNodeDeleted(final NodeId nodeId) {
        // Disconnect
        topologyDispatcher.unregisterMountPoint(nodeId);
        return topologyDispatcher.disconnectNode(nodeId);
    }

    @Override
    public ListenableFuture<Node> getCurrentStatusForNode(final NodeId nodeId) {
        return null;
    }

    @Override
    public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {
        if (roleChangeDTO.isOwner() && roleChangeDTO.wasOwner()) {
            return;
        }
        isMaster = roleChangeDTO.isOwner();
        if (isMaster) {
            // unregister old mountPoint if ownership changed, register a new one
//            topologyDispatcher.registerMountPoint(nodeId);
        } else {
//            topologyDispatcher.unregisterMountPoint(nodeId);
        }
    }

    @Override
    public void onReceive(final Object arg0, final ActorRef arg1) {

    }

    @Override
    public void onDeviceConnected(final SchemaContext remoteSchemaContext,
            final NetconfSessionPreferences netconfSessionPreferences, final DOMRpcService deviceRpc) {
        // we need to notify the higher level that something happened, get a current status from all
        // other nodes, and aggregate a new result
//        roleChangeStrategy.registerRoleCandidate(parentNodeManager);
    }

    @Override
    public void onDeviceDisconnected() {
        // we need to notify the higher level that something happened, get a current status from all
        // other nodes, and aggregate a new result
        // no need to remove mountpoint, we should receive onRoleChanged callback after unregistering from
        // election that unregisters the mountpoint
        roleChangeStrategy.unregisterRoleCandidate();
    }

    @Override
    public void onDeviceFailed(final Throwable throwable) {
        // we need to notify the higher level that something happened, get a current status from all other
        // nodes, and aggregate a new result
        // no need to remove mountpoint, we should receive onRoleChanged callback after unregistering
        // from election that unregisters the mountpoint
        roleChangeStrategy.unregisterRoleCandidate();
    }

    @Override
    public void onNotification(final DOMNotification domNotification) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    private static void nodeProducerInputValidation(final NodeId nodeId, final Node configNode) {
        Preconditions.checkArgument(configNode != null, "Node can not be null!");
        Preconditions.checkArgument(nodeId != null, "NodeId can not be null!");
    }
}

