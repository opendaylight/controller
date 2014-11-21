/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.CustomEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.Cont2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.Cont2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.cont2.Contlist1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.cont2.Contlist1Builder;
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
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class DOMCodecBug03Test extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName TOP_LEVEL_LIST_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final String TOP_LEVEL_LIST_NAME = "tll:foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TOP_LEVEL_LIST_NAME);

    private static final Map<QName, Object> TLL_KEY_BI = Collections.<QName, Object> singletonMap(TOP_LEVEL_LIST_NAME_QNAME,
            TOP_LEVEL_LIST_NAME);

    private static final InstanceIdentifier<Top> TOP_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .toInstance();


    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = TOP_INSTANCE_ID_BA.child(TopLevelList.class, TLL_KEY);


    private static final InstanceIdentifier<Cont2> CONT2_INSTANCE_ID_BA = //
            TOP_INSTANCE_ID_BA.builder() //
            .child(TopLevelList.class, TLL_KEY) //
            .augmentation(TllComplexAugment.class) //
            .child(Cont2.class)
            .toInstance();


    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier TLL_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
            .toInstance();
    private static final QName CONT2_QNAME = QName.create(TllComplexAugment.QNAME, Cont2.QNAME.getLocalName());


    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier CONT2_INSTANCE_ID_BI = //
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
                    .node(Top.QNAME) //
                    .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
                    .node(CONT2_QNAME) //
                    .toInstance();

    private final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> receivedChangeEvent = SettableFuture.create();



    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test
    public void testAugmentSerialization() throws Exception {


        baDataService.registerDataChangeListener(TOP_INSTANCE_ID_BA, this);

        TopLevelListBuilder tllBuilder = new TopLevelListBuilder();
        tllBuilder.setKey(TLL_KEY);
        DataModificationTransaction transaction = baDataService.beginTransaction();


        TllComplexAugmentBuilder tllcab = new TllComplexAugmentBuilder();
        tllcab.setAttrStr1("Hardware Foo");
        tllcab.setAttrStr2("Manufacturer Foo");
        tllcab.setAttrStr3("Serial Foo");
        tllcab.setAttrStr4("Description Foo");
        TllComplexAugment tlca = tllcab.build();
        tllBuilder.addAugmentation(TllComplexAugment.class, tlca);
        TopLevelList original = tllBuilder.build();
        transaction.putOperationalData(TLL_INSTANCE_ID_BA, original);

        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = receivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);

        verifyTll((Top) potential.getUpdatedOperationalSubtree(),original);
        assertBindingIndependentVersion(TLL_INSTANCE_ID_BI);
        Top top = checkForTop();
        verifyTll(top,original);

        testAddingNodeConnector();
        testTllRemove();

    }

    @Test
    public void testAugmentNestedSerialization() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();

        Cont2Builder cont2b = new Cont2Builder();
        Contlist1Builder cl1b = new Contlist1Builder();
        cl1b.setAttrStr("foo-action");
        cl1b.setAttrEnum(CustomEnum.Type1);
        List<Contlist1> contlists = Collections.singletonList(cl1b.build());
        cont2b.setContlist1(contlists);

        transaction.putOperationalData(CONT2_INSTANCE_ID_BA, cont2b.build());
        RpcResult<TransactionStatus> putResult = transaction.commit().get();
        assertNotNull(putResult);
        assertEquals(TransactionStatus.COMMITED, putResult.getResult());
        Cont2 readedTable = (Cont2) baDataService.readOperationalData(CONT2_INSTANCE_ID_BA);
        assertNotNull(readedTable);

        CompositeNode biSupportedActions = biDataService.readOperationalData(CONT2_INSTANCE_ID_BI);
        assertNotNull(biSupportedActions);

    }

    private void testAddingNodeConnector() throws Exception {
        NestedListKey nlKey = new NestedListKey("test:0:0");
        InstanceIdentifier<NestedList> ncInstanceId = TLL_INSTANCE_ID_BA.child(NestedList.class, nlKey);
        NestedListBuilder nlBuilder = new NestedListBuilder();
        nlBuilder.setKey(nlKey);
        NestedList nestedList = nlBuilder.build();
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putOperationalData(ncInstanceId, nestedList);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        TopLevelList tll = (TopLevelList) baDataService.readOperationalData(TLL_INSTANCE_ID_BA);
        assertNotNull(tll);
        assertNotNull(tll.getNestedList());
        assertFalse(tll.getNestedList().isEmpty());
        NestedList readedNl = tll.getNestedList().get(0);
        assertNotNull(readedNl);
    }

    private void testTllRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(TLL_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList tll = (TopLevelList) baDataService.readOperationalData(TLL_INSTANCE_ID_BA);
        assertNull(tll);
    }

    private void verifyTll(final Top top,final TopLevelList original) {
        assertNotNull(top);
        assertNotNull(top.getTopLevelList());
        assertEquals(1, top.getTopLevelList().size());
        TopLevelList readedNode = top.getTopLevelList().get(0);
        assertEquals(original.getName(), readedNode.getName());
        assertEquals(original.getKey(), readedNode.getKey());

        TllComplexAugment fnu = original.getAugmentation(TllComplexAugment.class);
        TllComplexAugment readedAugment = readedNode.getAugmentation(TllComplexAugment.class);
        assertNotNull(fnu);
        assertEquals(fnu.getAttrStr2(), readedAugment.getAttrStr2());
        assertEquals(fnu.getAttrStr3(), readedAugment.getAttrStr3());

    }

    private void assertBindingIndependentVersion(
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Top checkForTop() {
        return (Top) baDataService.readOperationalData(TOP_INSTANCE_ID_BA);
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent.set(change);
    }

}
