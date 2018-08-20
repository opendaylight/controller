/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.USES_ONE_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.USES_TWO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * This testsuite tries to replicate bug 1333 and tests regresion of it
 * using test-model with similar construction as one reported.
 *
 * <p>
 * See  https://bugs.opendaylight.org/show_bug.cgi?id=1333 for Bug Description
 */
public class Bug1333DataChangeListenerTest extends AbstractDataTreeChangeListenerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    private static final InstanceIdentifier<TreeComplexUsesAugment> AUGMENT_WILDCARD =
            TOP_PATH.child(TopLevelList.class).augmentation(TreeComplexUsesAugment.class);

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Top.class),
                BindingReflections.getModuleInfo(TreeComplexUsesAugment.class));
    }

    private Top topWithListItem() {
        return top(topLevelList(TOP_FOO_KEY, complexUsesAugment(USES_ONE_KEY, USES_TWO_KEY)));
    }

    public Top writeTopWithListItem(final LogicalDatastoreType store) {
        ReadWriteTransaction tx = getDataBroker().newReadWriteTransaction();
        Top topItem = topWithListItem();
        tx.put(store, TOP_PATH, topItem);
        assertCommit(tx.submit());
        return topItem;
    }

    public void deleteItem(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        ReadWriteTransaction tx = getDataBroker().newReadWriteTransaction();
        tx.delete(store, path);
        assertCommit(tx.submit());
    }

    @Test
    public void writeTopWithListItemAugmentedListenTopSubtree() {
        TestListener<Top> listener = createListener(CONFIGURATION, TOP_PATH, added(TOP_PATH, topWithListItem()));

        writeTopWithListItem(CONFIGURATION);

        listener.verify();
    }

    @Test
    public void writeTopWithListItemAugmentedListenAugmentSubtreeWildcarded() {
        TestListener<TreeComplexUsesAugment> listener = createListener(CONFIGURATION, AUGMENT_WILDCARD,
                added(path(TOP_FOO_KEY, TreeComplexUsesAugment.class), complexUsesAugment(USES_ONE_KEY, USES_TWO_KEY)));

        writeTopWithListItem(CONFIGURATION);

        listener.verify();
    }

    @Test
    public void deleteAugmentChildListenTopSubtree() {
        Top top = writeTopWithListItem(CONFIGURATION);

        TestListener<Top> listener = createListener(CONFIGURATION, TOP_PATH, added(TOP_PATH, top),
                subtreeModified(TOP_PATH, top, top(topLevelList(TOP_FOO_KEY, complexUsesAugment(USES_TWO_KEY)))));

        InstanceIdentifier<ListViaUses> deletePath = path(TOP_FOO_KEY, USES_ONE_KEY);
        deleteItem(CONFIGURATION, deletePath);

        listener.verify();
    }

    @Test
    public void deleteAugmentChildListenAugmentSubtreeWildcarded() {
        writeTopWithListItem(CONFIGURATION);

        TestListener<TreeComplexUsesAugment> listener = createListener(CONFIGURATION, AUGMENT_WILDCARD,
                added(path(TOP_FOO_KEY, TreeComplexUsesAugment.class), complexUsesAugment(USES_ONE_KEY, USES_TWO_KEY)),
                subtreeModified(path(TOP_FOO_KEY, TreeComplexUsesAugment.class),
                    complexUsesAugment(USES_ONE_KEY, USES_TWO_KEY), complexUsesAugment(USES_TWO_KEY)));

        InstanceIdentifier<?> deletePath = path(TOP_FOO_KEY, USES_ONE_KEY);
        deleteItem(CONFIGURATION, deletePath);

        listener.verify();
    }
}
