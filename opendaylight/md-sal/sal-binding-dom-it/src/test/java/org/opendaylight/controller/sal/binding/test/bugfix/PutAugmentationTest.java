/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.NestedListSimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.NestedListSimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.SettableFuture;

@SuppressWarnings("deprecation")
public class PutAugmentationTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName TLL_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final String TLL_NAME = "foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);

    private static final Map<QName, Object> TLL_KEY_BI = Collections.<QName, Object> singletonMap(TLL_NAME_QNAME,
            TLL_NAME);

    private static final InstanceIdentifier<Top> TOP_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .toInstance();

    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = //
            TOP_INSTANCE_ID_BA.builder() //
            .child(TopLevelList.class, TLL_KEY).toInstance();

    private static final InstanceIdentifier<TllComplexAugment> ALL_TCA = //
            TOP_INSTANCE_ID_BA.builder() //
            .child(TopLevelList.class) //
            .augmentation(TllComplexAugment.class) //
            .build();

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier TLL_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
            .toInstance();
    private static final InstanceIdentifier<TllComplexAugment> TCA_AUGMENTATION_PATH =
            TLL_INSTANCE_ID_BA.builder() //
            .augmentation(TllComplexAugment.class) //
            .build();

    private SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> lastReceivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void putNodeAndAugmentation() throws Exception {
        lastReceivedChangeEvent = SettableFuture.create();
        baDataService.registerDataChangeListener(ALL_TCA, this);


        TopLevelListBuilder nodeBuilder = new TopLevelListBuilder();
        nodeBuilder.setKey(TLL_KEY);
        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(TLL_INSTANCE_ID_BA, nodeBuilder.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList tll = (TopLevelList) baDataService.readOperationalData(TLL_INSTANCE_ID_BA);
        assertNotNull(tll);
        assertEquals(TLL_KEY, tll.getKey());

        TllComplexAugmentBuilder tcab = new TllComplexAugmentBuilder();
        tcab.setAttrStr1("FooFoo");
        tcab.setAttrStr2("BarBar");
        TllComplexAugment tca = tcab.build();
        InstanceIdentifier<TreeComplexUsesAugment> augmentIdentifier = TLL_INSTANCE_ID_BA
                .augmentation(TreeComplexUsesAugment.class);
        DataModificationTransaction augmentedTransaction = baDataService.beginTransaction();
        augmentedTransaction.putOperationalData(augmentIdentifier, tca);


        lastReceivedChangeEvent = SettableFuture.create();
        result = augmentedTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);
        assertTrue(potential.getCreatedOperationalData().containsKey(TCA_AUGMENTATION_PATH));

        lastReceivedChangeEvent = SettableFuture.create();

        TopLevelList augmentedTll = (TopLevelList) baDataService.readOperationalData(TLL_INSTANCE_ID_BA);
        assertNotNull(tll);
        assertEquals(TLL_KEY, augmentedTll.getKey());
        System.out.println("Before assertion");
        assertNotNull(augmentedTll.getAugmentation(TllComplexAugment.class));
        TllComplexAugment readedAugmentation = augmentedTll.getAugmentation(TllComplexAugment.class);
        assertEquals(tca.getAttrStr2(), readedAugmentation.getAttrStr2());
//        assertBindingIndependentVersion(TLL_INSTANCE_ID_BI);
        testTllRemove();
        assertTrue(lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS).getRemovedOperationalData().contains(TCA_AUGMENTATION_PATH));
    }

    @Test
    @Ignore
    public void putNodeWithAugmentation() throws Exception {
        lastReceivedChangeEvent = SettableFuture.create();
        baDataService.registerDataChangeListener(ALL_TCA, this);

        TopLevelListBuilder nodeBuilder = new TopLevelListBuilder();
        nodeBuilder.setKey(TLL_KEY);
        TllComplexAugmentBuilder tcab = new TllComplexAugmentBuilder();
        tcab.setAttrStr1("FooFoo");
        tcab.setAttrStr2("BarBar");
        TllComplexAugment tca = tcab.build();

        nodeBuilder.addAugmentation(TreeComplexUsesAugment.class, tca);
        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(TLL_INSTANCE_ID_BA, nodeBuilder.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();


        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);
        assertTrue(potential.getCreatedOperationalData().containsKey(TCA_AUGMENTATION_PATH));
        lastReceivedChangeEvent = SettableFuture.create();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TllComplexAugment readedAugmentation = (TllComplexAugment) baDataService.readOperationalData(
                TLL_INSTANCE_ID_BA.augmentation(TllComplexAugment.class));
        assertNotNull(readedAugmentation);

        assertEquals(tca.getAttrStr1(), readedAugmentation.getAttrStr1());

        testPutNodeConnectorWithAugmentation();
        lastReceivedChangeEvent = SettableFuture.create();
        testTllRemove();

        assertTrue(lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS).getRemovedOperationalData().contains(TCA_AUGMENTATION_PATH));
    }

    private void testPutNodeConnectorWithAugmentation() throws Exception {
        NestedListKey ncKey = new NestedListKey("test:0:0");
        InstanceIdentifier<NestedList> ncPath = TLL_INSTANCE_ID_BA
                .child(NestedList.class, ncKey);
        InstanceIdentifier<NestedListSimpleAugment> ncAugmentPath = ncPath
                .augmentation(NestedListSimpleAugment.class);

        NestedListBuilder nc = new NestedListBuilder();
        nc.setKey(ncKey);

        NestedListSimpleAugmentBuilder fncb = new NestedListSimpleAugmentBuilder();
        fncb.setType("Baz");
        nc.addAugmentation(NestedListSimpleAugment.class, fncb.build());

        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(ncPath, nc.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        NestedListSimpleAugment readedAugmentation = (NestedListSimpleAugment) baDataService
                .readOperationalData(ncAugmentPath);
        assertNotNull(readedAugmentation);
        assertEquals(fncb.getType(), readedAugmentation.getType());
    }

    private void testTllRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(TLL_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList tll = (TopLevelList) baDataService.readOperationalData(TLL_INSTANCE_ID_BA);
        assertNull(tll);
    }

//    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier tllId) {
//        CompositeNode tll = biDataService.readOperationalData(tllId);
//        assertNotNull(tll);
//    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        lastReceivedChangeEvent.set(change);
    }

}
