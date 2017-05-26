/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.leafOnlyUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;

public class Bug1418AugmentationTest extends AbstractDataChangeListenerTest{
    private static final InstanceIdentifier<Top> TOP = InstanceIdentifier.create(Top.class);
    private static final InstanceIdentifier<TopLevelList> TOP_FOO = TOP.child(TopLevelList.class, TOP_FOO_KEY);
    private static final InstanceIdentifier<TreeLeafOnlyUsesAugment> SIMPLE_AUGMENT =
            TOP.child(TopLevelList.class, TOP_FOO_KEY).augmentation(TreeLeafOnlyUsesAugment.class);
    private static final InstanceIdentifier<TreeComplexUsesAugment> COMPLEX_AUGMENT =
            TOP.child(TopLevelList.class, TOP_FOO_KEY).augmentation(TreeComplexUsesAugment.class);
    private static final ListViaUsesKey LIST_VIA_USES_KEY =
            new ListViaUsesKey("list key");
    private static final ListViaUsesKey LIST_VIA_USES_KEY_MOD =
            new ListViaUsesKey("list key modified");

    @Test
    public void leafOnlyAugmentationCreatedTest() {
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment("test leaf"));
        assertCommit(writeTx.submit());
        assertTrue(listener.hasEvent());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getCreatedData(), SIMPLE_AUGMENT);
        assertEmpty(event.getUpdatedData());
        assertEmpty(event.getOriginalData());
        assertEmpty(event.getRemovedPaths());
    }

    @Test
    public void leafOnlyAugmentationUpdatedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment("test leaf"));
        assertCommit(writeTx.submit());
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment("test leaf changed"));
        assertCommit(writeTx.submit());
        assertTrue(listener.hasEvent());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getUpdatedData(), SIMPLE_AUGMENT);
        assertContains(event.getOriginalData(), SIMPLE_AUGMENT);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getRemovedPaths());
    }

    @Test
    public void leafOnlyAugmentationDeletedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment("test leaf"));
        assertCommit(writeTx.submit());
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, SIMPLE_AUGMENT);
        assertCommit(writeTx.submit());
        assertTrue(listener.hasEvent());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getRemovedPaths(), SIMPLE_AUGMENT);
        assertContains(event.getOriginalData(), SIMPLE_AUGMENT);
        assertEmpty(event.getCreatedData());
        assertEmpty(event.getUpdatedData());
    }

    @Test
    public void complexAugmentationCreatedTest() {
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, COMPLEX_AUGMENT,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugment(LIST_VIA_USES_KEY));
        assertCommit(writeTx.submit());
        assertTrue(listener.hasEvent());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getCreatedData(), COMPLEX_AUGMENT);
        assertContains(event.getCreatedData(), COMPLEX_AUGMENT.child(ListViaUses.class, LIST_VIA_USES_KEY));
        assertEmpty(event.getUpdatedData());
        assertEmpty(event.getOriginalData());
        assertEmpty(event.getRemovedPaths());
    }

    @Test
    public void complexAugmentationUpdatedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugment(LIST_VIA_USES_KEY));
        assertCommit(writeTx.submit());
        TestListener listener = createListener(LogicalDatastoreType.CONFIGURATION, COMPLEX_AUGMENT,
                AsyncDataBroker.DataChangeScope.SUBTREE);
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugment(LIST_VIA_USES_KEY_MOD));
        assertCommit(writeTx.submit());
        assertTrue(listener.hasEvent());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();
        assertContains(event.getUpdatedData(), COMPLEX_AUGMENT);
        assertContains(event.getCreatedData(), COMPLEX_AUGMENT.child(ListViaUses.class, LIST_VIA_USES_KEY_MOD));
        assertContains(event.getRemovedPaths(), COMPLEX_AUGMENT.child(ListViaUses.class, LIST_VIA_USES_KEY));
        assertContains(event.getOriginalData(), COMPLEX_AUGMENT);
        assertContains(event.getOriginalData(), COMPLEX_AUGMENT.child(ListViaUses.class, LIST_VIA_USES_KEY));
    }
}
