/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List12;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List12Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List12Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;

/*
 * FIXME: THis test should be moved to compat test-suite and rewriten
 * to use sal-test-model
 */
@SuppressWarnings("deprecation")
public class WildcardedDataChangeListenerTest extends AbstractDataServiceTest {

    private static final TopLevelListKey FOO_KEY = new TopLevelListKey("foo");
    private static final TopLevelListKey BAR_KEY = new TopLevelListKey("bar");

    public static final InstanceIdentifier<List11> DEEP_WILDCARDED_PATH = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class) //
            .augmentation(TllComplexAugment.class) //
            .child(List1.class) //
            .child(List11.class)
            .build();

    private static final List1Key LIST1_KEY = new List1Key("one");
    private static final List12Key LIST12_KEY = new List12Key(0);

    private static final InstanceIdentifier<List1> FOO_LIST1_PATH = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class, FOO_KEY) //
            .augmentation(TllComplexAugment.class) //
            .child(List1.class) //
            .build();

    private static final InstanceIdentifier<List1> BAR_LIST1_PATH = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class, BAR_KEY) //
            .augmentation(TllComplexAugment.class) //
            .child(List1.class) //
            .build();

    private static final List11Key LIST11_KEY = new List11Key(0);

    private static final InstanceIdentifier<List11> FOO_LIST11_PATH =
            FOO_LIST1_PATH.child(List11.class, LIST11_KEY);

    private static final InstanceIdentifier<List11> BAR_LIST11_PATH =
            BAR_LIST1_PATH.child(List11.class, LIST11_KEY);

    private static final InstanceIdentifier<List12> FOO_LIST12_PATH =
            FOO_LIST1_PATH.child(List12.class, LIST12_KEY);

    private static final List12 LIST12 = new List12Builder()//
            .setKey(LIST12_KEY) //
            .setAttrStr("Foo") //
            .build();

    private static final List11 LIST11 = new List11Builder() //
            .setKey(LIST11_KEY) //
            .setAttrStr("nested") //
            .build();

    @Test
    public void testSepareteWrites() throws InterruptedException, TimeoutException, ExecutionException {

        DataProviderService dataBroker = testContext.getBindingDataBroker();

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture = SettableFuture.create();
        dataBroker.registerDataChangeListener(DEEP_WILDCARDED_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
                eventFuture.set(dataChangeEvent);
            }
        });

        DataModificationTransaction transaction = dataBroker.beginTransaction();
        transaction.putOperationalData(FOO_LIST11_PATH, LIST11);
        transaction.putOperationalData(BAR_LIST11_PATH, LIST11);
        transaction.commit().get();

        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = eventFuture.get(1000, TimeUnit.MILLISECONDS);

        validateEvent(event);
    }

    @Test
    public void testWriteByReplace() throws InterruptedException, TimeoutException, ExecutionException {

        DataProviderService dataBroker = testContext.getBindingDataBroker();

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture = SettableFuture.create();
        dataBroker.registerDataChangeListener(DEEP_WILDCARDED_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
                eventFuture.set(dataChangeEvent);
            }
        });

        DataModificationTransaction tableTx = dataBroker.beginTransaction();
        tableTx.putOperationalData(FOO_LIST12_PATH, LIST12);
        tableTx.commit().get();

        assertFalse(eventFuture.isDone());

        DataModificationTransaction flowTx = dataBroker.beginTransaction();

        List1 listViaUses = new List1Builder() //
                .setKey(LIST1_KEY) //
                .setList11(Collections.singletonList(LIST11)) //
                .build();

        flowTx.putOperationalData(FOO_LIST1_PATH, listViaUses);
        flowTx.putOperationalData(BAR_LIST11_PATH, LIST11);
        flowTx.commit().get();

        validateEvent(eventFuture.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNoChangeOnReplaceWithSameValue() throws InterruptedException, TimeoutException, ExecutionException {

        DataProviderService dataBroker = testContext.getBindingDataBroker();

        // We wrote initial state NODE_0_FLOW
        DataModificationTransaction transaction = dataBroker.beginTransaction();
        transaction.putOperationalData(FOO_LIST11_PATH, LIST11);
        transaction.commit().get();

        // We registered DataChangeListener
        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture = SettableFuture.create();
        dataBroker.registerDataChangeListener(DEEP_WILDCARDED_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
                eventFuture.set(dataChangeEvent);
            }
        });
        assertFalse(eventFuture.isDone());

        DataModificationTransaction secondTx = dataBroker.beginTransaction();
        secondTx.putOperationalData(FOO_LIST11_PATH, LIST11);
        secondTx.putOperationalData(BAR_LIST11_PATH, LIST11);
        secondTx.commit().get();

        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = (eventFuture.get(1000, TimeUnit.MILLISECONDS));
        assertNotNull(event);
        // Data change should contains NODE_1 Flow - which was added
        assertTrue(event.getCreatedOperationalData().containsKey(BAR_LIST11_PATH));
        // Data change must not containe NODE_0 Flow which was replaced with same value.
        assertFalse(event.getUpdatedOperationalData().containsKey(FOO_LIST11_PATH));
    }

    private static void validateEvent(final DataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        assertNotNull(event);
        assertTrue(event.getCreatedOperationalData().containsKey(BAR_LIST11_PATH));
        assertTrue(event.getCreatedOperationalData().containsKey(FOO_LIST11_PATH));
        assertFalse(event.getCreatedOperationalData().containsKey(FOO_LIST12_PATH));
    }

}
