/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;

import java.util.ArrayList;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.Root;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.RootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.Fooroot;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.FoorootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.Barroot;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.BarrootBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.test.bug._2562.namespace.rev160101.root.fooroot.BarrootKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Bug2562DeserializedUnkeyedListTest extends AbstractDataChangeListenerTest {
    private static final InstanceIdentifier<Root> ROOT_PATH = InstanceIdentifier.create(Root.class);

    private void writeRoot(final LogicalDatastoreType store) {
        final ReadWriteTransaction readWriteTransaction = getDataBroker().newReadWriteTransaction();
        final Barroot barRoot = new BarrootBuilder().setType(2).setValue(2).setKey(new BarrootKey(2)).build();
        final ArrayList barRootList = new ArrayList();
        barRootList.add(barRoot);
        final Fooroot fooRoot = new FoorootBuilder().setBarroot(barRootList).build();
        final ArrayList fooRootList = new ArrayList();
        fooRootList.add(fooRoot);
        final Root root = new RootBuilder().setFooroot(fooRootList).build();

        readWriteTransaction.put(store, ROOT_PATH, root);
        assertCommit(readWriteTransaction.submit());
    }

    @Test
    public void writeListToList2562Root() {
        final AbstractDataChangeListenerTest.TestListener listenerRoot = createListener(LogicalDatastoreType.CONFIGURATION,
                ROOT_PATH, AsyncDataBroker.DataChangeScope.ONE);
        listenerRoot.startCapture();
        writeRoot(LogicalDatastoreType.CONFIGURATION);
        final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> eventRoot = listenerRoot.event();

        assertContains(eventRoot.getCreatedData(), ROOT_PATH);
        assertEmpty(eventRoot.getUpdatedData());
        assertEmpty(eventRoot.getRemovedPaths());
        assertNotNull(eventRoot.toString());
    }
}
