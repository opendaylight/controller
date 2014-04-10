package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodePrinter;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.controller.datastore.infinispan.TestModel.DESC_PATH;
import static org.opendaylight.controller.datastore.infinispan.TestModel.DESC_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.ID_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.INNER_LIST_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.NAME_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.OUTER_LIST_PATH;
import static org.opendaylight.controller.datastore.infinispan.TestModel.OUTER_LIST_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.TEST_QNAME;
import static org.opendaylight.controller.datastore.infinispan.TestModel.VALUE_QNAME;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;
//Datastore testing
public class DataStoreImplTest {
    private TreeCache treeCache;
    private static final Integer ONE_ID = 1;
    private static final Integer TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";
    private static final String DESC="Hello there";

    private static final InstanceIdentifier DESC_PATH_ID = InstanceIdentifier.builder(DESC_PATH).build();
    private static final InstanceIdentifier OUTER_LIST_1_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, ONE_ID) //
            .build();

    private static final InstanceIdentifier OUTER_LIST_2_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .build();

    private static final InstanceIdentifier TWO_TWO_PATH = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
            .node(INNER_LIST_QNAME) //
            .nodeWithKey(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME) //
            .build();

    private static final InstanceIdentifier TWO_TWO_VALUE_PATH = InstanceIdentifier.builder(TWO_TWO_PATH)
            .node(VALUE_QNAME) //
            .build();

    private static final MapEntryNode BAR_NODE = mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .withChild(mapNodeBuilder(INNER_LIST_QNAME) //
                .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME)) //
                .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME)) //
                .build()) //
            .build();

    private SchemaContext schemaContext;

    private static TreeCacheManager tcm = TreeCacheManagerSingleton.get();


    @Before
    public void setUp(){
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);


    }


    /**
     * Returns a test document
     *
     * <pre>
     * test
     *     outer-list
     *          id 1
     *     outer-list
     *          id 2
     *          inner-list
     *                  name "one"
     *          inner-list
     *                  name "two"
     *
     * </pre>
     *
     * @return
     */
    public NormalizedNode<?, ?> createDocumentOne() {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
                .withChild(createTestContainer()).build();

    }

    private ContainerNode createTestContainer() {


        final LeafSetEntryNode<Object> nike = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "shoe"), "nike")).withValue("nike").build();
        final LeafSetEntryNode<Object> puma = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "shoe"), "puma")).withValue("puma").build();
        final LeafSetNode<Object> shoes = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME, "shoe"))).withChild(nike).withChild(puma).build();


        final LeafSetEntryNode<Object> five = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier((new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "number"), 5))).withValue(5).build();
        final LeafSetEntryNode<Object> fifteen = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier((new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "number"), 15))).withValue(15).build();
        final LeafSetNode<Object> numbers = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME, "number"))).withChild(five).withChild(fifteen).build();

        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(DESC_QNAME, DESC))
                .withChild(shoes)
                .withChild(numbers)
                .withChild(
                        mapNodeBuilder(OUTER_LIST_QNAME)
                                .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                                .withChild(BAR_NODE).build()).build();

    }

    @Test
    public void basicTreeCacheTest(){

        treeCache =  tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);

        treeCache.put(Fqn.fromString("/nodes/node/of:1"), "name", "foo");
        treeCache.put(Fqn.fromString("/nodes/node/of:2"), "name", "bar");

        final Node node = treeCache.getNode(Fqn.fromString("/"));

        final Node child = node.getChild(Fqn.fromString("nodes/node/of:1"));

        assertEquals("foo", child.get("name"));
    }



    public void basicNormalizedNodeCreationTest(){


        treeCache =  tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);


        // Need to be able to deal with
        //  - DataContainerNode
        //  - MixinNode
        //  - Any other node is probably a simple node?
        InstanceIdentifier.builder(QName.create("/"));

        final NormalizedNode<?, ?> documentOne = createDocumentOne();

        QName name = QName.create(documentOne.getIdentifier().toString());

        new NormalizedNodeNavigator(new NormalizedNodePrinter()).navigate(null, documentOne);


        DataNormalizationOperation operation = DataNormalizationOperation.from(schemaContext);

        final InstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifierWithPredicates = new InstanceIdentifier.NodeIdentifierWithPredicates(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"), QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=1"), "1");

        final NormalizedNode normalizedNode = operation.getChild(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"))
                .getChild(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"))
                .getChild(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"))
                .createDefault(nodeIdentifierWithPredicates);



        final InstanceIdentifier two = InstanceIdentifier.builder().node(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"))
                .node(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"))
                .nodeWithKey(QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"), QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)name"), "two").build();

        System.out.println(two.toString());

        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(null, createTestContainer());


        Node n1 = treeCache.getNode(Fqn.fromString("/urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"));


    }


    @Test
    public void normalizedNodeToTreeCacheSerialization(){

        treeCache = tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);
        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(null, createTestContainer());
        Node n1 = treeCache.getNode(Fqn.fromString("/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"));
    }


    @Test
    public void toInstanceIdentifier(){

        treeCache =  tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);


        String id = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=2}]/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id";

        String [] ids = id.split("/");

        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                NodeIdentifierFactory.getArgument(nodeId);
            }
        }

//        String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)shoe";
        String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

        ids = nodeWithValue.split("/");

        List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }

        final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

        final ContainerNode containerNode = createTestContainer();
        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(null, containerNode);

        NormalizedNode normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(instanceIdentifier, treeCache.getNode(nodeWithValue));

        assertTrue(compareNodes(containerNode, normalizedNode));

        new NormalizedNodeNavigator(new NormalizedNodePrinter()).navigate(null, containerNode);
        new NormalizedNodeNavigator(new NormalizedNodePrinter()).navigate(null, normalizedNode);

    }


    private boolean compareNodes(NormalizedNode expected, NormalizedNode actual){
        final boolean equals = expected.getNodeType().equals(actual.getNodeType());

        if(expected instanceof DataContainerNode){

        }

        return equals;
    }



}
