package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class DOMDataBrokerProxy extends AbstractBrokerServiceProxy<DOMDataBroker> implements DOMDataBroker {

    public DOMDataBrokerProxy(final ServiceReference<DOMDataBroker> ref, final DOMDataBroker delegate) {
        super(ref, delegate);
    }

    @Override
    public DOMDataReadTransaction newReadOnlyTransaction() {
        return getDelegate().newReadOnlyTransaction();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return getDelegate().newReadWriteTransaction();
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return getDelegate().newWriteOnlyTransaction();
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final InstanceIdentifier path, final DOMDataChangeListener listener,
            final org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope triggeringScope) {
        return getDelegate().registerDataChangeListener(store, path, listener, triggeringScope);
    }

}
