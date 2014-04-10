package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.infinispan.tree.TreeCache;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;

public class ReadWriteTransactionActorTest {

    private SchemaContext schemaContext;

    private static TreeCacheManager tcm = TreeCacheManagerSingleton.get();


    @Before
    public void setUp(){
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
    }

    @Test
    public void testBasic() throws Exception{
        TreeCache treeCache = tcm.getCache(DataStoreImpl.DEFAULT_STORE_CACHE_NAME);
        final ReadWriteTransactionActor actor1 = new ReadWriteTransactionActor(schemaContext,
                treeCache,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                1);

        List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

        String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

        String[] ids = nodeWithValue.split("/");
        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }

        final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

        actor1.write(null, TestModel.createTestContainer());

        final ListenableFuture<Optional<NormalizedNode<?,?>>> optionalListenableFuture = actor1.read(instanceIdentifier);
        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
        final NormalizedNode<?, ?> normalizedNode = normalizedNodeOptional.get();


        final ReadWriteTransactionActor actor2 = new ReadWriteTransactionActor(schemaContext,
                treeCache,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                2);


        actor2.write(null, TestModel.createTestContainer());

        actor1.close();

        actor2.close();
    }


}
