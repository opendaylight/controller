package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Test;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class Bug4482Test extends AbstractDataBrokerTest {
    @Test
    public void testWildcardNotificationOfPreexistingData() throws Exception {
        InstanceIdentifier<Top> id = InstanceIdentifier.builder(Top.class).build();
        ArrayList<TopLevelList> list = new ArrayList<>();
        list.add(new TopLevelListBuilder().setName("name").build());
        TopBuilder builder = new TopBuilder().setTopLevelList(list);

        DataBroker dataBroker = getDataBroker();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, id, builder.build());
        assertCommit(writeTransaction.submit());

        DataChangeListener listener = mock(DataChangeListener.class);
        InstanceIdentifier<TopLevelList> wildcard = InstanceIdentifier.builder(Top.class)
                .child(TopLevelList.class)
                .build();
        ListenerRegistration<DataChangeListener> reg = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, wildcard, listener, AsyncDataBroker.DataChangeScope.SUBTREE);

        verify(listener, timeout(1000)).onDataChanged(Matchers.<AsyncDataChangeEvent>anyObject());
    }

    @Test
    public void testNotificationOfPreexistingData() throws Exception {
        InstanceIdentifier<Top> id = InstanceIdentifier.builder(Top.class).build();
        ArrayList<TopLevelList> list = new ArrayList<>();
        list.add(new TopLevelListBuilder().setName("name").build());
        TopBuilder builder = new TopBuilder().setTopLevelList(list);

        DataBroker dataBroker = getDataBroker();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, id, builder.build());
        assertCommit(writeTransaction.submit());

        DataChangeListener listener = mock(DataChangeListener.class);
        ListenerRegistration<DataChangeListener> reg = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, id, listener, AsyncDataBroker.DataChangeScope.SUBTREE);

        verify(listener, timeout(1000)).onDataChanged(Matchers.<AsyncDataChangeEvent>anyObject());
    }
}
