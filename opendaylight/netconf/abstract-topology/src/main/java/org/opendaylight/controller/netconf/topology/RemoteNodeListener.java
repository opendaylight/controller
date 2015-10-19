/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.annotations.Beta;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import scala.concurrent.Future;

/**
 * Interface that provides methods of calling node events on a remote actor.
 * Use these when you want to call node events asynchronously similar to akka ask()
 */
@Beta
public interface RemoteNodeListener {

    /**
     * This is called when a remote node is informing you that a new configuration was recieved.
     * @param nodeId
     * @param node
     * @return
     */
    Future<Node> remoteNodeCreated(NodeId nodeId, Node node);

    /**
     * This is called when a remote node is informing you that a configuration was updated.
     * @param nodeId
     * @param node
     * @return
     */
    Future<Node> remoteNodeUpdated(NodeId nodeId, Node node);

    /**
     * This is called when a remote node is informing you that a new configuration was deleted.
     * @param nodeId
     * @return
     */
    Future<Void> remoteNodeDeleted(NodeId nodeId);

    /**
     * Called when a remote node is requesting a node's status, after a status change notification(f.ex sessionUp, sessionDown)
     * on lower level
     * @param nodeId
     * @return
     */
    Future<Node> remoteGetCurrentStatusForNode(NodeId nodeId);
}

