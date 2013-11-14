package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentMappingService;
import org.opendaylight.controller.sal.binding.impl.connect.dom.MappingServiceImpl;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleContext;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class MappingServiceTest {

    private static final QName NODES = QName.create("urn:opendaylight:inventory", "2013-08-19", "nodes");
    private static final QName NODE = QName.create(NODES,"node");
    private static final QName ID = QName.create(NODES,"id");
    
    BindingIndependentMappingService service;
    private MappingServiceImpl impl;

    @Before
    public void setUp() {
        impl = new MappingServiceImpl();
        service = impl;
    }

    @Test
    public void baDataToBiData() throws Exception {

        String[] yangFiles = AbstractDataServiceTest.getModelFilenamesImpl();

        SchemaContext ctx = AbstractDataServiceTest.getContext(yangFiles);

        impl.onGlobalContextUpdated(ctx);

        NodesBuilder nodes = new NodesBuilder();

        List<Node> nodeList = new ArrayList<>();
        nodeList.add(createChildNode("foo"));
        nodeList.add(createChildNode("bar"));

        nodes.setNode(nodeList);
        Nodes nodesTO = nodes.build();
        CompositeNode xmlNodes = service.toDataDom(nodesTO);
        assertNotNull(xmlNodes);
        List<CompositeNode> invNodes = xmlNodes.getCompositesByName(NODE);
        assertNotNull(invNodes);
        assertEquals(2, invNodes.size());
    }

    @Test
    public void instanceIdentifierTest() throws Exception {

        String[] yangFiles = AbstractDataServiceTest.getModelFilenamesImpl();
        SchemaContext ctx = AbstractDataServiceTest.getContext(yangFiles);
        impl.onGlobalContextUpdated(ctx);

        NodeKey nodeKey = new NodeKey(new NodeId("foo"));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder().node(Nodes.class).child(Node.class, nodeKey).toInstance();
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier result = service.toDataDom(path);
        assertNotNull(result);
        assertEquals(2, result.getPath().size());
    }

    private Node createChildNode(String id) {
        NodeBuilder node = new NodeBuilder();
        NodeId nodeId = new NodeId(id);

        node.setId(nodeId);
        node.setKey(new NodeKey(nodeId));

        FlowCapableNodeBuilder aug = new FlowCapableNodeBuilder();
        aug.setManufacturer(id);
        node.addAugmentation(FlowCapableNode.class, aug.build());

        return node.build();
    }

}
