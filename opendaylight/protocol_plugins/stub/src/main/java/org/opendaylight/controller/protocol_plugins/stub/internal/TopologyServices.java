package org.opendaylight.controller.protocol_plugins.stub.internal;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TopologyServices implements IPluginInTopologyService {
    private IPluginOutTopologyService pluginOutTopologyService;


    @Override
    public void sollicitRefresh() {
    }

    public void setPluginOutTopologyService(IPluginOutTopologyService pluginOutTopologyService){
        this.pluginOutTopologyService = pluginOutTopologyService;
    }

    public void unsetPluginOutTopologyService(IPluginOutTopologyService pluginOutTopologyService){
        this.pluginOutTopologyService = null;
    }


    public void addEdge(Edge edge, Set<Property> properties, UpdateType updateType){

        List<TopoEdgeUpdate > topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();

        topoedgeupdateList.add(new TopoEdgeUpdate(edge, properties, UpdateType.ADDED));

        this.pluginOutTopologyService.edgeUpdate(topoedgeupdateList);

    }
}
