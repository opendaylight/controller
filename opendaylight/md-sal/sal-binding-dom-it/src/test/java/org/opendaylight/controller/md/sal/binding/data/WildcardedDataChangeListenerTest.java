/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertFalse;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * FIXME: THis test should be moved to compat test-suite
 */
public class WildcardedDataChangeListenerTest extends AbstractDataTreeChangeListenerTest {

    private static final TopLevelListKey TOP_LEVEL_LIST_0_KEY = new TopLevelListKey("test:0");
    private static final TopLevelListKey TOP_LEVEL_LIST_1_KEY = new TopLevelListKey("test:1");

    protected static final InstanceIdentifier<ListViaUses> DEEP_WILDCARDED_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class)
            .augmentation(TreeComplexUsesAugment.class)
            .child(ListViaUses.class)
            .build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> NODE_0_TCU_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class, TOP_LEVEL_LIST_0_KEY)
            .augmentation(TreeComplexUsesAugment.class)
            .build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> NODE_1_TCU_PATH = InstanceIdentifier
            .builder(Top.class)
            .child(TopLevelList.class, TOP_LEVEL_LIST_1_KEY)
            .augmentation(TreeComplexUsesAugment.class)
            .build();


    private static final ListViaUsesKey LIST_VIA_USES_KEY = new ListViaUsesKey("test");

    private static final InstanceIdentifier<ListViaUses> NODE_0_LVU_PATH = NODE_0_TCU_PATH.child(ListViaUses.class,
        LIST_VIA_USES_KEY);

    private static final InstanceIdentifier<ListViaUses> NODE_1_LVU_PATH = NODE_1_TCU_PATH.child(ListViaUses.class,
        LIST_VIA_USES_KEY);

    private static final InstanceIdentifier<ContainerWithUses> NODE_0_CWU_PATH =
            NODE_0_TCU_PATH.child(ContainerWithUses.class);

    private static final ContainerWithUses CWU = new ContainerWithUsesBuilder()
            .setLeafFromGrouping("some container value").build();

    private static final ListViaUses LVU = new ListViaUsesBuilder()
            .withKey(LIST_VIA_USES_KEY).setName("john").build();

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Top.class),
                BindingReflections.getModuleInfo(TreeComplexUsesAugment.class));
    }

    @Test
    public void testSeparateWrites() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = getDataBroker();

        final TestListener<ListViaUses> listener = createListener(OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataTreeModification -> NODE_0_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()),
            dataTreeModification -> NODE_1_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()));

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(OPERATIONAL, NODE_0_CWU_PATH, CWU, true);
        transaction.put(OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        transaction.put(OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        listener.verify();
    }

    @Test
    public void testWriteByReplace() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = getDataBroker();

        final TestListener<ListViaUses> listener = createListener(OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataTreeModification -> NODE_0_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()),
            dataTreeModification -> NODE_1_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()));

        final WriteTransaction cwuTx = dataBroker.newWriteOnlyTransaction();
        cwuTx.put(OPERATIONAL, NODE_0_CWU_PATH, CWU, true);
        cwuTx.submit().get(5, TimeUnit.SECONDS);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertFalse(listener.hasChanges());

        final WriteTransaction lvuTx = dataBroker.newWriteOnlyTransaction();

        TreeComplexUsesAugment tcua = new TreeComplexUsesAugmentBuilder()
                .setListViaUses(Collections.singletonList(LVU)).build();

        lvuTx.put(OPERATIONAL, NODE_0_TCU_PATH, tcua, true);
        lvuTx.put(OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        lvuTx.submit().get(5, TimeUnit.SECONDS);

        listener.verify();
    }

    @Test
    public void testChangeOnReplaceWithSameValue() throws InterruptedException, TimeoutException, ExecutionException {

        DataBroker dataBroker = getDataBroker();

        // Write initial state NODE_0_FLOW
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        final TestListener<ListViaUses> listener = createListener(OPERATIONAL, DEEP_WILDCARDED_PATH,
            dataTreeModification -> NODE_1_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()),
            dataTreeModification -> NODE_0_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()),
            dataTreeModification -> NODE_1_LVU_PATH.equals(dataTreeModification.getRootPath().getRootIdentifier()));

        final WriteTransaction secondTx = dataBroker.newWriteOnlyTransaction();
        secondTx.put(OPERATIONAL, NODE_0_LVU_PATH, LVU, true);
        secondTx.put(OPERATIONAL, NODE_1_LVU_PATH, LVU, true);
        secondTx.submit().get(5, TimeUnit.SECONDS);

        listener.verify();
    }
}
