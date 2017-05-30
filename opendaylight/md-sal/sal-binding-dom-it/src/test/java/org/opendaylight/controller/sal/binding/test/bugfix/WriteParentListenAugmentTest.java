/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class WriteParentListenAugmentTest extends AbstractDataServiceTest {

    private static final String TLL_NAME = "foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);
    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, TLL_KEY).toInstance();

    private static final InstanceIdentifier<TreeComplexUsesAugment> AUGMENT_WILDCARDED_PATH = InstanceIdentifier
            .builder(Top.class).child(TopLevelList.class).augmentation(TreeComplexUsesAugment.class).toInstance();

    private static final InstanceIdentifier<TreeComplexUsesAugment> AUGMENT_TLL_PATH = InstanceIdentifier
            .builder(Top.class).child(TopLevelList.class, TLL_KEY).augmentation(TreeComplexUsesAugment.class).toInstance();

    @Test
    public void writeNodeListenAugment() throws Exception {

        final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> event = SettableFuture.create();
        DataBroker dataBroker = testContext.getDataBroker();
        ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> dclRegistration =
                dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, AUGMENT_WILDCARDED_PATH,
                    change -> event.set(change), DataChangeScope.SUBTREE);

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        TopLevelList tll = new TopLevelListBuilder() //
                .setKey(TLL_KEY) //
                .addAugmentation(TreeComplexUsesAugment.class, treeComplexUsesAugment("one")).build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, TLL_INSTANCE_ID_BA, tll, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> receivedEvent = event.get(1000, TimeUnit.MILLISECONDS);
        assertTrue(receivedEvent.getCreatedData().containsKey(AUGMENT_TLL_PATH));

        dclRegistration.close();

        final WriteTransaction transaction2 = dataBroker.newWriteOnlyTransaction();
        transaction2.put(LogicalDatastoreType.OPERATIONAL, AUGMENT_TLL_PATH, treeComplexUsesAugment("two"));
        transaction2.submit().get(5, TimeUnit.SECONDS);

        TreeComplexUsesAugment readedAug = dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.OPERATIONAL, AUGMENT_TLL_PATH).checkedGet(5, TimeUnit.SECONDS).get();
        assertEquals("two", readedAug.getContainerWithUses().getLeafFromGrouping());
    }

    private TreeComplexUsesAugment treeComplexUsesAugment(final String value) {
        return new TreeComplexUsesAugmentBuilder() //
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping(value).build()) //
                .build();
    }
}
