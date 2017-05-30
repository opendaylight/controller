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

import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * FIXME: THis test should be moved to compat test-suite
 */
public class WildcardedDataChangeListenerTest extends AbstractDataServiceTest {

    private static final TopLevelListKey TOP_LEVEL_LIST_0_KEY = new TopLevelListKey("test:0");
    private static final TopLevelListKey TOP_LEVEL_LIST_1_KEY = new TopLevelListKey("test:1");

    protected static final InstanceIdentifier<ListViaUses> DEEP_WILDCARDED_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class) //
            .augmentation(TreeComplexUsesAugment.class) //
            .child(ListViaUses.class) //
            .build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> NODE_0_TCU_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class, TOP_LEVEL_LIST_0_KEY) //
            .augmentation(TreeComplexUsesAugment.class) //
            .build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> NODE_1_TCU_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class, TOP_LEVEL_LIST_1_KEY) //
            .augmentation(TreeComplexUsesAugment.class) //
            .build();


    private static final ListViaUsesKey LIST_VIA_USES_KEY = new ListViaUsesKey("test");

    private static final InstanceIdentifier<ListViaUses> NODE_0_LVU_PATH = NODE_0_TCU_PATH.child(ListViaUses.class, LIST_VIA_USES_KEY);

    private static final InstanceIdentifier<ListViaUses> NODE_1_LVU_PATH = NODE_1_TCU_PATH.child(ListViaUses.class, LIST_VIA_USES_KEY);

    private static final InstanceIdentifier<ContainerWithUses> NODE_0_CWU_PATH =
            NODE_0_TCU_PATH.child(ContainerWithUses.class);

    private static final ContainerWithUses CWU= new ContainerWithUsesBuilder()//
            .setLeafFromGrouping("some container value") //
            .build();

    private static final ListViaUses LVU = new ListViaUsesBuilder() //
            .setKey(LIST_VIA_USES_KEY) //
            .setName("john")
            .build();

    @Test
    public void testSeparateWrites() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = testContext.getDataBroker();

        final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture =
                SettableFuture.create();
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataChangeEvent -> eventFuture.set(dataChangeEvent), DataChangeScope.SUBTREE);

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, NODE_0_CWU_PATH, CWU, true);
        transaction.put(LogicalDatastoreType.OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        transaction.put(LogicalDatastoreType.OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = eventFuture.get(1000, TimeUnit.MILLISECONDS);

        validateEvent(event);
    }

    @Test
    public void testWriteByReplace() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = testContext.getDataBroker();

        final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture =
                SettableFuture.create();
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataChangeEvent -> eventFuture.set(dataChangeEvent), DataChangeScope.SUBTREE);

        final WriteTransaction cwuTx = dataBroker.newWriteOnlyTransaction();
        cwuTx.put(LogicalDatastoreType.OPERATIONAL, NODE_0_CWU_PATH, CWU, true);
        cwuTx.submit().get(5, TimeUnit.SECONDS);

        assertFalse(eventFuture.isDone());

        final WriteTransaction lvuTx = dataBroker.newWriteOnlyTransaction();

        TreeComplexUsesAugment tcua = new TreeComplexUsesAugmentBuilder()
                .setListViaUses(Collections.singletonList(LVU)).build();

        lvuTx.put(LogicalDatastoreType.OPERATIONAL, NODE_0_TCU_PATH, tcua, true);
        lvuTx.put(LogicalDatastoreType.OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        lvuTx.submit().get(5, TimeUnit.SECONDS);

        validateEvent(eventFuture.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNoChangeOnReplaceWithSameValue() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = testContext.getDataBroker();

        // We wrote initial state NODE_0_FLOW
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        // We registered DataChangeListener
        final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture =
                SettableFuture.create();
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataChangeEvent -> eventFuture.set(dataChangeEvent), DataChangeScope.SUBTREE);
        assertFalse(eventFuture.isDone());

        final WriteTransaction secondTx = dataBroker.newWriteOnlyTransaction();
        secondTx.put(LogicalDatastoreType.OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        secondTx.put(LogicalDatastoreType.OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        secondTx.submit().get(5, TimeUnit.SECONDS);

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = eventFuture.get(1000, TimeUnit.MILLISECONDS);
        assertNotNull(event);
        // Data change should contains NODE_1 Flow - which was added
        assertTrue(event.getCreatedData().containsKey(NODE_1_LVU_PATH));
        // Data change must not containe NODE_0 Flow which was replaced with same value.
        assertFalse(event.getUpdatedData().containsKey(NODE_0_LVU_PATH));
    }

    private static void validateEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        assertNotNull(event);
        assertTrue(event.getCreatedData().containsKey(NODE_1_LVU_PATH));
        assertTrue(event.getCreatedData().containsKey(NODE_0_LVU_PATH));
        assertFalse(event.getCreatedData().containsKey(NODE_0_CWU_PATH));
    }

}
