/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Regression test suite for Bug 1125 - Can't detect switch disconnection
 * https://bugs.opendaylight.org/show_bug.cgi?id=1125
 */
public class Bug1125RegressionTest extends AbstractDataChangeListenerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier
            .create(Top.class);
    private static final InstanceIdentifier<TopLevelList> TOP_FOO_PATH = TOP_PATH
            .child(TopLevelList.class, TOP_FOO_KEY);

    private static final InstanceIdentifier<TreeComplexUsesAugment> FOO_AUGMENT_PATH = TOP_FOO_PATH
            .augmentation(TreeComplexUsesAugment.class);

    private static final InstanceIdentifier<TreeComplexUsesAugment> WILDCARDED_AUGMENT_PATH = TOP_PATH
            .child(TopLevelList.class).augmentation(
                    TreeComplexUsesAugment.class);

    private void writeInitialState() {
        WriteTransaction initialTx = getDataBroker().newWriteOnlyTransaction();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH,
                new TopBuilder().build());
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(
                        new ContainerWithUsesBuilder().setLeafFromGrouping(
                                "foo").build()).build();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, path(TOP_FOO_KEY),
                topLevelList(TOP_FOO_KEY, fooAugment));
        assertCommit(initialTx.submit());
    }

    private void delete(final InstanceIdentifier<?> path) {
        WriteTransaction tx = getDataBroker().newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
        assertCommit(tx.submit());
    }

    private void verifyRemoved(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getUpdatedData());
        assertContains(event.getRemovedPaths(), FOO_AUGMENT_PATH);
    }

    private void deleteAndListenAugment(final DataChangeScope scope,
            final InstanceIdentifier<?> path) {
        writeInitialState();
        TestListener listener = createListener(
                LogicalDatastoreType.OPERATIONAL, WILDCARDED_AUGMENT_PATH,
                scope);
        delete(path);
        verifyRemoved(listener.event());
    }

    @Test
    public void deleteAndListenAugment() {

        deleteAndListenAugment(DataChangeScope.ONE, TOP_PATH);

        deleteAndListenAugment(DataChangeScope.BASE, TOP_PATH);

        deleteAndListenAugment(DataChangeScope.SUBTREE, TOP_PATH);

        deleteAndListenAugment(DataChangeScope.BASE, TOP_FOO_PATH);

        deleteAndListenAugment(DataChangeScope.ONE, TOP_FOO_PATH);

        deleteAndListenAugment(DataChangeScope.SUBTREE, TOP_FOO_PATH);

        deleteAndListenAugment(DataChangeScope.BASE, FOO_AUGMENT_PATH);

        deleteAndListenAugment(DataChangeScope.ONE, FOO_AUGMENT_PATH);

        deleteAndListenAugment(DataChangeScope.SUBTREE, FOO_AUGMENT_PATH);
    }
}
