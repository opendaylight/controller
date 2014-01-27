/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology

import com.google.common.collect.FluentIterable
import java.util.concurrent.CopyOnWriteArrayList
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.core.UpdateType
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import static extension org.opendaylight.controller.sal.compatibility.topology.TopologyMapping.*
import org.slf4j.LoggerFactory

class TopologyCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
    static val LOG = LoggerFactory.getLogger(TopologyCommitHandler);
    @Property
    IPluginOutTopologyService topologyPublisher;
    
    @Property
    DataProviderService dataService;
    
    new(DataProviderService dataService) {
        _topologyPublisher = topologyPublisher
        _dataService = dataService
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        val msg = new CopyOnWriteArrayList<TopoEdgeUpdate>()
        try {
            val reader = TypeSafeDataReader.forReader(dataService)
            val topologyPath = InstanceIdentifier.builder(NetworkTopology).child(Topology, new TopologyKey(new TopologyId("flow:1"))).toInstance
            val topology = reader.readOperationalData(topologyPath)
            val adds = FluentIterable.from(modification.createdOperationalData.entrySet)
                .filter[value instanceof Link]
                .transform[(value as Link).toAdEdge(topology).toTopoEdgeUpdate(UpdateType.ADDED,reader)]
                .toList
            val updates = FluentIterable.from(modification.updatedOperationalData.entrySet)
                .filter[!modification.createdOperationalData.containsKey(key) && (value instanceof Link)]
                .transform[(value as Link).toAdEdge(topology).toTopoEdgeUpdate(UpdateType.ADDED,reader)] // Evidently the ADSAL does not expect edge 'CHANGED"
                .toList
            val removes = FluentIterable.from(modification.removedOperationalData)
                .transform[reader.readOperationalData(it as InstanceIdentifier<DataObject>)]
                .filter[it instanceof Link]
                .transform[(it as Link).toAdEdge(topology).toTopoEdgeUpdate(UpdateType.REMOVED,reader)]
                .toList
            msg.addAll(adds)
            msg.addAll(updates)
            msg.addAll(removes)
         } catch (Exception e) {
            LOG.error("Exception caught",e)
         }
        return new TopologyTransaction(modification,topologyPublisher,dataService,msg)
    }
}
