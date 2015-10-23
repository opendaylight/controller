/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

// TODO maybe rename to NetconfTopologyDispatcher?
public interface NetconfTopology {

    String getTopologyId();

    DataBroker getDataBroker();

    ListenableFuture<NetconfDeviceCapabilities> connectNode(NodeId nodeId, Node configNode);

    ListenableFuture<Void> disconnectNode(NodeId nodeId);

    void registerMountPoint(NodeId nodeId);

    void unregisterMountPoint(NodeId nodeId);

    void registerConnectionStatusListener(NodeId node, RemoteDeviceHandler<NetconfSessionPreferences> listener);
}