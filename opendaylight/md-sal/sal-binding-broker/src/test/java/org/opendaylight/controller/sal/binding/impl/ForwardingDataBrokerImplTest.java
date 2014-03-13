package org.opendaylight.controller.sal.binding.impl;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import static org.mockito.Mockito.mock;

public class ForwardingDataBrokerImplTest {

    ForwardingDataBrokerImpl forwardingDataBroker;

    @Before
    public void setUp(){
        forwardingDataBroker = new ForwardingDataBrokerImpl();
        forwardingDataBroker.setDomDataBroker(mock(DOMDataBroker.class));
        forwardingDataBroker.setMappingService(mock(BindingIndependentMappingService.class));
    }

    @Test
    public void testNewReadOnlyTransaction() throws Exception {
        BindingDataReadTransaction transaction = forwardingDataBroker.newReadOnlyTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);
    }

    @Test
    public void testNewReadWriteTransaction() throws Exception {
        BindingDataReadWriteTransaction transaction = forwardingDataBroker.newReadWriteTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);
    }

    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        BindingDataWriteTransaction transaction = forwardingDataBroker.newWriteOnlyTransaction();
        Assert.assertNotNull("transaction should not be null", transaction);
    }

    @Test
    public void testRegisterDataChangeListener() throws Exception {
        ListenerRegistration<BindingDataChangeListener> listener = forwardingDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class).build(), mock(BindingDataChangeListener.class), AsyncDataBroker.DataChangeScope.BASE);
        Assert.assertNotNull("listener should not be null", listener);
    }

}
