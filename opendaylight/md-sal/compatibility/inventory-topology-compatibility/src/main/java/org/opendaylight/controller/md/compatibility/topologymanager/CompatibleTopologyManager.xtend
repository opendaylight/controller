package org.opendaylight.controller.md.compatibility.topologymanager

import org.opendaylight.controller.topologymanager.ITopologyManager
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.Host
import org.opendaylight.controller.sal.core.UpdateType
import java.util.Set
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import java.util.HashMap
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import com.google.common.collect.FluentIterable

class CompatibleTopologyManager extends ConfigurableLinkManager implements ITopologyManager {

    @Property
    var TypeSafeDataReader dataReader;

    @Property
    var extension AdSalTopologyMapping topologyMapping;

    override getEdges() {
        val topology = dataReader.readOperationalData(topologyPath);
        return topology.link.toEdgePropertiesMap();
    }

    override getNodeEdges() {
        val topology = dataReader.readOperationalData(topologyPath);
        val ret = new HashMap<org.opendaylight.controller.sal.core.Node, Set<Edge>>;
        for (node : topology.node) {
            val adNode = node.toAdNode();
            val adEdges = FluentIterable.from(topology.link).filter[
                source.sourceNode == node.nodeId || destination.destNode == node.nodeId].toEdges();
            ret.put(adNode, adEdges)
        }
        return ret;
    }

    /**
     *   Returns true if point is connected to link
    */
    def isInternal(TerminationPoint point) {
        val topology = dataReader.readConfigurationData(topologyPath);
        val tpId = point.key.tpId;
        return FluentIterable.from(topology.link).anyMatch(
            [
                source.sourceTp == tpId || destination.destTp == tpId
            ])
    }

    override getNodeConnectorWithHost() {
    }

    override getHostAttachedToNodeConnector(NodeConnector p) {
        val tpPath = p.toTerminationPoint();
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getHostsAttachedToNodeConnector(NodeConnector p) {
        val topology = dataReader.readOperationalData(topologyPath);

        throw new UnsupportedOperationException("Hosts not mapped yet")
    }

    override getNodesWithNodeConnectorHost() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override isInternal(NodeConnector p) {
        val tpPath = p.toTerminationPoint();
        val connector = dataReader.readConfigurationData(tpPath);
        return connector.isInternal();
    }

    override updateHostLink(NodeConnector p, Host h, UpdateType t,
        Set<org.opendaylight.controller.sal.core.Property> props) {
        // Update app defined topology
    }

    override saveConfig() {
        // FIXME: commit configuration
    }

}
