/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.AugmentationVerifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class MultipleAugmentationPutsTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName TOP_LEVEL_LIST_ID_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final String TOP_LEVEL_LIST_ID = "foo";

    private static final TopLevelListKey TOP_LEVEL_LIST_KEY = new TopLevelListKey(TOP_LEVEL_LIST_ID);

    private static final Map<QName, Object> TOP_KEY_BI = Collections.<QName, Object> singletonMap(TOP_LEVEL_LIST_ID_QNAME,
            TOP_LEVEL_LIST_ID);

    private static final InstanceIdentifier<Top> TOP_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .toInstance();

    private static final InstanceIdentifier<TopLevelList> TOP_LEVEL_LIST_INSTANCE_ID_BA =
            TOP_INSTANCE_ID_BA.child(TopLevelList.class, TOP_LEVEL_LIST_KEY);

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier TOP_LEVEL_LIST_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, TOP_KEY_BI) //
            .toInstance();
    private DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test()
    public void testAugmentSerialization() throws Exception {

        baDataService.registerDataChangeListener(TOP_INSTANCE_ID_BA, this);

        TopLevelList topLevelListAugmented = createTestTopLevelListAug(TreeLeafOnlyAugment.class, treeLeafOnlyAugmentation());
        commitTopLevelListAndVerifyTransaction(topLevelListAugmented);

        assertNotNull(receivedChangeEvent);
        verifyTopLevelList((Top) receivedChangeEvent.getUpdatedOperationalSubtree(), topLevelListAugmented);

        Top top = checkForTop();
        verifyTopLevelList(top, topLevelListAugmented).assertHasAugmentation(TreeLeafOnlyAugment.class);
        assertBindingIndependentVersion(TOP_LEVEL_LIST_INSTANCE_ID_BI);

        testNodeRemove();
    }

    private <T extends Augmentation<TopLevelList>> TopLevelList createTestTopLevelListAug(final Class<T> augmentationClass, final T augmentation) {
        TopLevelListBuilder tllBuilder = new TopLevelListBuilder();
        tllBuilder.setKey(new TopLevelListKey(TOP_LEVEL_LIST_ID));
        tllBuilder.addAugmentation(augmentationClass, augmentation);
        return tllBuilder.build();
    }

    private DataModificationTransaction commitTopLevelListAndVerifyTransaction(final TopLevelList original) throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putOperationalData(TOP_LEVEL_LIST_INSTANCE_ID_BA, original);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        return transaction;
    }

    private void testNodeRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(TOP_LEVEL_LIST_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList tll = (TopLevelList) baDataService.readOperationalData(TOP_LEVEL_LIST_INSTANCE_ID_BA);
        assertNull(tll);
    }

    private AugmentationVerifier<TopLevelList> verifyTopLevelList(final Top top, final TopLevelList original) {
        assertNotNull(top);
        assertNotNull(top.getTopLevelList());
        assertEquals(1, top.getTopLevelList().size());
        TopLevelList tll = top.getTopLevelList().get(0);
        assertEquals(original.getName(), tll.getName());
        assertEquals(original.getKey(), tll.getKey());
        return new AugmentationVerifier<TopLevelList>(tll);
    }

    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier tllId) {
        CompositeNode tll = biDataService.readOperationalData(tllId);
        assertNotNull(tll);
    }

    private Top checkForTop() {
        return (Top) baDataService.readOperationalData(TOP_INSTANCE_ID_BA);
    }


    private TreeLeafOnlyAugment treeLeafOnlyAugmentation() {
        TreeLeafOnlyAugmentBuilder tloaBuilder = new TreeLeafOnlyAugmentBuilder();
        tloaBuilder.setSimpleValue("foo augment");
        return tloaBuilder.build();
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent = change;
    }

}
