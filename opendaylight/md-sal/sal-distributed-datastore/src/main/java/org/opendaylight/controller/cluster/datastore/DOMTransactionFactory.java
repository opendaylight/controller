/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.slf4j.Logger;

/**
 * A factory for creating DOM transactions, either normal or chained.
 *
 * @author Thomas Pantelis
 */
public class DOMTransactionFactory {

    private final Map<String, DOMStoreTransactionChain> transactionChains = new HashMap<>();
    private final InMemoryDOMDataStore store;
    private final ShardStats shardMBean;
    private final Logger log;
    private final String name;

    public DOMTransactionFactory(InMemoryDOMDataStore store, ShardStats shardMBean, Logger log, String name) {
        this.store = store;
        this.shardMBean = shardMBean;
        this.log = log;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public <T extends DOMStoreTransaction> T newTransaction(TransactionProxy.TransactionType type,
            String transactionID, String transactionChainID) {

        DOMStoreTransactionFactory factory = store;

        if(!transactionChainID.isEmpty()) {
            factory = transactionChains.get(transactionChainID);
            if(factory == null) {
                if(log.isDebugEnabled()) {
                    log.debug("{}: Creating transaction with ID {} from chain {}", name, transactionID,
                            transactionChainID);
                }

                DOMStoreTransactionChain transactionChain = store.createTransactionChain();
                transactionChains.put(transactionChainID, transactionChain);
                factory = transactionChain;
            }
        } else {
            log.debug("{}: Creating transaction with ID {}", name, transactionID);
        }

        T transaction = null;
        switch(type) {
            case READ_ONLY:
                transaction = (T) factory.newReadOnlyTransaction();
                shardMBean.incrementReadOnlyTransactionCount();
                break;
            case READ_WRITE:
                transaction = (T) factory.newReadWriteTransaction();
                shardMBean.incrementReadWriteTransactionCount();
                break;
            case WRITE_ONLY:
                transaction = (T) factory.newWriteOnlyTransaction();
                shardMBean.incrementWriteOnlyTransactionCount();
                break;
        }

        return transaction;
    }

    public void closeTransactionChain(String transactionChainID) {
        DOMStoreTransactionChain chain =
                transactionChains.remove(transactionChainID);

        if(chain != null) {
            chain.close();
        }
    }

    public void closeAllTransactionChains() {
        for(Map.Entry<String, DOMStoreTransactionChain> entry : transactionChains.entrySet()){
            entry.getValue().close();
        }

        transactionChains.clear();
    }
}
