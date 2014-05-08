/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;
import static org.junit.Assert.assertFalse;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertNotContains;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_BAR_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 *
 * This testsuite tests explanation for data change scope and data modifications
 * which were described in
 * https://lists.opendaylight.org/pipermail/controller-dev/2014-July/005541.html
 *
 *
 */
public class ListInsertionDataChangeListenerTest extends AbstractDataChangeListenerTest{

    private static final InstanceIdentifier<Top> TOP = InstanceIdentifier.create(Top.class);
    private static final InstanceIdentifier<TopLevelList> WILDCARDED = TOP.child(TopLevelList.class);
    private static final InstanceIdentifier<TopLevelList> TOP_FOO = TOP.child(TopLevelList.class, TOP_FOO_KEY);
    private static final InstanceIdentifier<TopLevelList> TOP_BAR = TOP.child(TopLevelList.class, TOP_BAR_KEY);


    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        WriteTransaction initialTx = dataBroker.newWriteOnlyTransaction();
        initialTx.put(CONFIGURATION, TOP, top(topLevelList(TOP_FOO_KEY)));
        assertCommit(initialTx.submit());
    }

    @Test
    public void replaceTopNodeSubtreeListeners() {
        TestListener topListener = createListener(CONFIGURATION, TOP, DataChangeScope.SUBTREE);
        TestListener allListener = createListener(CONFIGURATION, WILDCARDED, DataChangeScope.SUBTREE);
        TestListener fooListener = createListener(CONFIGURATION, TOP_FOO, DataChangeScope.SUBTREE);
        TestListener barListener = createListener(CONFIGURATION, TOP_BAR, DataChangeScope.SUBTREE);

        ReadWriteTransaction writeTx = getDataBroker().newReadWriteTransaction();
        writeTx.put(CONFIGURATION, TOP, top(topLevelList(TOP_BAR_KEY)));
        assertCommit(writeTx.submit());
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> top = topListener.event();
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> all = allListener.event();
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> foo = fooListener.event();
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> bar = barListener.event();

        // Listener for TOP element
        assertContains(top.getOriginalData(), TOP,TOP_FOO);
        assertContains(top.getCreatedData(), TOP_BAR);
        assertContains(top.getUpdatedData(), TOP);
        assertContains(top.getRemovedPaths(), TOP_FOO);

        /*
         *  Listener for all list items
         *
         *  Updated should be empty, since no list item was
         *  updated, items were only removed and added
         */
        assertContains(all.getOriginalData(), TOP_FOO);
        assertContains(all.getCreatedData(), TOP_BAR);
        assertEmpty(all.getUpdatedData());
        assertContains(all.getRemovedPaths(), TOP_FOO);


        /*
         *  Listener for all Foo item
         *
         *  This one should see only Foo item removed
         */
        assertContains(foo.getOriginalData(), TOP_FOO);
        assertEmpty(foo.getCreatedData());
        assertEmpty(foo.getUpdatedData());
        assertContains(foo.getRemovedPaths(), TOP_FOO);

        /*
         *  Listener for bar list items
         *
         *  Updated should be empty, since no list item was
         *  updated, items were only removed and added
         */
        assertEmpty(bar.getOriginalData());
        assertContains(bar.getCreatedData(), TOP_BAR);
        assertEmpty(bar.getUpdatedData());
        assertEmpty(bar.getRemovedPaths());
    }

    @Test
    public void mergeTopNodeSubtreeListeners() {
        TestListener topListener = createListener(CONFIGURATION, TOP, DataChangeScope.SUBTREE);
        TestListener allListener = createListener(CONFIGURATION, WILDCARDED, DataChangeScope.SUBTREE);
        TestListener fooListener = createListener(CONFIGURATION, TOP_FOO, DataChangeScope.SUBTREE);
        TestListener barListener = createListener(CONFIGURATION, TOP_BAR, DataChangeScope.SUBTREE);

        ReadWriteTransaction writeTx = getDataBroker().newReadWriteTransaction();
        writeTx.merge(CONFIGURATION, TOP, top(topLevelList(TOP_BAR_KEY)));
        assertCommit(writeTx.submit());

        verifyBarOnlyAdded(topListener,allListener,fooListener,barListener);
    }

    @Test
    public void putTopBarNodeSubtreeListeners() {
        TestListener topListener = createListener(CONFIGURATION, TOP, DataChangeScope.SUBTREE);
        TestListener allListener = createListener(CONFIGURATION, WILDCARDED, DataChangeScope.SUBTREE);
        TestListener fooListener = createListener(CONFIGURATION, TOP_FOO, DataChangeScope.SUBTREE);
        TestListener barListener = createListener(CONFIGURATION, TOP_BAR, DataChangeScope.SUBTREE);

        ReadWriteTransaction writeTx = getDataBroker().newReadWriteTransaction();
        writeTx.put(CONFIGURATION, TOP_BAR, topLevelList(TOP_BAR_KEY));
        assertCommit(writeTx.submit());

        verifyBarOnlyAdded(topListener,allListener,fooListener,barListener);
    }

    @Test
    public void mergeTopBarNodeSubtreeListeners() {
        TestListener topListener = createListener(CONFIGURATION, TOP, DataChangeScope.SUBTREE);
        TestListener allListener = createListener(CONFIGURATION, WILDCARDED, DataChangeScope.SUBTREE);
        TestListener fooListener = createListener(CONFIGURATION, TOP_FOO, DataChangeScope.SUBTREE);
        TestListener barListener = createListener(CONFIGURATION, TOP_BAR, DataChangeScope.SUBTREE);

        ReadWriteTransaction writeTx = getDataBroker().newReadWriteTransaction();
        writeTx.merge(CONFIGURATION, TOP_BAR, topLevelList(TOP_BAR_KEY));
        assertCommit(writeTx.submit());

        verifyBarOnlyAdded(topListener,allListener,fooListener,barListener);
    }

    private void verifyBarOnlyAdded(final TestListener top, final TestListener all, final TestListener foo,
            final TestListener bar) {

        assertFalse(foo.hasEvent());

        // Listener for TOP element
        assertContains(top.event().getOriginalData(), TOP);
        assertNotContains(top.event().getOriginalData(),TOP_FOO);
        assertContains(top.event().getCreatedData(), TOP_BAR);
        assertContains(top.event().getUpdatedData(), TOP);
        assertEmpty(top.event().getRemovedPaths());

        /*
         *  Listener for all list items
         *
         *  Updated should be empty, since no list item was
         *  updated, items were only removed and added
         */
        assertEmpty(all.event().getOriginalData());
        assertContains(all.event().getCreatedData(), TOP_BAR);
        assertEmpty(all.event().getUpdatedData());
        assertEmpty(all.event().getRemovedPaths());

        /*
         *  Listener for all Foo item
         *
         *  Foo Listener should not have foo event
         */
        assertFalse(foo.hasEvent());

        /*
         *  Listener for bar list items
         *
         *  Updated should be empty, since no list item was
         *  updated, items were only removed and added
         */
        assertEmpty(bar.event().getOriginalData());
        assertContains(bar.event().getCreatedData(), TOP_BAR);
        assertEmpty(bar.event().getUpdatedData());
        assertEmpty(bar.event().getRemovedPaths());
    }

}
