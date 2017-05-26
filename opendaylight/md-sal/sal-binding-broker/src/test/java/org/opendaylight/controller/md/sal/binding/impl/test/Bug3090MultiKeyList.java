/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertContains;
import static org.opendaylight.controller.md.sal.binding.test.AssertCollections.assertEmpty;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataChangeListenerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.Root;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.RootBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.root.ListInRoot;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.opendaylight.test.bug._3090.rev160101.root.ListInRootBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Bug3090MultiKeyList extends AbstractDataChangeListenerTest{
    private static final InstanceIdentifier<Root> ROOT_PATH = InstanceIdentifier.create(Root.class);

    private void write(final LogicalDatastoreType store) {
        final ReadWriteTransaction readWriteTransaction = getDataBroker().newReadWriteTransaction();

        final List<ListInRoot> listInRoots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            listInRoots.add(new ListInRootBuilder()
                .setLeafA("leaf a" + i)
                .setLeafC("leaf c" + i)
                .setLeafB("leaf b" + i)
                .build()
            );
        }
        final Root root = new RootBuilder().setListInRoot(listInRoots).build();
        readWriteTransaction.put(store, ROOT_PATH, root);
        assertCommit(readWriteTransaction.submit());
    }

    @Test
    public void listWithMultiKeyTest() {
        final AbstractDataChangeListenerTest.TestListener listener = createListener(CONFIGURATION, ROOT_PATH,
                AsyncDataBroker.DataChangeScope.BASE);
        listener.startCapture();

        write(CONFIGURATION);
        final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event = listener.event();

        assertContains(event.getCreatedData(), ROOT_PATH);
        assertEmpty(event.getUpdatedData());
        assertEmpty(event.getRemovedPaths());
    }
}