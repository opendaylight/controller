/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.ioc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataStore {
    private static final FutureCallback<Void> DEFAULT_CALLBACK =
            new FutureCallback<Void>() {
                public void onSuccess(Void result) {
                    // TODO: Implement default behaviour
                }

                public void onFailure(Throwable t) {
                    // TODO: Implement default behaviour
                };
            };

    @Inject
    private MdSAL mdSAL;

    public ListenerRegistration<DataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
                                                                               InstanceIdentifier<?> path,
                                                                               DataChangeListener listener,
                                                                               AsyncDataBroker.DataChangeScope triggeringScope) {
        return mdSAL.getDataBroker().registerDataChangeListener(store, path, listener, triggeringScope);
    }

    public <T extends DataObject> void asyncPUT(LogicalDatastoreType datastoreType,
                                                InstanceIdentifier<T> path,
                                                T data) {
        asyncPUT(datastoreType, path, data, DEFAULT_CALLBACK);
    }

    public <T extends DataObject> void asyncPUT(LogicalDatastoreType datastoreType,
                                                InstanceIdentifier<T> path,
                                                T data,
                                                FutureCallback<Void> callback) {
        WriteTransaction tx = mdSAL.getDataBroker().newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        execPut(tx, callback);
    }

    public <T extends DataObject> T read(LogicalDatastoreType datastoreType,
                                         InstanceIdentifier<T> path) {

        ReadOnlyTransaction tx = mdSAL.getDataBroker().newReadOnlyTransaction();
        T result = null;

        try {
            result = tx.read(datastoreType, path).get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private static void execPut(WriteTransaction tx, FutureCallback<Void> callback) {
        Futures.addCallback(tx.submit(), callback);
    }
}
