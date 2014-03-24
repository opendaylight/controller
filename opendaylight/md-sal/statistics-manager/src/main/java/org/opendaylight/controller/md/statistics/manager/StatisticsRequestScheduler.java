/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
    
    private long PendingTransactions;
    
    public StatisticsRequestScheduler(){
        PendingTransactions = 0;
    }
    
    public void addToSchedulerList(NodeStatisticsHandler nodeStatisticsHandler){
        srsLogger.debug("AAA:{} queued up for scheduling",nodeStatisticsHandler.getTargetNodeKey());
        requesters.put(nodeStatisticsHandler, null);
    }
    
    public NodeStatisticsHandler getNextNodeFromSchedulerList(){
        //Remove first element
        Set<NodeStatisticsHandler> nodes = requesters.keySet();
        NodeStatisticsHandler node = null;
        synchronized(requesters){
            if(nodes.iterator().hasNext()){
                node = nodes.iterator().next();
                srsLogger.debug("AAA:{} picked up for execution",node.getTargetNodeKey());
                nodes.iterator().remove();
                return node;
            }
        }
        return node;
    }
    
    @Override
    public synchronized void onStatusUpdated(DataModificationTransaction transaction, TransactionStatus status) {
        
        srsLogger.debug("AAA:Number of pending MD-SAL transactions : {}",this.PendingTransactions);
        
        switch(status){
        case SUBMITED:
            this.PendingTransactions++;
            break;
        case COMMITED:
        case FAILED:
            this.PendingTransactions--;
            if(PendingTransactions == 0){
                NodeStatisticsHandler node = this.getNextNodeFromSchedulerList();
                if(node != null){

                    node.requestPeriodicStatistics();

                    node.cleanStaleStatistics();
                }
            }
            break;
        default:
            break;
        }
    }

}
