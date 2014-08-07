/*
 *
 *  * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.A;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.ABuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.ListOfB;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.ListOfBBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.ListOfBKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.list.of.b.B;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.list.of.b.BBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.list.of.b.b.CContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bug1313.test.module.rev140806.a.list.of.b.b.CContainerBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;

public class Bug1313ListenerTest extends AbstractDataChangeListenerTest {
    private static final Logger LOG = LoggerFactory.getLogger(Bug1313ListenerTest.class);

    public static final String LIST_OF_B_ITEM_NAME = "listOfBItem";

    public static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:bug1313-test-module",
            "2014-08-06", "bug1313-test-module");
    public static final QName A_QNAME = QName.create(TEST_QNAME, "a");
    public static final QName LIST_OF_B_QNAME = QName.create(TEST_QNAME, "list-of-b");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName B_QNAME = QName.create(TEST_QNAME, "b");
    public static final QName B1_QNAME = QName.create(TEST_QNAME, "b1");
    public static final QName B2_QNAME = QName.create(TEST_QNAME, "b2");
    public static final QName C_CONTAINER_QNAME = QName.create(TEST_QNAME, "c-container");
    public static final QName C1_QNAME = QName.create(TEST_QNAME, "c1");

    public static final YangInstanceIdentifier A_PATH = YangInstanceIdentifier.of(A_QNAME);
    public static final YangInstanceIdentifier B_PATH = YangInstanceIdentifier.builder(A_PATH).node(LIST_OF_B_QNAME).
            nodeWithKey(LIST_OF_B_QNAME, NAME_QNAME, LIST_OF_B_ITEM_NAME).node(B_QNAME).build();

    public static final InstanceIdentifier<A> BA_A_PATH = InstanceIdentifier.create(A.class);
    public static final InstanceIdentifier<B> BA_B_PATH = BA_A_PATH
            .child(ListOfB.class, new ListOfBKey(LIST_OF_B_ITEM_NAME)).child(B.class);
    public static final InstanceIdentifier<CContainer> BA_C_CONTAINER_PATH = BA_B_PATH.child(CContainer.class);

    @Test
    public void createAReplaceBUpdateB2() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH, createA(LIST_OF_B_ITEM_NAME, 5, "five"));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(A.class)
                .child(ListOfB.class, new ListOfBKey(LIST_OF_B_ITEM_NAME)).child(B.class), createB(5, "six"));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getRemovedPaths());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getOriginalData(), B_PATH.node(B2_QNAME));
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getUpdatedData(), B_PATH.node(B2_QNAME));
        assertEmpty(domEvent.getCreatedData());
        assertEmpty(domEvent.getRemovedPaths());
    }

    @Test
    public void createAReplaceBDeleteB2() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH, createA(LIST_OF_B_ITEM_NAME, 5, "five"));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(A.class)
                .child(ListOfB.class, new ListOfBKey(LIST_OF_B_ITEM_NAME)).child(B.class), createB(5, null));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getRemovedPaths());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getOriginalData(), B_PATH.node(B2_QNAME));
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getRemovedPaths(), B_PATH.node(B2_QNAME));
        assertEmpty(domEvent.getCreatedData());
    }

    @Test
    public void createAReplaceBCreateB2() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH, createA(LIST_OF_B_ITEM_NAME, 5, null));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(A.class)
                .child(ListOfB.class, new ListOfBKey(LIST_OF_B_ITEM_NAME)).child(B.class), createB(5, "five"));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getRemovedPaths());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getCreatedData(), B_PATH.node(B2_QNAME));
        assertEmpty(domEvent.getRemovedPaths());
    }

    @Test
    public void createAReplaceBWithContainerCModifyC() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH,
                createA(LIST_OF_B_ITEM_NAME, 5, "five", "c old"));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_B_PATH, createB(5, "five", "c new"));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getOriginalData(), BA_C_CONTAINER_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertContains(event.getUpdatedData(), BA_C_CONTAINER_PATH);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getRemovedPaths());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getOriginalData(), B_PATH.node(C_CONTAINER_QNAME));
        assertContains(domEvent.getOriginalData(), B_PATH.node(C_CONTAINER_QNAME).node(C1_QNAME));
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getUpdatedData(), B_PATH.node(C_CONTAINER_QNAME));
        assertContains(domEvent.getUpdatedData(), B_PATH.node(C_CONTAINER_QNAME).node(C1_QNAME));
        assertEmpty(domEvent.getCreatedData());
        assertEmpty(domEvent.getRemovedPaths());
    }

    @Test
    public void createARemoveBWithContainerCDeleteC() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH,
                createA(LIST_OF_B_ITEM_NAME, 5, "five", "c old"));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_B_PATH, createB(5, "five"));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getOriginalData(), BA_C_CONTAINER_PATH);
        assertContains(event.getRemovedPaths(), BA_C_CONTAINER_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertEmpty(event.getCreatedData());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getOriginalData(), B_PATH.node(C_CONTAINER_QNAME));
        assertContains(domEvent.getOriginalData(), B_PATH.node(C_CONTAINER_QNAME).node(C1_QNAME));
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getRemovedPaths(), B_PATH.node(C_CONTAINER_QNAME));
        assertContains(domEvent.getRemovedPaths(), B_PATH.node(C_CONTAINER_QNAME).node(C1_QNAME));
        assertEmpty(domEvent.getCreatedData());
    }

    @Test
    public void createARemoveBWithContainerCCreateC() throws ExecutionException, InterruptedException {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_A_PATH,
                createA(LIST_OF_B_ITEM_NAME, 5, "five"));
        writeTransaction.submit().get();
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, BA_B_PATH,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, BA_B_PATH, createB(5, "five", "c new"));
        writeTransaction.submit().get();

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getOriginalData(), BA_B_PATH);
        assertContains(event.getCreatedData(), BA_C_CONTAINER_PATH);
        assertContains(event.getUpdatedData(), BA_B_PATH);
        assertEmpty(event.getRemovedPaths());

        Class<? extends AsyncDataChangeEvent> actualChangeEventClass = event.getClass();
        Field domEventField = null;
        try {
            domEventField = actualChangeEventClass.getDeclaredField("domEvent");
        } catch (NoSuchFieldException e) {
            fail("Class doesn't contain domEvent field: " + e.getMessage());
        }
        assertNotNull("Error occurred while trying to get domEvent field from Class", domEventField);
        domEventField.setAccessible(true);
        DOMImmutableDataChangeEvent domEvent = null;
        try {
            domEvent = (DOMImmutableDataChangeEvent) domEventField.get(event);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occured while trying to get DOMImmutableDataChangeEvent: " + e.getMessage());
        }

        assertContains(domEvent.getOriginalData(), B_PATH);
        assertContains(domEvent.getUpdatedData(), B_PATH);
        assertContains(domEvent.getCreatedData(), B_PATH.node(C_CONTAINER_QNAME));
        assertContains(domEvent.getCreatedData(), B_PATH.node(C_CONTAINER_QNAME).node(C1_QNAME));
        assertEmpty(domEvent.getRemovedPaths());
    }

    private A createA(String listOfBName ,int b1, String b2) {
        ABuilder aBuilder = new ABuilder();
        List<ListOfB> listOfB = new ArrayList<>();
        listOfB.add(new ListOfBBuilder().setKey(new ListOfBKey(listOfBName)).setB(createB(b1, b2)).build());
        aBuilder.setListOfB(listOfB);
        return aBuilder.build();
    }

    private A createA(String listOfBName ,int b1, String b2, String c1) {
        ABuilder aBuilder = new ABuilder();
        List<ListOfB> listOfB = new ArrayList<>();
        listOfB.add(new ListOfBBuilder().setKey(new ListOfBKey(listOfBName)).setB(createB(b1, b2, c1)).build());
        aBuilder.setListOfB(listOfB);
        return aBuilder.build();
    }

    private B createB(int b1, String b2) {
        BBuilder bBuilder = new BBuilder();
        bBuilder.setB1(b1);
        bBuilder.setB2(b2);
        return bBuilder.build();
    }

    private B createB(int b1, String b2, String c1) {
        BBuilder bBuilder = new BBuilder();
        bBuilder.setB1(b1);
        bBuilder.setB2(b2);
        bBuilder.setCContainer(new CContainerBuilder().setC1(c1).build());
        return bBuilder.build();
    }
}
