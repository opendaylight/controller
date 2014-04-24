package org.opendaylight.controller.datastore.notification;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.DataStoreImpl;
import org.opendaylight.controller.datastore.infinispan.TreeCacheManagerSingleton;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;

/**
 * @author: syedbahm
 * Date: 4/17/14
 */
public class TransactionTrackerListenerLevelBaseTest {


  private SchemaContext schemaContext;


  @Before
  public void beforeClass() throws Exception {
    schemaContext = FamilyModel.createTestContext();
    assertNotNull("Schema context must not be null.", schemaContext);
  }


  @Test
  public void RegisterBaseListenerOnChildAndUpdateChildName() throws Exception {

    final DataStoreImpl dataStore = new DataStoreImpl(TreeCacheManagerSingleton.get().getCache("RegisterBaseListenerOnChildAndUpdateChildName"), "configuration", schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);

    NormalizedNode normalizedNode = FamilyModel.prepareFamilyTree(schemaContext, dataStore);

    String firstChildNamePath =
        "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)children/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)children[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)child-number=1}]/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)child-name";
    String familyPath = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family";

    //ok here we will register listener on one of children
    //spliting this and creating list
    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
    String[] ids = firstChildNamePath.split("/");
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

    //ok here we will register listener on one of children
    //spliting this and creating list
    List<InstanceIdentifier.PathArgument> pathArguments1 = new ArrayList<>();
    ids = familyPath.split("/");
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments1.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifierFamily = new InstanceIdentifier(pathArguments1);


    final DOMStoreReadTransaction domStoreReadTransaction = dataStore.newReadOnlyTransaction();
    //let us read the previous value of this instance identifier
    final ListenableFuture<Optional<NormalizedNode<?, ?>>> optionalListenableFuture = domStoreReadTransaction.read(instanceIdentifier);

    final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();

    if (normalizedNodeOptional.isPresent()) {
      normalizedNode = normalizedNodeOptional.get();
    } else {
      normalizedNode = null;
    }

    Assert.assertEquals(normalizedNode.getValue(), FamilyModel.FIRST_CHILD_NAME);


    //here we want to register a single listener at the OUTER_LIST_PATH instance identifer level

    FamilyModelDataChangeListener dcl1 = new FamilyModelDataChangeListener(normalizedNode,
        FamilyModelDataChangeListener.ExpectedOperation.ADDED,
        instanceIdentifier,
        AsyncDataBroker.DataChangeScope.BASE, "RegisterBaseListenerOnChildAndUpdateChildName");

    //ok let us register listener here
    dataStore.registerChangeListener(instanceIdentifier, dcl1, AsyncDataBroker.DataChangeScope.BASE);

    //let us change the first child to first child of family


    final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

    domStoreReadWriteTransaction.write(instanceIdentifierFamily, FamilyModel.createTestContainerWithFirstChildNameChanged("first child of the family"));

    //let us set what we are expecting from the listener side
    dcl1.setExpectedInstanceIdentifier(instanceIdentifier);
    dcl1.setExpectedOne(normalizedNode);
    dcl1.setExpectedOperation(FamilyModelDataChangeListener.ExpectedOperation.UPDATED);


    DOMStoreThreePhaseCommitCohort t = domStoreReadWriteTransaction.ready();
    t.preCommit();
    t.commit();

    synchronized (dcl1) {
      dcl1.wait(1000);
    }

    Assert.assertTrue(dcl1.updateEventReceived);

  }




  public void RegisterBaseListenerOnChildrenAndRemoveFirstChild() throws Exception {

    final DataStoreImpl dataStore = new DataStoreImpl(TreeCacheManagerSingleton.get().getCache("RegisterBaseListenerOnChildrenAndRemoveSecondChild"), "configuration", schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);

    NormalizedNode normalizedNode = FamilyModel.prepareFamilyTree(schemaContext,dataStore);

    String childrenOne =
    "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)children";
    String familyPath = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family";

    //ok here we will register listener on one of children
    //spliting this and creating list
    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
    String[] ids = childrenOne.split("/");
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

    //ok here we will register listener on one of children
    //spliting this and creating list
    List<InstanceIdentifier.PathArgument> pathArguments1 = new ArrayList<>();
    ids = familyPath.split("/");
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments1.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    InstanceIdentifier instanceIdentifierFamily = new InstanceIdentifier(pathArguments1);


    DOMStoreReadTransaction domStoreReadTransaction = dataStore.newReadOnlyTransaction();
    //let us read the previous value of this instance identifier
    ListenableFuture<Optional<NormalizedNode<?, ?>>> optionalListenableFuture = domStoreReadTransaction.read(instanceIdentifier);

    Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();

    if (normalizedNodeOptional.isPresent()) {
      normalizedNode = normalizedNodeOptional.get();
    } else {
      normalizedNode = null;
    }

    Assert.assertNotNull(normalizedNode);


    //here we want to register a single listener at the OUTER_LIST_PATH instance identifer level

    FamilyModelDataChangeListener dcl1 = new FamilyModelDataChangeListener(normalizedNode,
        FamilyModelDataChangeListener.ExpectedOperation.ADDED,
        instanceIdentifier,
        AsyncDataBroker.DataChangeScope.BASE, "RegisterBaseListenerOnChildrenAndRemoveFirstChild");

    //ok let us register listener here
    dataStore.registerChangeListener(instanceIdentifier, dcl1, AsyncDataBroker.DataChangeScope.BASE);




    final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

    domStoreReadWriteTransaction.delete(instanceIdentifier);


    //ok let us read the new removed children

    domStoreReadTransaction = dataStore.newReadOnlyTransaction();
    //let us read the previous value of this instance identifier
    optionalListenableFuture = domStoreReadTransaction.read(instanceIdentifier);

     normalizedNodeOptional = optionalListenableFuture.get();

    if (normalizedNodeOptional.isPresent()) {
      normalizedNode = normalizedNodeOptional.get();
    } else {
      normalizedNode = null;
    }

    Assert.assertNull(normalizedNode);


    //let us set what we are expecting from the listener side
    dcl1.setExpectedInstanceIdentifier(instanceIdentifier);
    dcl1.setExpectedOne(normalizedNode);
    dcl1.setExpectedOperation(FamilyModelDataChangeListener.ExpectedOperation.UPDATED);
    dcl1.setScope(AsyncDataBroker.DataChangeScope.BASE) ;


    DOMStoreThreePhaseCommitCohort t = domStoreReadWriteTransaction.ready();
    t.preCommit();
    t.commit();

    synchronized (dcl1) {
      dcl1.wait(10000);
    }

    Assert.assertTrue(dcl1.updateEventReceived);

  }


  @Test
  public void prepareFamilyTree1() throws Exception {
    final DataStoreImpl dataStore = new DataStoreImpl(TreeCacheManagerSingleton.get().getCache("listenerlevelbase"), "configuration", schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);
    FamilyModel.prepareFamilyTree(schemaContext, dataStore);
  }

}

