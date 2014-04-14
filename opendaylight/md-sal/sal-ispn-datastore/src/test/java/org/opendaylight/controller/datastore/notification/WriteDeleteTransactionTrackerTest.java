package org.opendaylight.controller.datastore.notification;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import junit.framework.Assert;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.DataStoreImpl;
import org.opendaylight.controller.datastore.infinispan.TreeCacheManagerSingleton;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.*;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;

/**
 * @author: syedbahm
 * Date: 4/17/14
 */
public class WriteDeleteTransactionTrackerTest {


  private SchemaContext schemaContext;


  @Before
  public void beforeClass()throws Exception{
    schemaContext = FamilyModel.createTestContext();
    assertNotNull("Schema context must not be null.", schemaContext);
  }

  public NormalizedNode<?,?> prepareFamilyTree ()throws Exception{

    DataStoreImpl.setTreeCacheManager(TreeCacheManagerSingleton.get());
    final DataStoreImpl dataStore = new DataStoreImpl("configuration",schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);
    final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

    //this seems to be root of the test container
    String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-15)family";

    //spliting this and creating list
    String[] ids = nodeWithValue.split("/");
    for(String nodeId : ids){
      if(!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

    domStoreReadWriteTransaction.write(null, FamilyModel.createTestContainer());


     final ListenableFuture<Optional<NormalizedNode<?,?>>> optionalListenableFuture = domStoreReadWriteTransaction.read(instanceIdentifier);

    final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
    final NormalizedNode<?, ?> normalizedNode = normalizedNodeOptional.get();



    final ListenableFuture<Void> listenableFuture = domStoreReadWriteTransaction.ready().commit();

    DOMStoreReadTransaction domStoreReadTransaction = dataStore.newReadOnlyTransaction();

    final ListenableFuture<Optional<NormalizedNode<?, ?>>> readNode = domStoreReadTransaction.read(instanceIdentifier);

    final NormalizedNode<?, ?> readNormalizedNode = readNode.get().get();
    Assert.assertNotNull(readNormalizedNode);
    return readNormalizedNode;

  }
  @Test
  public void prepareFamilyTree1()throws Exception{
    prepareFamilyTree();
  }
  @Test
  public void testPreparePotentialTasks() throws Exception {

  }




  private Node prepareTree (TreeCache tc){
    Map personA = new HashMap<>();
    personA.put("person", "a");
    tc.put(Fqn.fromString("/a"),personA );
    Map childrenOfA =  new HashMap <>();
    childrenOfA.put("child 1","B");
    tc.put(Fqn.fromString("/a/b"),childrenOfA );
    childrenOfA.clear();
    childrenOfA.put("child 2","C");
    tc.put(Fqn.fromString("/a/c"),childrenOfA );
    childrenOfA.clear();
    childrenOfA.put("child 3","D");
    tc.put(Fqn.fromString("/a/d"),childrenOfA );

    Map childrenOfB = new HashMap <>();
    childrenOfB.put("child 1","E");
    tc.put(Fqn.fromString("/a/b/e"),childrenOfB );
    childrenOfB.clear();
    childrenOfB.put("child 2","F");
    tc.put(Fqn.fromString("/a/b/f"),childrenOfB );
    childrenOfB.clear();
    childrenOfB.put("child 3","G");
    tc.put(Fqn.fromString("/a/b/g"),childrenOfB );


    Map personH = new HashMap<>();
    personH.put("person", "H");
    tc.put(Fqn.fromString("/h"),personH );
    Map childrenOfH =  new HashMap <>();
    childrenOfH.put("child 1","I");
    tc.put(Fqn.fromString("/h/i"),childrenOfH );
    childrenOfH.clear();
    childrenOfH.put("child 2","J");
    tc.put(Fqn.fromString("/h/j"),childrenOfH );
    childrenOfH.clear();


    Map childrenOfJ = new HashMap <>();
    childrenOfJ.put("child 1","K");
    tc.put(Fqn.fromString("/h/j/k"),childrenOfJ );

    Node root =  tc.getRoot();
    return prepareSnapshot(root);
  }
  Node prepareSnapshot(Node root){
    Set<Node> children = root.getChildren();
    for(Node child:children){
      if(child != null){
        prepareSnapshot(child);
      }
    }
    return root;
  }
  private void treeCacheSample() throws Exception {
    WriteDeleteTransactionTracker wdtt = new WriteDeleteTransactionTracker();
    TreeCacheManager tcm = TreeCacheManagerSingleton.get();
    TreeCache tc = tcm.getCache("Sample");

    // the following is our Tree
    // 1. b,c,d are children of a
    // 2. e,f,g are children of b  i.e. grand children of a
    // 3. h is sibling of a and has children i,j
    // 4. k is child of j i.e. grandchild of h

    Node snapshot = prepareTree(tc);




    //now we want to make some operation on the treeCache and track the same in WriteDeleteTransactionTracker

    //Transactions
    //1. we will add   l as a child of k i.e. great-grandchild of h
    //2, we will update l as great-grandchild-of-h
    //3. we will remove d as child of a
    //4. we will remove i as child of h

    Map childrenOfK = new HashMap <>();
    childrenOfK.put("child 1","L");
    tc.put(Fqn.fromString("/h/j/k/l"), childrenOfK);



  }
}
