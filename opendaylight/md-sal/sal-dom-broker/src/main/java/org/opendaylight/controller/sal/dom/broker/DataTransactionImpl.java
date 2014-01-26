/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DataTransactionImpl extends AbstractDataTransaction<InstanceIdentifier, CompositeNode> 
    implements DataModificationTransaction {
    private final ListenerRegistry<DataTransactionListener> listeners = new ListenerRegistry<DataTransactionListener>();
    
    
    
    public DataTransactionImpl(Object identifier,DataBrokerImpl dataBroker) {
        super(identifier,dataBroker);
    }

    @Override
    public ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener) {
        return listeners.register(listener);
    }

    protected void onStatusChange(TransactionStatus status) {
        for (ListenerRegistration<DataTransactionListener> listenerRegistration : listeners) {
            listenerRegistration.getInstance().onStatusUpdated(this, status);
        }
    }
}
