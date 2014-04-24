package org.opendaylight.controller.datastore.notification;

import junit.framework.Assert;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.Map;

/**
 * @author: syedbahm
 * Date: 4/22/14
 */
public class FamilyModelDataChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
  public static enum ExpectedOperation {ADDED, UPDATED, REMOVED}

  ;
  ExpectedOperation expectedOperation;
  NormalizedNode<?, ?> expectedOne;

  InstanceIdentifier expectedInstanceIdentifier;



  AsyncDataBroker.DataChangeScope  scope;

  final String testCaseName;

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
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : added.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());
          //TODO: once snapshot issue is fixed assert here

        }

          Assert.assertNull(change.getOriginalSubtree());
          Assert.assertNotNull(change.getUpdatedSubtree());
          if (change.getOriginalData() != null && !change.getOriginalData().isEmpty()) {
            Assert.fail("failure original data found");
          }

          if (change.getUpdatedData() != null && !change.getUpdatedData().isEmpty()) {
            Assert.fail("failure updated data found");
          }

      } else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.BASE && testCaseName.equals("RegisterBaseListenerOnChildAndUpdateChildName")) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> updated = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : updated.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());
          Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());
          System.out.println("expected nodetype:" + expectedOne.getNodeType());
          System.out.println("actual nodetype:" + entry.getValue().getNodeType());

          Assert.assertNotNull(change.getOriginalSubtree());
          Assert.assertNotNull(change.getUpdatedSubtree());
          //  Assert.assertNull(change.getCreatedData()); TODO: This is commented as there issue with snapshot  data and data captured during
          Assert.assertNotNull(change.getOriginalData());

          if (change.getRemovedPaths() != null && !change.getRemovedPaths().isEmpty()) {
            Assert.fail("failure removed data found");
          }
          Assert.assertEquals(entry.getValue().getValue(),"first child of the family");

        }
      } else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.ONE && testCaseName.equals("RegisterOneLevelListenerFamilyAndAddAChild")) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> added = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getCreatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : added.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());
          //TODO : assert here once snapshot issue is fixed.

        }
        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        if (change.getOriginalData() != null && !change.getOriginalData().isEmpty()) {
          Assert.fail("failure original data found");
        }

        if (change.getUpdatedData() != null && !change.getUpdatedData().isEmpty()) {
          Assert.fail("failure updated data found");
        }
      }else if (expectedOperation == ExpectedOperation.UPDATED  && scope == AsyncDataBroker.DataChangeScope.SUBTREE) {
        Map<InstanceIdentifier, NormalizedNode<?, ?>> updated = (Map<InstanceIdentifier, NormalizedNode<?, ?>>) change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> entry : updated.entrySet()) {
          System.out.println("Path identifying the InstanceIdenetifier:" + entry.getKey().getPath());
          Assert.assertEquals(entry.getKey().getPath(), expectedInstanceIdentifier.getPath());
          System.out.println("expected nodetype:" + expectedOne.getNodeType());
          System.out.println("actual nodetype:" + entry.getValue().getNodeType());
        }
        Assert.assertNotNull(change.getOriginalSubtree());
        Assert.assertNotNull(change.getUpdatedSubtree());
        //  Assert.assertNull(change.getCreatedData()); TODO: This is commented as there issue with snapshot  data and data captured during
        Assert.assertNotNull(change.getOriginalData());
        Assert.assertNull(change.getRemovedPaths());
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

