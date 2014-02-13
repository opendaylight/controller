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
import java.util.concurrent.TimeUnit;

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
public class MultipartMessageManager {
    private static final int NUMBER_OF_WAIT_CYCLES = 2;

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

    private static final class TxIdEntry {
        private final StatsRequestType requestType;
        private final TransactionId txId;

        public TxIdEntry(TransactionId txId, StatsRequestType requestType){
            this.txId = txId;
            this.requestType = requestType;
        }
        public TransactionId getTxId() {
            return txId;
        }
        public StatsRequestType getRequestType() {
            return requestType;
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
            return "TxIdEntry [txId=" + txId + ", requestType=" + requestType + "]";
        }
    }

    public void recordExpectedTableTransaction(TransactionId id, StatsRequestType type, Short tableId) {
        recordExpectedTransaction(id, type);
        txIdTotableIdMap.put(new TxIdEntry(id, null), Preconditions.checkNotNull(tableId));
    }

    public Short isExpectedTableTransaction(TransactionAware transaction, Boolean more) {
        if (!isExpectedTransaction(transaction, more)) {
            return null;
        }

        final TxIdEntry key = new TxIdEntry(transaction.getTransactionId(), null);
        if (more != null && more.booleanValue()) {
            return txIdTotableIdMap.get(key);
        } else {
            return txIdTotableIdMap.remove(key);
        }
    }

    public void recordExpectedTransaction(TransactionId id, StatsRequestType type) {
        TxIdEntry entry = new TxIdEntry(Preconditions.checkNotNull(id), Preconditions.checkNotNull(type));
        txIdToRequestTypeMap.put(entry, getExpiryTime());
    }

    public boolean isExpectedTransaction(TransactionAware transaction, Boolean more) {
        TxIdEntry entry = new TxIdEntry(transaction.getTransactionId(), null);
        if (more != null && more.booleanValue()) {
            return txIdToRequestTypeMap.containsKey(entry);
        } else {
            return txIdToRequestTypeMap.remove(entry) != null;
        }
    }

    private static Long getExpiryTime(){
        return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(
                StatisticsProvider.STATS_COLLECTION_MILLIS*NUMBER_OF_WAIT_CYCLES);
    }

    public enum StatsRequestType{
        ALL_FLOW,
        AGGR_FLOW,
        ALL_PORT,
        ALL_FLOW_TABLE,
        ALL_QUEUE_STATS,
        ALL_GROUP,
        ALL_METER,
        GROUP_DESC,
        METER_CONFIG
    }

    public void cleanStaleTransactionIds(){
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
