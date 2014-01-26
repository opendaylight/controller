/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology

import com.google.common.collect.FluentIterable
import java.util.Collections
import java.util.List
import java.util.concurrent.CopyOnWriteArrayList
import org.opendaylight.controller.sal.core.ConstructionException
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.UpdateType
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link

import static com.google.common.base.Preconditions.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.slf4j.LoggerFactory

class TopologyMapping {
    private static val LOG = LoggerFactory.getLogger(TopologyMapping);
    private new() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }
    
    public static def toADEdgeUpdates(Topology topology,TypeSafeDataReader reader) {
        val List<TopoEdgeUpdate> result = new CopyOnWriteArrayList<TopoEdgeUpdate>()
        return FluentIterable.from(topology.link).transform[toAdEdge(topology).toTopoEdgeUpdate(reader)].copyInto(result)
    }
    
    public static def toAdEdge(Link link,Topology topology) {
        val adSrc = link.source.sourceTp.toADNodeConnector(link.source.sourceNode)
        val adDst = link.destination.destTp.toADNodeConnector(link.destination.destNode)
        return new Edge(adSrc,adDst); 
    }
    
    public static def toTopoEdgeUpdate(Edge e,TypeSafeDataReader reader) {
        return toTopoEdgeUpdate(e,UpdateType.ADDED,reader)
    }
    
    public static def toTopoEdgeUpdate(Edge e,UpdateType type,TypeSafeDataReader reader) {
        return new TopoEdgeUpdate(e,e.toAdEdgeProperties(reader),type)
    }
    
    public static def toAdEdgeProperties(Edge e,TypeSafeDataReader reader) {
        val ncref = e.tailNodeConnector.toNodeConnectorRef
        if(ncref == null) {
            LOG.debug("Edge {} ncref {}",e,ncref)
            return null;
        }
        val ncInstanceId = (ncref.value as InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>)
        if(ncInstanceId == null) {
            LOG.debug("Edge {} ncref {}",e,ncref)
            return null;
        }
        val nc = reader.readOperationalData(ncInstanceId)
        if(nc == null) {
            return null;
        }
        return nc.toADNodeConnectorProperties     
    }
    
    public static def toADNodeId(NodeId nodeId) {
        checkNotNull(nodeId);
        return nodeId.value
    }
    public static def toADNodeConnector(TpId source,NodeId nodeId) throws ConstructionException {
        checkNotNull(source);
        return new NodeConnector(MD_SAL_TYPE,source.toADNodeConnectorId,nodeId.toADNode)
    }
    
    public static def toADNodeConnectorId(TpId nodeConnectorId) {
        return nodeConnectorId.value
    }
    
    public static def toADNode(NodeId nodeId) {
        checkNotNull(nodeId);
        return new Node(MD_SAL_TYPE,nodeId.toADNodeId);       
    }
}
