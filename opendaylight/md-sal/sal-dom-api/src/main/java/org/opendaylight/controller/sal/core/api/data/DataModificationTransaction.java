/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface DataModificationTransaction extends DataModification<InstanceIdentifier, CompositeNode>{

    /**
     * Returns transaction identifier
     * 
     * @return Transaction identifier
     */
    Object getIdentifier();
    
    TransactionStatus getStatus();
    
    /**
     * Commits transaction to be stored in global data repository.
     * 
     * 
     * @return  Future object which returns RpcResult with TransactionStatus 
     *          when transaction is processed by store.
     */
    Future<RpcResult<TransactionStatus>> commit();
    
    ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener);
    
    
    public interface DataTransactionListener extends EventListener {
        
        void onStatusUpdated(DataModificationTransaction transaction,TransactionStatus status);
        
    }
    
    
    
}
