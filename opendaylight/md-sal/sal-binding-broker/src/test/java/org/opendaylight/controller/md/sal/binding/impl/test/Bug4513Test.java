package org.opendaylight.controller.md.sal.binding.impl.test;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.ListenerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.ListenerTestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.listener.test.ListItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.listener.rev150825.listener.test.ListItemBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class Bug4513Test extends AbstractDataBrokerTest {
    @Test
    public void testChangeEvent() throws Exception {
        DataBroker dataBroker = getDataBroker();

        DataChangeListener listener = mock(DataChangeListener.class);
        InstanceIdentifier<ListItem> wildCard = InstanceIdentifier.builder(ListenerTest.class)
                .child(ListItem.class)
                .build();
        ListenerRegistration<DataChangeListener> reg = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, wildCard,
                listener, AsyncDataBroker.DataChangeScope.SUBTREE);

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        ArrayList<ListItem> list = new ArrayList<>();
        ListItemBuilder node = new ListItemBuilder()
                .setSip("name")
                .setOp(43L);
        list.add(node.build());
        ListenerTestBuilder builder = new ListenerTestBuilder().setListItem(list);
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(ListenerTest.class).build(), builder.build());
        CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();

        assertCommit(submit);

        ArgumentCaptor<AsyncDataChangeEvent> captor = ArgumentCaptor.forClass(AsyncDataChangeEvent.class);

        verify(listener, timeout(100)).onDataChanged(captor.capture());

        AsyncDataChangeEvent value = captor.getValue();
        assertTrue(!value.getCreatedData().isEmpty());
    }
}
