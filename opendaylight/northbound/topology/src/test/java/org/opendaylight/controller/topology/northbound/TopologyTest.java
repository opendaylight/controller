package org.opendaylight.controller.topology.northbound;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class TopologyTest {

    @Test
    public void edgePropertiesTopologyTest() {

        //Create 3 nodes and edges between them
        Node n1 = NodeCreator.createOFNode((long)1);
        Node n2 = NodeCreator.createOFNode((long)2);
        Node n3 = NodeCreator.createOFNode((long)3);

        NodeConnector nc11 = NodeConnectorCreator.createOFNodeConnector((short) 1, n1);
        NodeConnector nc12 = NodeConnectorCreator.createOFNodeConnector((short) 2, n1);
        NodeConnector nc21 = NodeConnectorCreator.createOFNodeConnector((short) 1, n2);
        NodeConnector nc22 = NodeConnectorCreator.createOFNodeConnector((short) 2, n2);
        NodeConnector nc23 = NodeConnectorCreator.createOFNodeConnector((short) 2, n3);
        NodeConnector nc32 = NodeConnectorCreator.createOFNodeConnector((short) 3, n2);
        NodeConnector nc33 = NodeConnectorCreator.createOFNodeConnector((short) 3, n3);

        Edge e12 = null;
        Edge e23 = null;

        try {
            e12 = new Edge(nc12, nc21);
        } catch (ConstructionException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        try {
            e23 = new Edge(nc23, nc32);
        } catch (ConstructionException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        Set<Property> props = new HashSet<Property>();
        State state = new State(State.EDGE_UP);
        Bandwidth bw = new Bandwidth(Bandwidth.BW100Gbps);
        Latency l = new Latency(Latency.LATENCY100ns);
        props.add(state);
        props.add(bw);
        props.add(l);

        //Check get methods for edge and properties
        EdgeProperties edgeProp = new EdgeProperties(e12, props);

        Edge getEdge = edgeProp.getEdge();
        assertEquals(e12, getEdge);
        assertEquals(nc12, getEdge.getTailNodeConnector());
        assertEquals(n1, getEdge.getTailNodeConnector().getNode());
        assertEquals((long)1, getEdge.getTailNodeConnector().getNode().getID());
        assertEquals(nc21, getEdge.getHeadNodeConnector());
        assertEquals(n2, getEdge.getHeadNodeConnector().getNode());
        assertEquals((long)2, getEdge.getHeadNodeConnector().getNode().getID());

        Set<Property> getProp = edgeProp.getProperties();
        assertEquals(props, getProp);
        assertEquals(props.size(), getProp.size());

        //Use set methods
        edgeProp.setEdge(e23);
        getEdge = edgeProp.getEdge();
        assertEquals(e23, getEdge);
        assertEquals(nc23, getEdge.getTailNodeConnector());
        assertEquals(nc32, getEdge.getHeadNodeConnector());

        props.remove(state);
        edgeProp.setProperties(props);
        assertEquals(props, getProp);
        assertEquals(props.size(), getProp.size());


        //Create and check topology
        List<EdgeProperties> edgePropList= new ArrayList<EdgeProperties>();
        edgePropList.add(edgeProp);

        Topology t = new Topology(edgePropList);

        List<EdgeProperties> getEdgePropList = t.getEdgeProperties();
        assertEquals(edgePropList, getEdgePropList);
        assertEquals(1, getEdgePropList.size());

        EdgeProperties edgeProp2 = new EdgeProperties(e23, props);
        edgePropList.add(edgeProp2);
        t.setEdgeProperties(edgePropList);

        getEdgePropList = t.getEdgeProperties();
        assertEquals(edgePropList, getEdgePropList);
        assertEquals(2, getEdgePropList.size());

    }
	

}
