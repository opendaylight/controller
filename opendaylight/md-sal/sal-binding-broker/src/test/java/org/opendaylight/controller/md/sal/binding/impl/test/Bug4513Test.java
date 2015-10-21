/*
 * Copyright (c) 2015 Cisco Systems, Inc., Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.ListenerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.ListenerTestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.listener.test.ListItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.listener.test.ListItemBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Regression test suite for https://bugs.opendaylight.org/show_bug.cgi?id=4513 - Change event is empty when
 * Homogeneous composite key is used homogeneous composite key is used.
 */
public class Bug4513Test extends AbstractDataBrokerTest {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangeListener() throws Exception {
        DataChangeListener listener = mock(DataChangeListener.class);
        InstanceIdentifier<ListItem> wildCard = InstanceIdentifier.builder(ListenerTest.class)
                .child(ListItem.class).build();
        ListenerRegistration<DataChangeListener> reg = getDataBroker().registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, wildCard, listener, AsyncDataBroker.DataChangeScope.SUBTREE);

        final ListItem item = writeListItem();

        ArgumentCaptor<AsyncDataChangeEvent> captor = ArgumentCaptor.forClass(AsyncDataChangeEvent.class);

        verify(listener, timeout(100)).onDataChanged(captor.capture());

        AsyncDataChangeEvent event = captor.getValue();
        assertEquals("createdData", 1, event.getCreatedData().size());
        assertEquals("ListItem", item, event.getCreatedData().values().iterator().next());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataTreeChangeListener() throws Exception {
        DataBroker dataBroker = getDataBroker();

        DataTreeChangeListener<ListItem> listener = mock(DataTreeChangeListener.class);
        InstanceIdentifier<ListItem> wildCard = InstanceIdentifier.builder(ListenerTest.class)
                .child(ListItem.class).build();
        ListenerRegistration<DataTreeChangeListener<ListItem>> reg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, wildCard), listener);

        final ListItem item = writeListItem();

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);

        verify(listener, timeout(100)).onDataTreeChanged(captor.capture());

        Collection<DataTreeModification<ListItem>> mods = captor.getValue();
        assertEquals("ListItem", item, mods.iterator().next().getRootNode().getDataAfter());
    }

    private ListItem writeListItem() {
        WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        final ListItem item = new ListItemBuilder().setSip("name").setOp(43L).build();
        ListenerTestBuilder builder = new ListenerTestBuilder().setListItem(Arrays.asList(item));
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(
                ListenerTest.class).build(), builder.build());
        assertCommit(writeTransaction.submit());
        return item;
    }
}
