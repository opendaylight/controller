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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;

/**
 * Main responsibility of the class is to check the MD-SAL data store read/write
 * transaction accumulation level and send statistics request if number of pending
 * read/write transactions are zero.
 * @author avishnoi@in.ibm.com
 *
 */
public class StatisticsRequestScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsRequestScheduler.class);
    private static final long REQUEST_MONITOR_INTERVAL = 1000;

    private final Timer timer = new Timer("request-monitor", true);
//    private BindingTransactionChain txChain;

    // We need ordered retrieval, and O(1) contains operation
    private final Map<AbstractStatsTracker<?, ?>,Integer> requestQueue =
            Collections.synchronizedMap(new LinkedHashMap<AbstractStatsTracker<?, ?>,Integer>());

    private Long PendingTransactions;

    private long lastRequestTime = System.nanoTime();

    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            try{
                long now = System.nanoTime();
                if(now > lastRequestTime+TimeUnit.MILLISECONDS.toNanos(REQUEST_MONITOR_INTERVAL)){
                    requestStatistics();
                }
            }catch (IllegalArgumentException | IllegalStateException | NullPointerException e){
                LOG.warn("Exception occured while sending statistics request : {}",e);
            }
        }
    };

    public StatisticsRequestScheduler (DataBroker dataBroker) {

    }

    public StatisticsRequestScheduler(){
        PendingTransactions = (long) 0;
    }

    public void addRequestToSchedulerQueue(AbstractStatsTracker<?, ?> statsRequest){
        requestQueue.put(statsRequest, null);
    }

    public void removeRequestsFromSchedulerQueue(NodeRef node){
        AbstractStatsTracker<?, ?> stats = null;
        synchronized(requestQueue){
            Iterator<Map.Entry<AbstractStatsTracker<?, ?>, Integer>> nodesItr = requestQueue.entrySet().iterator();
            while(nodesItr.hasNext()){
                stats = nodesItr.next().getKey();
                if(stats.getNodeRef().equals(node)){
                    nodesItr.remove();
                }
            }
        }

    }
    public AbstractStatsTracker<?, ?> getNextRequestFromSchedulerQueue(){
        //Remove first element
        AbstractStatsTracker<?, ?> stats = null;
        synchronized(requestQueue){
            Iterator<Map.Entry<AbstractStatsTracker<?, ?>, Integer>> nodesItr = requestQueue.entrySet().iterator();
            if(nodesItr.hasNext()){
                stats = nodesItr.next().getKey();
                LOG.debug("{} chosen up for execution",stats.getNodeRef());
                nodesItr.remove();
                return stats;
            }
        }
        return stats;
    }

    private void requestStatistics(){
        AbstractStatsTracker<?, ?> stats = this.getNextRequestFromSchedulerQueue();
        sendStatsRequest(stats);
    }

    void statusUpdated(final TransactionStatus status) {
        AbstractStatsTracker<?, ?> stats = null;
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
                LOG.debug("Pending MD-SAL transactions : {} & Scheduler queue size : {}",this.PendingTransactions,this.requestQueue.size());
                break;
            default:
                break;
            }
        }
        sendStatsRequest(stats);
    }

    private void sendStatsRequest(AbstractStatsTracker<?, ?> stats){
        if(stats != null){
            try{
                stats.request();
                stats.increaseRequestCounter();
            }catch(Exception e){
                LOG.warn("Statistics request was not sent successfully. Reason : {}",e.getMessage());
            }
        }
    }
    public void start(){
        timer.schedule(task, 0, REQUEST_MONITOR_INTERVAL);

    }
}

    class StatChangeListener implements FutureCallback<Optional<? extends DataObject>> {

        StatisticsRequestScheduler scheduler;

        StatChangeListener (final StatisticsRequestScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void onSuccess(Optional<? extends DataObject> result) {
            scheduler.statusUpdated(TransactionStatus.SUBMITED);
        }

        @Override
        public void onFailure(Throwable t) {
            scheduler.statusUpdated(TransactionStatus.FAILED);
        }
    }
