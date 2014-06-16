package org.opendaylight.controller.cluster.datastore;

import junit.framework.Assert;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DistributedDataStoreTest {

    private DistributedDataStore distributedDataStore;

    @org.junit.Before
    public void setUp() throws Exception {
        distributedDataStore = new DistributedDataStore();
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void testRegisterChangeListener() throws Exception {
        ListenerRegistration registration =
                distributedDataStore.registerChangeListener(InstanceIdentifier.builder().build(), new AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
                throw new UnsupportedOperationException("onDataChanged");
            }
        }, AsyncDataBroker.DataChangeScope.BASE);

        Assert.assertNotNull(registration);
    }

    @org.junit.Test
    public void testCreateTransactionChain() throws Exception {
        final DOMStoreTransactionChain transactionChain = distributedDataStore.createTransactionChain();
        Assert.assertNotNull(transactionChain);
    }

    @org.junit.Test
    public void testNewReadOnlyTransaction() throws Exception {
        final DOMStoreReadTransaction transaction = distributedDataStore.newReadOnlyTransaction();
        Assert.assertNotNull(transaction);
    }

    @org.junit.Test
    public void testNewWriteOnlyTransaction() throws Exception {
        final DOMStoreWriteTransaction transaction = distributedDataStore.newWriteOnlyTransaction();
        Assert.assertNotNull(transaction);
    }

    @org.junit.Test
    public void testNewReadWriteTransaction() throws Exception {
        final DOMStoreReadWriteTransaction transaction = distributedDataStore.newReadWriteTransaction();
        Assert.assertNotNull(transaction);
    }
}
