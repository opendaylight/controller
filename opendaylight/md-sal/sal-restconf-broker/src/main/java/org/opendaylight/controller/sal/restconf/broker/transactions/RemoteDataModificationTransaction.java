/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.transactions;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class RemoteDataModificationTransaction implements DataModificationTransaction {
    //TODO implement this

    @Override
    public Object getIdentifier() {
        return null;
    }

    @Override
    public TransactionStatus getStatus() {
        return null;
    }

    @Override
    public void putRuntimeData(InstanceIdentifier<? extends DataObject> path, DataObject data) {

    }

    @Override
    public void putOperationalData(InstanceIdentifier<? extends DataObject> path, DataObject data) {

    }

    @Override
    public void putConfigurationData(InstanceIdentifier<? extends DataObject> path, DataObject data) {

    }

    @Override
    public void removeRuntimeData(InstanceIdentifier<? extends DataObject> path) {

    }

    @Override
    public void removeOperationalData(InstanceIdentifier<? extends DataObject> path) {

    }

    @Override
    public void removeConfigurationData(InstanceIdentifier<? extends DataObject> path) {

    }

    @Override
    public Future<RpcResult<TransactionStatus>> commit() {
        return null;
    }

    @Override
    public ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener) {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedOperationalData() {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedConfigurationData() {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedOperationalData() {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedConfigurationData() {
        return null;
    }

    @Override
    public Set<InstanceIdentifier<? extends DataObject>> getRemovedConfigurationData() {
        return null;
    }

    @Override
    public Set<InstanceIdentifier<? extends DataObject>> getRemovedOperationalData() {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalConfigurationData() {
        return null;
    }

    @Override
    public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalOperationalData() {
        return null;
    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return null;
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return null;
    }
}
