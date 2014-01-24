package org.opendaylight.controller.sal.compatibility.topology

import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link
import org.slf4j.LoggerFactory

class TopologyProvider implements AutoCloseable{
    static val LOG = LoggerFactory.getLogger(TopologyProvider);
    TopologyCommitHandler commitHandler

    @Property
    IPluginOutTopologyService topologyPublisher;

    @Property
    DataProviderService dataService;

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;

    def void start() {
        commitHandler = new TopologyCommitHandler(dataService)
        commitHandler.setTopologyPublisher(topologyPublisher)
        val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(NetworkTopology)
            .child(Topology,new TopologyKey(new TopologyId("flow:1")))
            .child(Link)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("TopologyProvider started")
    }

    override close() throws Exception {
        commitHandlerRegistration.close
    }

    def setTopologyPublisher(IPluginOutTopologyService topologyPublisher) {
        _topologyPublisher = topologyPublisher;
        commitHandler.setTopologyPublisher(topologyPublisher);
    }

}