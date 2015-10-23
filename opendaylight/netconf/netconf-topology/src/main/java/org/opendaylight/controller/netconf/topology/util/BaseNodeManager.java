/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.netconf.topology.NodeManager;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback.NodeManagerCallbackFactory;
import org.opendaylight.controller.netconf.topology.RoleChangeStrategy;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

public class BaseNodeManager implements NodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(BaseNodeManager.class);

    private final String nodeId;
    private final String topologyId;
    private final ActorSystem actorSystem;

    private boolean isMaster;
    private final NodeManagerCallback delegate;
    private final List<String> remotePaths;

    private BaseNodeManager(final String nodeId, final String topologyId, final ActorSystem actorSystem,
            final NodeManagerCallbackFactory<?> delegateFactory, final RoleChangeStrategy roleChangeStrategy,
            final List<String> remotePaths) {
        LOG.debug("Creating BaseNodeManager, id: {}, {}", topologyId, nodeId);
        this.nodeId = nodeId;
        this.topologyId = topologyId;
        this.actorSystem = actorSystem;
        this.delegate = delegateFactory.create(nodeId, topologyId, actorSystem);
        this.remotePaths = remotePaths;
        // if we want to override the place election happens,
        // we need to override this with noop election strategy and implement election in callback
        // cannot leak this here! have to use TypedActor.self()
        roleChangeStrategy.registerRoleCandidate((NodeManager) TypedActor.self());
    }

    @Override
    public Node getInitialState(@Nonnull final NodeId nodeId, final Node configNode) {
        LOG.trace("Retrieving Node {} initial state", nodeId);
        return delegate.getInitialState(nodeId, configNode);
    }

    @Override
    public Node getFailedState( final NodeId nodeId,  final Node configNode) {
        LOG.trace("Retrieving Node {} failed state", nodeId);
        return delegate.getFailedState(nodeId, configNode);
    }

    @Override
    public ListenableFuture<Node> onNodeCreated(final NodeId nodeId, final Node configNode) {
        LOG.debug("Creating Node {}, with configuration: {}", nodeId.getValue(), configNode);
        return delegate.onNodeCreated(nodeId, configNode);
    }

    @Override
    public ListenableFuture<Node> onNodeUpdated(final NodeId nodeId, final Node configNode) {
        LOG.debug("Updating Node {}, with configuration: {}", nodeId.getValue(), configNode);
        return delegate.onNodeUpdated(nodeId, configNode);
    }

    @Override
    public ListenableFuture<Void> onNodeDeleted(final NodeId nodeId) {
        LOG.debug("Deleting Node {}", nodeId.getValue());
        return delegate.onNodeDeleted(nodeId);
    }

    @Override
    public ListenableFuture<Node> getCurrentStatusForNode( final NodeId nodeId) {
        return delegate.getCurrentStatusForNode(nodeId);
    }

    @Override
    public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {
        LOG.debug("Node {} role has changed from: {} to {}", nodeId, (roleChangeDTO.wasOwner() ? "master" : "slave"),
                (roleChangeDTO.isOwner() ? "master" : "slave"));

        isMaster = roleChangeDTO.isOwner();
        delegate.onRoleChanged(roleChangeDTO);
    }

    @Override
    public Future<Node> remoteNodeCreated(final NodeId nodeId, final Node node) {
        return null;
    }

    @Override
    public Future<Node> remoteNodeUpdated(final NodeId nodeId, final Node node) {
        return null;
    }

    @Override
    public Future<Void> remoteNodeDeleted(final NodeId nodeId) {
        return null;
    }

    @Override
    public Future<Node> remoteGetCurrentStatusForNode(final NodeId nodeId) {
        return null;
    }

    @Override
    public void onReceive(final Object o, final ActorRef actorRef) {

    }

    /**
     * Builder of BaseNodeManager instances that are proxied as TypedActors
     *
     * @param
     */
    public static class BaseNodeManagerBuilder {
        private String nodeId;
        private String topologyId;
        private NodeManagerCallbackFactory delegateFactory;
        private RoleChangeStrategy roleChangeStrategy;
        private ActorContext actorContext;
        private List<String> remotePaths;

        public BaseNodeManagerBuilder setNodeId(final String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public BaseNodeManagerBuilder setTopologyId(final String topologyId) {
            this.topologyId = topologyId;
            return this;
        }

        public BaseNodeManagerBuilder setDelegateFactory(final NodeManagerCallback.NodeManagerCallbackFactory delegateFactory) {
            this.delegateFactory = delegateFactory;
            return this;
        }

        public BaseNodeManagerBuilder setRoleChangeStrategy(final RoleChangeStrategy roleChangeStrategy) {
            this.roleChangeStrategy = roleChangeStrategy;
            return this;
        }

        public BaseNodeManagerBuilder setActorContext(final ActorContext actorContext) {
            this.actorContext = actorContext;
            return this;
        }

        public BaseNodeManagerBuilder setRemotePaths(final List<String> remotePaths) {
            this.remotePaths = remotePaths;
            return this;
        }

        public NodeManager build() {
            Preconditions.checkNotNull(nodeId);
            Preconditions.checkNotNull(topologyId);
            Preconditions.checkNotNull(delegateFactory);
            Preconditions.checkNotNull(roleChangeStrategy);
            Preconditions.checkNotNull(actorContext);
            Preconditions.checkNotNull(remotePaths);
            LOG.debug("Creating typed actor with id: {}", nodeId);

            return TypedActor.get(actorContext).typedActorOf(
                    new TypedProps<>(NodeManager.class, new Creator<BaseNodeManager>() {
                        @Override
                        public BaseNodeManager create() throws Exception {
                            return new BaseNodeManager(nodeId, topologyId, actorContext.system(), delegateFactory,
                                    roleChangeStrategy, remotePaths);
                        }
                    }), nodeId);
        }
    }
}
