/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertFalse;

import com.google.common.util.concurrent.SettableFuture;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.List11SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.List11SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeleteNestedAugmentationListenParentTest extends AbstractDataServiceTest {

    private static final TopLevelListKey FOO_KEY = new TopLevelListKey("foo");

    private static final List1Key LIST1_KEY = new List1Key("one");

    private static final List11Key LIST11_KEY = new List11Key(100);

    private static final InstanceIdentifier<TllComplexAugment> TLL_COMPLEX_AUGMENT_PATH = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class,FOO_KEY)
            .augmentation(TllComplexAugment.class)
            .build();

    private static final InstanceIdentifier<List11> LIST11_PATH = TLL_COMPLEX_AUGMENT_PATH.builder()
            .child(List1.class,LIST1_KEY)
            .child(List11.class,LIST11_KEY)
            .build();


    @Test
    public void deleteChildListenParent() throws InterruptedException, ExecutionException, TimeoutException {
        DataBroker dataBroker = testContext.getDataBroker();
        final WriteTransaction initTx = dataBroker.newWriteOnlyTransaction();

        initTx.put(LogicalDatastoreType.OPERATIONAL, LIST11_PATH, createList11(), true);
        initTx.submit().get(5, TimeUnit.SECONDS);

        final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> event = SettableFuture.create();
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, LIST11_PATH,
            change -> event.set(change), DataChangeScope.SUBTREE);

        final WriteTransaction deleteTx = dataBroker.newWriteOnlyTransaction();
        deleteTx.delete(LogicalDatastoreType.OPERATIONAL, LIST11_PATH.augmentation(List11SimpleAugment.class));
        deleteTx.submit().get(5, TimeUnit.SECONDS);

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> receivedEvent = event.get();
        assertFalse(receivedEvent.getRemovedPaths().contains(TLL_COMPLEX_AUGMENT_PATH));
    }

    private List11 createList11() {
        List11Builder builder = new List11Builder()
            .setKey(LIST11_KEY)
            .addAugmentation(List11SimpleAugment.class,new List11SimpleAugmentBuilder()
                    .setAttrStr2("bad").build())
            .setAttrStr("good");
        return builder.build();
    }

}
