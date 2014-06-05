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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction.DataTransactionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main responsibility of the class is to check the MD-SAL data store read/write
 * transaction accumulation level and send statistics request if number of pending 
 * read/write transactions are zero.
 * @author avishnoi@in.ibm.com
 *
 */
@SuppressWarnings("rawtypes")
public class StatisticsRequestScheduler implements DataTransactionListener {

    private static final Logger srsLogger = LoggerFactory.getLogger(StatisticsRequestScheduler.class);
    private final Timer timer = new Timer("request-monitor", true);

    // We need ordered retrieval, and O(1) contains operation
    private final Map<AbstractStatsTracker,Integer> requestQueue = 
            Collections.synchronizedMap(new LinkedHashMap<AbstractStatsTracker,Integer>());
    
    private Long PendingTransactions;
    
    private long lastRequestTime = System.nanoTime();
    
    private static final long REQUEST_MONITOR_INTERVAL = 1000;
    
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            long now = System.nanoTime();
            if(now > lastRequestTime+TimeUnit.MILLISECONDS.toNanos(REQUEST_MONITOR_INTERVAL)){
                requestStatistics();
            }
        }
    };

    public StatisticsRequestScheduler(){
        PendingTransactions = (long) 0;
    }
    
    public void addRequestToSchedulerQueue(AbstractStatsTracker statsRequest){
        requestQueue.put(statsRequest, null);
    }
    
    public void removeRequestsFromSchedulerQueue(NodeRef node){
        AbstractStatsTracker stats = null;
        synchronized(requestQueue){
            Iterator<Map.Entry<AbstractStatsTracker, Integer>> nodesItr = requestQueue.entrySet().iterator();
            while(nodesItr.hasNext()){
                stats = nodesItr.next().getKey();
                if(stats.getNodeRef().equals(node)){
                    nodesItr.remove();
                }
            }
        }

    }
    public AbstractStatsTracker getNextRequestFromSchedulerQueue(){
        //Remove first element
        AbstractStatsTracker stats = null;
        synchronized(requestQueue){
            Iterator<Map.Entry<AbstractStatsTracker, Integer>> nodesItr = requestQueue.entrySet().iterator();
            if(nodesItr.hasNext()){
                stats = nodesItr.next().getKey();
                srsLogger.debug("{} chosen up for execution",stats.getNodeRef());
                nodesItr.remove();
                return stats;
            }
        }
        return stats;
    }

    private void requestStatistics(){
        AbstractStatsTracker stats = this.getNextRequestFromSchedulerQueue();
        sendStatsRequest(stats);
    }
    @Override
    public void onStatusUpdated(DataModificationTransaction transaction, TransactionStatus status) {
        
        AbstractStatsTracker stats = null;
        synchronized(PendingTransactions){
            switch(status){
            case SUBMITED:
                this.PendingTransactions++;
                break;
            case COMMITED:
            case FAILED:
                this.PendingTransactions--;
                if(PendingTransactions == 0){
                    lastRequestTime = System.nanoTime();
                    stats = this.getNextRequestFromSchedulerQueue();
                }
                srsLogger.debug("Pending MD-SAL transactions : {} & Scheduler queue size : {}",this.PendingTransactions,this.requestQueue.size());
                break;
            default:
                break;
            }
        }
        sendStatsRequest(stats);
    }
    
    private void sendStatsRequest(AbstractStatsTracker stats){
        if(stats != null){
            try{
                stats.request();
                stats.increaseRequestCounter();
            }catch(Exception e){
                srsLogger.warn("Statistics request was not sent successfully. Reason : {}",e.getMessage());
            }
        }
    }
    public void start(){
        timer.schedule(task, 0, REQUEST_MONITOR_INTERVAL);
    }
}
