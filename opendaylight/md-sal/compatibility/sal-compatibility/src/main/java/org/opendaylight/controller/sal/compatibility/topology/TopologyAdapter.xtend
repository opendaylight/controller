/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.topology.IPluginInTopologyService
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

import static extension org.opendaylight.controller.sal.compatibility.topology.TopologyMapping.*

class TopologyAdapter implements IPluginInTopologyService {
    
    @Property
    DataProviderService dataService;
    
    @Property
    IPluginOutTopologyService topologyPublisher;
    
    override sollicitRefresh() {
        val path = InstanceIdentifier.builder(NetworkTopology).child(Topology,new TopologyKey(new TopologyId("flow:1"))).toInstance;
        val reader = TypeSafeDataReader.forReader(dataService)
        val topology = reader.readOperationalData(path)
        topologyPublisher.edgeUpdate(topology.toADEdgeUpdates(reader))
    }
    
}
