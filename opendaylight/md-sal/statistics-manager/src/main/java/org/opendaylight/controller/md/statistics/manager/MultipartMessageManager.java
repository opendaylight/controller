/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * Main responsibility of the class is to manage multipart response 
 * for multipart request. It also handles the flow aggregate request
 * and response mapping. 
 * @author avishnoi@in.ibm.com
 *
 */
public class MultipartMessageManager {

    /*
     *  Map for tx id and type of request, to keep track of all the request sent 
     *  by Statistics Manager. Statistics Manager won't entertain any multipart 
     *  response for which it didn't send the request.  
     */
    
    private static Map<TxIdEntry,Date> txIdToRequestTypeMap = new ConcurrentHashMap<TxIdEntry,Date>();
    /*
     * Map to keep track of the request tx id for flow table statistics request.
     * Because flow table statistics multi part response do not contains the table id.
     */
    private static Map<TxIdEntry,Short> txIdTotableIdMap = new ConcurrentHashMap<TxIdEntry,Short>();
    
    private final int NUMBER_OF_WAIT_CYCLES =2;

    class TxIdEntry{
        private final TransactionId txId;
        private final NodeId nodeId;
        private final StatsRequestType requestType;
        
        public TxIdEntry(NodeId nodeId, TransactionId txId, StatsRequestType requestType){
            this.txId = txId;
            this.nodeId = nodeId;
            this.requestType = requestType;
        }
        public TransactionId getTxId() {
            return txId;
        }
        public NodeId getNodeId() {
            return nodeId;
        }
        public StatsRequestType getRequestType() {
            return requestType;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
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
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (nodeId == null) {
                if (other.nodeId != null) {
                    return false;
                }
            } else if (!nodeId.equals(other.nodeId)) {
                return false;
            }
            if (txId == null) {
                if (other.txId != null) {
                    return false;
                }
            } else if (!txId.equals(other.txId)) {
                return false;
            }
            return true;
        }
        private MultipartMessageManager getOuterType() {
            return MultipartMessageManager.this;
        }
        @Override
        public String toString() {
            return "TxIdEntry [txId=" + txId + ", nodeId=" + nodeId + ", requestType=" + requestType + "]";
        }
    }

    public MultipartMessageManager(){}
    
    public Short getTableIdForTxId(NodeId nodeId,TransactionId id){
        
        return txIdTotableIdMap.get(new TxIdEntry(nodeId,id,null));
        
    }
    
    public void setTxIdAndTableIdMapEntry(NodeId nodeId, TransactionId id,Short tableId){
        
        txIdTotableIdMap.put(new TxIdEntry(nodeId,id,null), tableId);
    }
    
    public boolean isRequestTxIdExist(NodeId nodeId, TransactionId id, Boolean moreRepliesToFollow){
        TxIdEntry entry = new TxIdEntry(nodeId,id,null);
        if(moreRepliesToFollow.booleanValue()){
            return txIdToRequestTypeMap.containsKey(entry);
        }else{
            return txIdToRequestTypeMap.remove(entry)==null?false:true;
        }
    }
    public void addTxIdToRequestTypeEntry (NodeId nodeId, TransactionId id,StatsRequestType type){
        TxIdEntry entry = new TxIdEntry(nodeId,id,type);
        txIdToRequestTypeMap.put(entry, getExpiryTime());
    }
    public boolean removeTxId(NodeId nodeId, TransactionId id){
        TxIdEntry entry = new TxIdEntry(nodeId,id,null);
        return txIdToRequestTypeMap.remove(entry)==null?false:true;
    }
    
    private Date getExpiryTime(){
        Date expires = new Date();
        expires.setTime(expires.getTime()+StatisticsProvider.STATS_THREAD_EXECUTION_TIME*NUMBER_OF_WAIT_CYCLES);
        return expires;
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
        for (Iterator<TxIdEntry> it = txIdToRequestTypeMap.keySet().iterator();it.hasNext();){
            TxIdEntry txIdEntry = it.next();
            Date now = new Date();
            Date expiryTime = txIdToRequestTypeMap.get(txIdEntry);
            if(now.after(expiryTime)){
                it.remove();
                txIdTotableIdMap.remove(txIdEntry);
            }            
        }
    }
}
