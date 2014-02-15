/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;

import com.google.common.base.Preconditions;

/**
 * Main responsibility of the class is to manage multipart response
 * for multipart request. It also handles the flow aggregate request
 * and response mapping.
 * @author avishnoi@in.ibm.com
 *
 */
class MultipartMessageManager {
    /*
     *  Map for tx id and type of request, to keep track of all the request sent
     *  by Statistics Manager. Statistics Manager won't entertain any multipart
     *  response for which it didn't send the request.
     */
    private final Map<TxIdEntry,Long> txIdToRequestTypeMap = new ConcurrentHashMap<>();
    /*
     * Map to keep track of the request tx id for flow table statistics request.
     * Because flow table statistics multi part response do not contains the table id.
     */
    private final Map<TxIdEntry,Short> txIdTotableIdMap = new ConcurrentHashMap<>();
    private final long lifetimeNanos;

    public MultipartMessageManager(long lifetimeNanos) {
        this.lifetimeNanos = lifetimeNanos;
    }

    private static final class TxIdEntry {
        private final TransactionId txId;

        public TxIdEntry(TransactionId txId) {
            this.txId = txId;
        }
        public TransactionId getTxId() {
            return txId;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((txId == null) ? 0 : txId.hashCode());
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
            if (!(obj instanceof TxIdEntry)) {
                return false;
            }
            TxIdEntry other = (TxIdEntry) obj;

            if (txId == null) {
                if (other.txId != null) {
                    return false;
                }
            } else if (!txId.equals(other.txId)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "TxIdEntry [txId=" + txId + ']';
        }
    }

    public void recordExpectedTableTransaction(TransactionId id, Short tableId) {
        recordExpectedTransaction(id);
        txIdTotableIdMap.put(new TxIdEntry(id), Preconditions.checkNotNull(tableId));
    }

    public Short isExpectedTableTransaction(TransactionAware transaction, Boolean more) {
        if (!isExpectedTransaction(transaction, more)) {
            return null;
        }

        final TxIdEntry key = new TxIdEntry(transaction.getTransactionId());
        if (more != null && more.booleanValue()) {
            return txIdTotableIdMap.get(key);
        } else {
            return txIdTotableIdMap.remove(key);
        }
    }

    public void recordExpectedTransaction(TransactionId id) {
        TxIdEntry entry = new TxIdEntry(Preconditions.checkNotNull(id));
        txIdToRequestTypeMap.put(entry, getExpiryTime());
    }

    public boolean isExpectedTransaction(TransactionAware transaction, Boolean more) {
        TxIdEntry entry = new TxIdEntry(transaction.getTransactionId());
        if (more != null && more.booleanValue()) {
            return txIdToRequestTypeMap.containsKey(entry);
        } else {
            return txIdToRequestTypeMap.remove(entry) != null;
        }
    }

    private Long getExpiryTime() {
        return System.nanoTime() + lifetimeNanos;
    }

    public void cleanStaleTransactionIds() {
        final long now = System.nanoTime();

        for (Iterator<TxIdEntry> it = txIdToRequestTypeMap.keySet().iterator();it.hasNext();){
            TxIdEntry txIdEntry = it.next();

            Long expiryTime = txIdToRequestTypeMap.get(txIdEntry);
            if(now > expiryTime){
                it.remove();
                txIdTotableIdMap.remove(txIdEntry);
            }
        }
    }
}
