/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnorderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.util.Filter;

/**
 * Covers creating, reading and deleting of an item in dataStore.
 */
public class DataServiceIT extends AbstractIT {
    @Inject
    @Filter(timeout = 120 * 1000)
    DataBroker dataBroker;

    /**
     * Ignored this, because classes here are constructed from very different class loader as MD-SAL is run into,
     * this is code is run from different classloader.
     */
    @Test
    public void test() throws Exception {
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        assertNotNull(transaction);

        InstanceIdentifier<UnorderedList> node1 = createNodeRef("0");
        Optional<UnorderedList> node = dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, node1)
                .get(5, TimeUnit.SECONDS);
        assertFalse(node.isPresent());
        UnorderedList nodeData1 = createNode("0");

        transaction.put(LogicalDatastoreType.OPERATIONAL, node1, nodeData1);
        transaction.commit().get(5, TimeUnit.SECONDS);

        Optional<UnorderedList> readedData = dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                node1).get(5, TimeUnit.SECONDS);
        assertTrue(readedData.isPresent());
        assertEquals(nodeData1.key(), readedData.get().key());

        final WriteTransaction transaction2 = dataBroker.newWriteOnlyTransaction();
        assertNotNull(transaction2);

        transaction2.delete(LogicalDatastoreType.OPERATIONAL, node1);

        transaction2.commit().get(5, TimeUnit.SECONDS);

        Optional<UnorderedList> readedData2 = dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                node1).get(5, TimeUnit.SECONDS);
        assertFalse(readedData2.isPresent());
    }

    private static InstanceIdentifier<UnorderedList> createNodeRef(final String string) {
        UnorderedListKey key = new UnorderedListKey(string);
        return InstanceIdentifier.builder(Lists.class).child(UnorderedContainer.class).child(UnorderedList.class, key)
                .build();
    }

    private static UnorderedList createNode(final String string) {
        UnorderedListBuilder ret = new UnorderedListBuilder();
        UnorderedListKey nodeKey = new UnorderedListKey(string);
        ret.withKey(nodeKey);
        ret.setName("name of " + string);
        ret.setName("value of " + string);
        return ret.build();
    }
}
