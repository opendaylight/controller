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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
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
  public void beforeClass() throws Exception {
    schemaContext = FamilyModel.createTestContext();
    assertNotNull("Schema context must not be null.", schemaContext);
  }

  public NormalizedNode<?, ?> prepareFamilyTree() throws Exception {

    DataStoreImpl.setTreeCacheManager(TreeCacheManagerSingleton.get());
    final DataStoreImpl dataStore = new DataStoreImpl("configuration", schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);
    final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

    //this seems to be root of the test container
    String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family";

    //spliting this and creating list
    String[] ids = nodeWithValue.split("/");
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

    domStoreReadWriteTransaction.write(null, FamilyModel.createTestContainer());


    final ListenableFuture<Optional<NormalizedNode<?, ?>>> optionalListenableFuture = domStoreReadWriteTransaction.read(instanceIdentifier);

    final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
    NormalizedNode<?, ?> normalizedNode;
    if (!normalizedNodeOptional.isPresent()) {
      normalizedNode = normalizedNodeOptional.get();
    } else {
      normalizedNode = null;
    }


    //here we want to register a single listener at the OUTER_LIST_PATH instance identifer level

    FamilyModelBaseNodeChangeListener dcl1 = new FamilyModelBaseNodeChangeListener(normalizedNode, FamilyModelBaseNodeChangeListener.ExpectedOperation.ADDED, instanceIdentifier);

    dataStore.registerChangeListener(instanceIdentifier, dcl1, AsyncDataBroker.DataChangeScope.BASE);

    DOMStoreThreePhaseCommitCohort t = domStoreReadWriteTransaction.ready();
    t.preCommit();
    t.commit();

    DOMStoreReadTransaction domStoreReadTransaction = dataStore.newReadOnlyTransaction();

    Thread.sleep(1000);
    final ListenableFuture<Optional<NormalizedNode<?, ?>>> readNode = domStoreReadTransaction.read(instanceIdentifier);

    final NormalizedNode<?, ?> readNormalizedNode = readNode.get().get();
    Assert.assertNotNull(readNormalizedNode);
    return readNormalizedNode;

  }

  @Test
  public void prepareFamilyTree1() throws Exception {
    prepareFamilyTree();
  }


  private Node prepareTree(TreeCache tc) {
    Map personA = new HashMap<>();
    personA.put("person", "a");
    tc.put(Fqn.fromString("/a"), personA);
    Map childrenOfA = new HashMap<>();
    childrenOfA.put("child 1", "B");
    tc.put(Fqn.fromString("/a/b"), childrenOfA);
    childrenOfA.clear();
    childrenOfA.put("child 2", "C");
    tc.put(Fqn.fromString("/a/c"), childrenOfA);
    childrenOfA.clear();
    childrenOfA.put("child 3", "D");
    tc.put(Fqn.fromString("/a/d"), childrenOfA);

    Map childrenOfB = new HashMap<>();
    childrenOfB.put("child 1", "E");
    tc.put(Fqn.fromString("/a/b/e"), childrenOfB);
    childrenOfB.clear();
    childrenOfB.put("child 2", "F");
    tc.put(Fqn.fromString("/a/b/f"), childrenOfB);
    childrenOfB.clear();
    childrenOfB.put("child 3", "G");
    tc.put(Fqn.fromString("/a/b/g"), childrenOfB);


    Map personH = new HashMap<>();
    personH.put("person", "H");
    tc.put(Fqn.fromString("/h"), personH);
    Map childrenOfH = new HashMap<>();
    childrenOfH.put("child 1", "I");
    tc.put(Fqn.fromString("/h/i"), childrenOfH);
    childrenOfH.clear();
    childrenOfH.put("child 2", "J");
    tc.put(Fqn.fromString("/h/j"), childrenOfH);
    childrenOfH.clear();


    Map childrenOfJ = new HashMap<>();
    childrenOfJ.put("child 1", "K");
    tc.put(Fqn.fromString("/h/j/k"), childrenOfJ);

    Node root = tc.getRoot();
    return prepareSnapshot(root);
  }

  Node prepareSnapshot(Node root) {
    Set<Node> children = root.getChildren();
    for (Node child : children) {
      if (child != null) {
        prepareSnapshot(child);
      }
    }
    return root;
  }


}


class FamilyModelBaseNodeChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
  public static enum ExpectedOperation {ADDED, UPDATED, REMOVED}

  ;
  final ExpectedOperation expectedOperation;
  final NormalizedNode<?, ?> expectedOne;
  final InstanceIdentifier expectedInstanceIdentifier;

  public FamilyModelBaseNodeChangeListener(NormalizedNode<?, ?> expected, ExpectedOperation operation, InstanceIdentifier id) {
    expectedOne = expected;
    expectedOperation = operation;
    expectedInstanceIdentifier = id;
  }

  @Override
  public void onDataChanged(
      final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

    try {
      //here we will compare with the execpted one and the original one.
      if (expectedOperation == ExpectedOperation.ADDED) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> added = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getCreatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : added.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());

          Assert.assertNull(change.getOriginalSubtree());
          Assert.assertNotNull(change.getUpdatedSubtree());
          if (change.getOriginalData() != null && !change.getOriginalData().isEmpty()) {
            Assert.fail("failure original data found");
          }
        }
      } else {
        throw new UnsupportedOperationException();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      Assert.assertNull(t);
    }
  }
}

