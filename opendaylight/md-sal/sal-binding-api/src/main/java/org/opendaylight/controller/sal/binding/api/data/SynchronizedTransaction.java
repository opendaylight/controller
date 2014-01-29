/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;

/**
 * Synchronized wrapper for DataModificationTransaction.
 * 
 * To get instance of synchronized wrapper use {@link #from(DataModificationTransaction)}
 *
 */
public final class SynchronizedTransaction implements DataModificationTransaction,Delegator<DataModificationTransaction> {

    private final DataModificationTransaction delegate;
    
    private SynchronizedTransaction(DataModificationTransaction delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns synchronized wrapper on supplied transaction.
     * 
     * @param transaction Transaction for which synchronized wrapper should be created.
     * @return Synchronized wrapper over transaction.
     */
    public static final SynchronizedTransaction from(DataModificationTransaction transaction) {
        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        if (transaction instanceof SynchronizedTransaction) {
            return (SynchronizedTransaction) transaction;
        }
        return new SynchronizedTransaction(transaction);
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedOperationalData() {
        return delegate.getCreatedOperationalData();
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedConfigurationData() {
        return delegate.getCreatedConfigurationData();
    }

    @Override
    public synchronized DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return delegate.readOperationalData(path);
    }

    @Override
    public synchronized TransactionStatus getStatus() {
        return delegate.getStatus();
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedOperationalData() {
        return delegate.getUpdatedOperationalData();
    }

    @Deprecated
    public synchronized void putRuntimeData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
        delegate.putRuntimeData(path, data);
    }

    @Override
    public synchronized Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public synchronized DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return delegate.readConfigurationData(path);
    }

    @Override
    public synchronized Future<RpcResult<TransactionStatus>> commit() {
        return delegate.commit();
    }

    @Override
    public synchronized void putOperationalData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
        delegate.putOperationalData(path, data);
    }

    @Override
    public synchronized void putConfigurationData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
        delegate.putConfigurationData(path, data);
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedConfigurationData() {
        return delegate.getUpdatedConfigurationData();
    }

    @Deprecated
    public synchronized void removeRuntimeData(InstanceIdentifier<? extends DataObject> path) {
        delegate.removeRuntimeData(path);
    }

    @Override
    public synchronized void removeOperationalData(InstanceIdentifier<? extends DataObject> path) {
        delegate.removeOperationalData(path);
    }

    @Override
    public synchronized void removeConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        delegate.removeConfigurationData(path);
    }

    @Override
    public synchronized Set<InstanceIdentifier<? extends DataObject>> getRemovedConfigurationData() {
        return delegate.getRemovedConfigurationData();
    }

    @Override
    public synchronized Set<InstanceIdentifier<? extends DataObject>> getRemovedOperationalData() {
        return delegate.getRemovedOperationalData();
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalConfigurationData() {
        return delegate.getOriginalConfigurationData();
    }

    @Override
    public synchronized ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener) {
        return delegate.registerListener(listener);
    }

    @Override
    public synchronized Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalOperationalData() {
        return delegate.getOriginalOperationalData();
    }

    @Override
    public synchronized DataModificationTransaction getDelegate() {
        return delegate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        SynchronizedTransaction other = (SynchronizedTransaction) obj;
        if (delegate == null) {
            if (other.delegate != null) {
                return false;
            }
        } else if (!delegate.equals(other.delegate)) {
            return false;
        }
        return true;
    }
}

