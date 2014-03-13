package org.opendaylight.controller.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class ForwardingDataBrokerImplTest {

    ForwardingDataBrokerImpl forwardingDataBroker;
    DOMDataBroker domDataBroker;
    BindingIndependentMappingService bindingIndependentMappingService;


    @Before
    public void setUp() throws Exception {
        forwardingDataBroker = new ForwardingDataBrokerImpl();

        domDataBroker = mock(DOMDataBroker.class);
        DOMDataReadTransaction readTransaction = mock(DOMDataReadTransaction.class);
        DOMDataReadWriteTransaction readWriteTransaction = mock(DOMDataReadWriteTransaction.class);
        ListenableFuture<Optional<NormalizedNode<?, ?>>> future = mock(ListenableFuture.class);

        Mockito.when(domDataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        Mockito.when(domDataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

        Mockito.when(readTransaction.read(any(LogicalDatastoreType.class), any(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.class))).thenReturn(future);
        Mockito.when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.class))).thenReturn(future);

        Mockito.when(future.get()).thenReturn(mock(Optional.class));
        Mockito.when(future.get(1000, TimeUnit.MICROSECONDS)).thenReturn(mock(Optional.class));

        bindingIndependentMappingService = mock(BindingIndependentMappingService.class);
        Mockito.when(bindingIndependentMappingService.dataObjectFromDataDom(any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(mock(DataObject.class));
        Mockito.when(bindingIndependentMappingService.toDataDom(any(DataObject.class))).thenReturn(mock(CompositeNode.class));

        forwardingDataBroker.setDomDataBroker(domDataBroker);
        forwardingDataBroker.setMappingService(bindingIndependentMappingService);
    }

    @Test
    public void testNewReadOnlyTransaction() throws Exception {
        BindingDataReadTransaction transaction = forwardingDataBroker.newReadOnlyTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);

        final ListenableFuture<Optional<DataObject>> listenableFuture = transaction.read(LogicalDatastoreType.CONFIGURATION, nodesIdentifier());
        Assert.assertNotNull("listenableFuture should not be null", listenableFuture);

        listenableFuture.addListener(mock(Runnable.class), mock(Executor.class));
        Assert.assertNotNull("should get a valid future", listenableFuture.get());
        Assert.assertNotNull("should get a valid future", listenableFuture.get(1000, TimeUnit.MICROSECONDS));
    }

    @Test
    public void testNewReadWriteTransaction() throws Exception {
        BindingDataReadWriteTransaction transaction = forwardingDataBroker.newReadWriteTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);

        final ListenableFuture<Optional<DataObject>> listenableFuture = transaction.read(LogicalDatastoreType.CONFIGURATION, nodesIdentifier());
        Assert.assertNotNull("listenableFuture should not be null", listenableFuture);

        listenableFuture.addListener(mock(Runnable.class), mock(Executor.class));
        Assert.assertNotNull("should get a valid future", listenableFuture.get());
        Assert.assertNotNull("should get a valid future", listenableFuture.get(1000, TimeUnit.MICROSECONDS));

        // FIXME : Will continue to fail as long as we have the invalid cast to NormalizedNode
        // transaction.put(LogicalDatastoreType.CONFIGURATION, nodesIdentifier(), mock(DataObject.class));
    }

    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        BindingDataWriteTransaction transaction = forwardingDataBroker.newWriteOnlyTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);

    }

    @Test
    public void testRegisterDataChangeListener() throws Exception {
        ListenerRegistration<BindingDataChangeListener> listener = forwardingDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodesIdentifier() , mock(BindingDataChangeListener.class), AsyncDataBroker.DataChangeScope.BASE);
        Assert.assertNotNull("listener should not be null", listener);
    }



    private InstanceIdentifier nodesIdentifier(){
        return InstanceIdentifier.builder(Nodes.class).build();
    }

}
