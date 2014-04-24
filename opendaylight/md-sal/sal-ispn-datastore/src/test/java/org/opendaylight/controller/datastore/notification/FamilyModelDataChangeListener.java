package org.opendaylight.controller.datastore.notification;

import junit.framework.Assert;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author: syedbahm
 * Date: 4/22/14
 */
public class FamilyModelDataChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
  ;
  public static enum ExpectedOperation {ADDED, UPDATED, REMOVED}

  ;
  ExpectedOperation expectedOperation;
  NormalizedNode<?, ?> expectedOne;

  InstanceIdentifier expectedInstanceIdentifier;



  AsyncDataBroker.DataChangeScope  scope;

  final String testCaseName;

  boolean createdEventReceived = false;
  boolean updateEventReceived = false;

  public FamilyModelDataChangeListener(NormalizedNode<?, ?> expected, ExpectedOperation operation, InstanceIdentifier id, AsyncDataBroker.DataChangeScope scope, String testCaseName) {
    expectedOne = expected;
    expectedOperation = operation;
    expectedInstanceIdentifier = id;
    this.scope = scope;
    this.testCaseName = testCaseName;
  }

  @Override
  public void onDataChanged(
      final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

    try {
      //here we will compare with the execpted one and the original one.
      if (expectedOperation == ExpectedOperation.ADDED && scope == AsyncDataBroker.DataChangeScope.BASE && testCaseName.equals("prepareFamilyTree")) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> added = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getCreatedData();
        Assert.assertNotNull(added);
        Assert.assertTrue(!added.isEmpty());

          Assert.assertNull(change.getOriginalSubtree());
          Assert.assertNotNull(change.getUpdatedSubtree());
          if (change.getOriginalData() != null && !change.getOriginalData().isEmpty()) {
            Assert.fail("failure original data found");
          }

          if (change.getUpdatedData() != null && !change.getUpdatedData().isEmpty()) {
            Assert.fail("failure updated data found");
          }
        this.createdEventReceived = true;

      } else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.BASE && testCaseName.equals("RegisterBaseListenerOnChildAndUpdateChildName")) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> updated = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : updated.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());
          Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());


          Assert.assertEquals(entry.getValue().getValue(),"first child of the family");

        }

        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        //  Assert.assertNull(change.getCreatedData()); TODO: This is commented as there issue with snapshot  data and data captured during
        Assert.assertNotNull(change.getOriginalData());

        if (change.getRemovedPaths() != null && !change.getRemovedPaths().isEmpty()) {
          Assert.fail("failure removed data found");
        }
        this.updateEventReceived = true;
      } else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.ONE && testCaseName.equals("RegisterOneLevelListenerFamilyAndAddAChild")) {

        Map<InstanceIdentifier, NormalizedNode<?, ?>> added = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getCreatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : added.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());

          if(entry.getKey().toString().equals("/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)family/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)children/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)children[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-17)child-number=3}]")){
              NormalizedNode<?,?>child3 = entry.getValue();
              Iterable<?>value = (Iterable<?>)child3.getValue();
              Iterator it =  value.iterator();
              it.next();

              NormalizedNode<?,?>childName = (NormalizedNode<?,?>)it.next();

             Assert.assertEquals(childName.getValue(),FamilyModel.THIRD_CHILD_NAME);
          }

        }

        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        if (change.getOriginalData() != null && !change.getOriginalData().isEmpty()) {
          Assert.fail("failure original data found");
        }

        if (change.getUpdatedData() != null && !change.getUpdatedData().isEmpty()) {
          Assert.fail("failure updated data found");
        }
        this.updateEventReceived = true;

      }else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.SUBTREE && testCaseName.equals("RegisterSubTreeListenerFamilyLevel")) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> updated = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : updated.entrySet()) {
          System.out.println("Path identifying the InstanceIdentifier:" + entry.getKey().getPath());
          Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());


          Assert.assertEquals(entry.getValue().getValue(),"first child of the family");

        }
        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        if(change.getCreatedData() != null && !change.getCreatedData().isEmpty()){
          Assert.fail("failure found a created one");
        }

        Assert.assertNotNull(change.getOriginalData());

        if (change.getRemovedPaths() != null && !change.getRemovedPaths().isEmpty()) {
          Assert.fail("failure removed data found");
        }
        this.updateEventReceived =true;

      }else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.BASE && testCaseName.equals("RegisterBaseListenerOnChildrenAndRemoveSecondChild")) {
        Set<InstanceIdentifier> removedSet =  change.getRemovedPaths();
        for (InstanceIdentifier entry : removedSet) {
          System.out.println("Path identifying the removed InstanceIdentifier:" + entry.getPath());
          //Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());
        }
        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        Assert.assertNotNull(change.getOriginalData());

        if (change.getUpdatedData() != null && !change.getUpdatedData().isEmpty()) {
          Assert.fail("failure updated data found");
        }

        this.updateEventReceived =true;

      }
      else
      {
        throw new UnsupportedOperationException();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      Assert.assertNull(t);
    }finally{
      this.notifyAll();
    }
  }
  public void setExpectedOperation(ExpectedOperation expectedOperation) {
    this.expectedOperation = expectedOperation;
  }

  public void setExpectedOne(NormalizedNode<?, ?> expectedOne) {
    this.expectedOne = expectedOne;
  }

  public void setExpectedInstanceIdentifier(InstanceIdentifier expectedInstanceIdentifier) {
    this.expectedInstanceIdentifier = expectedInstanceIdentifier;
  }

  public void setScope(AsyncDataBroker.DataChangeScope scope) {
    this.scope = scope;
  }
}

