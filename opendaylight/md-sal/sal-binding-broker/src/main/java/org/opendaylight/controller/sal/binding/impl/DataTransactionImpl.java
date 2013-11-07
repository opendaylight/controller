package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification;
import org.opendaylight.controller.md.sal.common.impl.ListenerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DataTransactionImpl extends AbstractDataModification<InstanceIdentifier<? extends DataObject>, DataObject>
        implements DataModificationTransaction {

    private final Object identifier;

    private TransactionStatus status;
    private ListenerRegistry<DataTransactionListener> listeners;

    final DataBrokerImpl broker;

    public DataTransactionImpl(DataBrokerImpl dataBroker) {
        super(dataBroker);
        identifier = new Object();
        broker = dataBroker;
        status = TransactionStatus.NEW;
        listeners = new ListenerRegistry<>();
    }

    @Override
    public Future<RpcResult<TransactionStatus>> commit() {
        return broker.commit(this);
    }

    @Override
    public DataObject readConfigurationData(
            org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> path) {
        return broker.readConfigurationData(path);
    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return broker.readOperationalData(path);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((broker == null) ? 0 : broker.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataTransactionImpl other = (DataTransactionImpl) obj;
        if (broker == null) {
            if (other.broker != null)
                return false;
        } else if (!broker.equals(other.broker))
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }

    @Override
    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public Object getIdentifier() {
        return identifier;
    }

    @Override
    public ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener) {
        return listeners.register(listener);
    }

    public void changeStatus(TransactionStatus status) {
        this.status = status;
        Iterable<ListenerRegistration<DataTransactionListener>> listenersToNotify = listeners.getListeners();
        for (ListenerRegistration<DataTransactionListener> listenerRegistration : listenersToNotify) {
            listenerRegistration.getInstance().onStatusUpdated(this, status);
        }
    }
}
