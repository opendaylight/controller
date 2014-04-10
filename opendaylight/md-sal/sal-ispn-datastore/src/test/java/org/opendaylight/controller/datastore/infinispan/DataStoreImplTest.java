package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodePrinter;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class DataStoreImplTest {
    private TreeCache treeCache;


    private SchemaContext schemaContext;

    private static TreeCacheManager tcm = TreeCacheManagerSingleton.get();


    @Before
    public void setUp(){
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
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

        final NormalizedNode<?, ?> documentOne = TestModel.createDocumentOne(schemaContext);

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

        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(null, TestModel.createTestContainer());


        Node n1 = treeCache.getNode(Fqn.fromString("/urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"));


    }


    @Test
    public void normalizedNodeToTreeCacheSerialization(){

        treeCache = tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);
        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(null, TestModel.createTestContainer());
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

        final ContainerNode containerNode = TestModel.createTestContainer();
        new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(instanceIdentifier, containerNode);

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


    @Test
    public void testDataStore() throws Exception{
        final DataStoreImpl dataStore = new DataStoreImpl(tcm);
        dataStore.onGlobalContextUpdated(schemaContext);
        final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

        List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

        String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

        String[] ids = nodeWithValue.split("/");
        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }

        final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

        domStoreReadWriteTransaction.write(null, TestModel.createTestContainer());
        final ListenableFuture<Optional<NormalizedNode<?,?>>> optionalListenableFuture = domStoreReadWriteTransaction.read(instanceIdentifier);

        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
        final NormalizedNode<?, ?> normalizedNode = normalizedNodeOptional.get();

        final ListenableFuture<Void> listenableFuture = domStoreReadWriteTransaction.ready().commit();
        listenableFuture.addListener(new Runnable(){
            @Override
            public void run() {
                DOMStoreReadTransaction domStoreReadTransaction = dataStore.newReadOnlyTransaction();

                final ListenableFuture<Optional<NormalizedNode<?, ?>>> readNode = domStoreReadTransaction.read(instanceIdentifier);
                Assert.assertNotNull(readNode);

            }
        }, Executors.newSingleThreadExecutor());


        Thread.sleep(3000);

    }


}
