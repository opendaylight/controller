/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction.DataTransactionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main responsibility of the class is to check the MD-SAL data store read/write
 * transaction accumulation level and send statistics request if number of pending 
 * read/write transactions are zero.
 * @author avishnoi@in.ibm.com
 *
 */
public class StatisticsRequestScheduler implements DataTransactionListener {

    private static final Logger srsLogger = LoggerFactory.getLogger(StatisticsRequestScheduler.class);

    // We need ordered retrieval, and O(1) contains operation
    private final Map<NodeStatisticsHandler,Integer> requesters = 
            Collections.synchronizedMap(new LinkedHashMap<NodeStatisticsHandler,Integer>());
    
    private Long PendingTransactions;
    
    public StatisticsRequestScheduler(){
        PendingTransactions = (long) 0;
    }
    
    public void addToSchedulerList(NodeStatisticsHandler node){
        srsLogger.debug("{} queued up for scheduling. Queue Size : {}",
                node.getTargetNodeKey(),requesters.size());
        if(requesters.size()==0 && PendingTransactions ==0){
            requestNodeStatistics(node);
        }else{
            requesters.put(node, null);
        }
    }
    
    public NodeStatisticsHandler getNextNodeFromSchedulerList(){
        //Remove first element
        NodeStatisticsHandler node = null;
        synchronized(requesters){
            Iterator<Map.Entry<NodeStatisticsHandler, Integer>> nodesItr = requesters.entrySet().iterator();
            if(nodesItr.hasNext()){
                node = nodesItr.next().getKey();
                srsLogger.debug("{} picked up for execution",node.getTargetNodeKey());
                nodesItr.remove();
                return node;
            }
        }
        return node;
    }
    
    private void requestNodeStatistics(NodeStatisticsHandler node){
        node.requestPeriodicStatistics();

        node.cleanStaleStatistics();
    }
    
    @Override
    public void onStatusUpdated(DataModificationTransaction transaction, TransactionStatus status) {
        
        srsLogger.debug("Number of pending MD-SAL transactions : {}",this.PendingTransactions);
        NodeStatisticsHandler node = null;
        synchronized(PendingTransactions){
            switch(status){
            case SUBMITED:
                this.PendingTransactions++;
                break;
            case COMMITED:
            case FAILED:
                this.PendingTransactions--;
                if(PendingTransactions == 0){
                    node = this.getNextNodeFromSchedulerList();
                }
                break;
            default:
                break;
            }
        }
        if(node != null){
            requestNodeStatistics(node);
        }
    }
}
