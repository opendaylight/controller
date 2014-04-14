package org.opendaylight.controller.datastore.notification;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.DataStoreImpl;
import org.opendaylight.controller.datastore.infinispan.TestModel;
import org.opendaylight.controller.datastore.infinispan.TreeCacheManagerSingleton;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;

public class DataStoreImplNotificationTest {



    private SchemaContext schemaContext;



    @Before
    public void beforeClass()throws Exception{
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
    }


    @Test
    public void testNotificationRegistrationInitialSnapshotEvent() throws Exception{

        DataStoreImpl.setTreeCacheManager(TreeCacheManagerSingleton.get());
        final DataStoreImpl dataStore = new DataStoreImpl("configuration",schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
        dataStore.onGlobalContextUpdated(schemaContext);
        final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

        List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

        //this seems to be root of the test container
        String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

        //spliting this and creating list
        String[] ids = nodeWithValue.split("/");
        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }
        //creating instance identifier
        final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

        domStoreReadWriteTransaction.write(null, TestModel.createTestContainer());



      final ListenableFuture<Optional<NormalizedNode<?,?>>> optionalListenableFuture = domStoreReadWriteTransaction.read(instanceIdentifier);

        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
        final NormalizedNode<?, ?> normalizedNode = normalizedNodeOptional.get();



        final ListenableFuture<Void> listenableFuture = domStoreReadWriteTransaction.ready().commit();
      //ok let us register listener here
      dataStore.registerChangeListener(instanceIdentifier, new DataChangeListener(normalizedNode,DataChangeListener.ExpectedOperation.ADDED,instanceIdentifier), AsyncDataBroker.DataChangeScope.BASE);
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

  @Test
  public void testMulitpleNotificationRegistration() throws Exception{

    DataStoreImpl.setTreeCacheManager(TreeCacheManagerSingleton.get());
    final DataStoreImpl dataStore = new DataStoreImpl("configuration",schemaContext, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    dataStore.onGlobalContextUpdated(schemaContext);
    final DOMStoreReadWriteTransaction domStoreReadWriteTransaction = dataStore.newReadWriteTransaction();

    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();

    //this seems to be root of the test container
    String nodeWithValue = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

    //spliting this and creating list
    String[] ids = nodeWithValue.split("/");
    for(String nodeId : ids){
      if(!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    //creating instance identifier
    final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);

    domStoreReadWriteTransaction.write(null, TestModel.createTestContainer());



    final ListenableFuture<Optional<NormalizedNode<?,?>>> optionalListenableFuture = domStoreReadWriteTransaction.read(instanceIdentifier);

    final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = optionalListenableFuture.get();
    final NormalizedNode<?, ?> normalizedNode = normalizedNodeOptional.get();



    final ListenableFuture<Void> listenableFuture = domStoreReadWriteTransaction.ready().commit();
    DataChangeListener dcl1 = new DataChangeListener(normalizedNode,DataChangeListener.ExpectedOperation.ADDED,instanceIdentifier);
    DataChangeListener dcl2 = new DataChangeListener(normalizedNode,DataChangeListener.ExpectedOperation.ADDED,instanceIdentifier);
    //ok let us register listener here
    dataStore.registerChangeListener(instanceIdentifier, dcl1, AsyncDataBroker.DataChangeScope.BASE);
    dataStore.registerChangeListener(instanceIdentifier, dcl2, AsyncDataBroker.DataChangeScope.SUBTREE);
    Map<InstanceIdentifier,Collection<?>> listenersMap =  dataStore.listeners();
    for(Map.Entry<InstanceIdentifier,Collection<?>>registration :listenersMap.entrySet()){
       Assert.assertEquals(registration.getKey(),instanceIdentifier) ;

       Assert.assertEquals(registration.getValue().size(),2);
       ArrayList al = new ArrayList();
       al.add(dcl1);
       al.add(dcl2);
       Assert.assertTrue(registration.getValue().containsAll(al));


    }

    listenableFuture.addListener(new Runnable() {
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


class DataChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
  public static enum ExpectedOperation {ADDED,UPDATED,REMOVED};
  final ExpectedOperation expectedOperation;
  final NormalizedNode<?,?>expectedOne;
  final InstanceIdentifier expectedInstanceIdentifier;

  public DataChangeListener(NormalizedNode<?,?>expected,ExpectedOperation operation,InstanceIdentifier id ) {
       expectedOne = expected;
       expectedOperation = operation;
       expectedInstanceIdentifier = id;
  }

  @Override
  public void onDataChanged(
      final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

    try{
     //here we will compare with the execpted one and the original one.
     if(expectedOperation ==ExpectedOperation.ADDED){
       Map<InstanceIdentifier,NormalizedNode<?,?>> added = (Map<InstanceIdentifier,NormalizedNode<?,?>>)change.getCreatedData();
       for(Map.Entry<InstanceIdentifier,NormalizedNode<?,?>> entry:added.entrySet()) {
         System.out.println("Path identifying the InstanceIdenetifier:"+entry.getKey().getPath());
         Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());
         System.out.println("expected nodetype:" + expectedOne.getNodeType());
         System.out.println("actual nodetype:"+entry.getValue().getNodeType());

         Assert.assertEquals(entry.getValue().getNodeType(),expectedOne.getNodeType());
         System.out.println("actual value:" + entry.getValue().getValue());
         System.out.println("expected value :" + expectedOne.getValue());
         Assert.assertEquals(entry.getValue().getValue().toString(), expectedOne.getValue().toString());
       }
     }else{
          throw new UnsupportedOperationException();
     }
    }catch(Throwable t){
      t.printStackTrace();
      Assert.assertNull(t);
    }
  }
}
