/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.statistics.manager.StatListeningCommiter;
import org.opendaylight.controller.md.statistics.manager.StatNodeRegistration;
import org.opendaylight.controller.md.statistics.manager.StatNotifyCommiter;
import org.opendaylight.controller.md.statistics.manager.StatPermCollector;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
* statistics-manager
* org.opendaylight.controller.md.statistics.manager.impl
*
* StatisticsManagerImpl
* It represent a central point for whole module. Implementation
* {@link StatisticsManager} registers all Operation/DS {@link StatNotifyCommiter} and
* Config/DS {@StatListeningCommiter}, as well as {@link StatPermCollector}
* for statistic collecting and {@link StatRpcMsgManager} as Device RPCs provider.
* In next, StatisticsManager provides all DS contact Transaction services.
*
* @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
*
*/
public class StatisticsManagerImpl implements StatisticsManager, Runnable {

   private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerImpl.class);

   private static final int QUEUE_DEPTH = 500;
   private static final int MAX_BATCH = 100;

   private final BlockingQueue<StatDataStoreOperation> dataStoreOperQueue = new LinkedBlockingDeque<>(QUEUE_DEPTH);

   private final DataBroker dataBroker;
   private final ExecutorService statNetCollectorServ;
   private final ExecutorService statRpcMsgManagerExecutor;
   private final ExecutorService statDataStoreOperationServ;
   private StatRpcMsgManager rpcMsgManager;
   private StatPermCollector statCollector;
   private BindingTransactionChain txChain;
   private volatile boolean finishing = false;

   private StatNodeRegistration nodeRegistrator;
   private StatListeningCommiter<Flow, OpendaylightFlowStatisticsListener> flowListeningCommiter;
   private StatListeningCommiter<Meter, OpendaylightMeterStatisticsListener> meterListeningCommiter;
   private StatListeningCommiter<Group, OpendaylightGroupStatisticsListener> groupListeningCommiter;
   private StatListeningCommiter<Queue, OpendaylightQueueStatisticsListener> queueNotifyCommiter;
   private StatNotifyCommiter<OpendaylightFlowTableStatisticsListener> tableNotifCommiter;
   private StatNotifyCommiter<OpendaylightPortStatisticsListener> portNotifyCommiter;

   public StatisticsManagerImpl (final DataBroker dataBroker) {
       this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
       statNetCollectorServ = Executors.newSingleThreadExecutor();
       statRpcMsgManagerExecutor = Executors.newSingleThreadExecutor();
       statDataStoreOperationServ = Executors.newSingleThreadExecutor();

       txChain =  dataBroker.createTransactionChain(this);
   }

   public String getName() {
       return "StatisticsManagerImpl";
   }

   @Override
   public void start(final NotificationProviderService notifService,
           final RpcConsumerRegistry rpcRegistry, final long minReqNetMonitInt) {
       Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
       rpcMsgManager = new StatRpcMsgManagerImpl(this, rpcRegistry, minReqNetMonitInt);
       statCollector = new StatPermCollectorImpl(this, minReqNetMonitInt);
//       nodeRegistrator = new StatNodeRegistrationImpl(this, dataBroker);
       nodeRegistrator = new StatNodeRegistrationImpl(this, dataBroker, notifService);
       flowListeningCommiter = new StatListenCommitFlow(this, dataBroker, notifService);
       meterListeningCommiter = new StatListenCommitMeter(this, dataBroker, notifService);
       groupListeningCommiter = new StatListenCommitGroup(this, dataBroker, notifService);
       tableNotifCommiter = new StatNotifyCommitTable(this, notifService);
       portNotifyCommiter = new StatNotifyCommitPort(this, notifService);
       queueNotifyCommiter = new StatListenCommitQueue(this, dataBroker, notifService);

       statNetCollectorServ.execute(statCollector);
       statRpcMsgManagerExecutor.execute(rpcMsgManager);
       statDataStoreOperationServ.execute(this);
       LOG.info("Statistics Manager started successfully!");
   }

   @Override
   public void close() throws Exception {
       finishing = true;
       if (nodeRegistrator != null) {
           nodeRegistrator.close();
           nodeRegistrator = null;
       }
       if (flowListeningCommiter != null) {
           flowListeningCommiter.close();
           flowListeningCommiter = null;
       }
       if (meterListeningCommiter != null) {
           meterListeningCommiter.close();
           meterListeningCommiter = null;
       }
       if (groupListeningCommiter != null) {
           groupListeningCommiter.close();
           groupListeningCommiter = null;
       }
       if (tableNotifCommiter != null) {
           tableNotifCommiter.close();
           tableNotifCommiter = null;
       }
       if (portNotifyCommiter != null) {
           portNotifyCommiter.close();
           portNotifyCommiter = null;
       }
       if (queueNotifyCommiter != null) {
           queueNotifyCommiter.close();
           queueNotifyCommiter = null;
       }
       if (statCollector != null) {
           statCollector.close();
           statCollector = null;
       }
       if (rpcMsgManager != null) {
           rpcMsgManager.close();
           rpcMsgManager = null;
       }
       statNetCollectorServ.shutdown();
       statRpcMsgManagerExecutor.shutdown();
       statDataStoreOperationServ.shutdown();
       if (txChain != null) {
           txChain.close();
           txChain = null;
       }
   }

   @Override
   public void enqueue(final StatDataStoreOperation op) {
       try {
           dataStoreOperQueue.put(op);
       } catch (final InterruptedException e) {
           LOG.warn("Failed to enqueue operation {}", op, e);
       }
   }

   @Override
   public void run() {
       /* Neverending cyle - wait for finishing */
       while ( ! finishing) {
           try {
               StatDataStoreOperation op = dataStoreOperQueue.take();
               final ReadWriteTransaction tx = txChain.newReadWriteTransaction();
               LOG.trace("New operations available, starting transaction {}", tx.getIdentifier());

               int ops = 0;
               do {
                   op.applyOperation(tx);

                   ops++;
                   if (ops < MAX_BATCH) {
                       op = dataStoreOperQueue.poll();
                   } else {
                       op = null;
                   }
               } while (op != null);

               LOG.trace("Processed {} operations, submitting transaction {}", ops, tx.getIdentifier());

               final CheckedFuture<Void, TransactionCommitFailedException> result = tx.submit();
               Futures.addCallback(result, new FutureCallback<Void>() {
                   @Override
                   public void onSuccess(final Void aVoid) {
                       //NOOP
                   }

                   @Override
                   public void onFailure(final Throwable throwable) {
                       LOG.info("Transaction {} failed.", tx.getIdentifier(), throwable);
                   }
               });
           }
           catch (final IllegalStateException e) {
               LOG.debug("Stat DataStoreOperation unexpected State!", e);
               txChain.close();
               txChain = dataBroker.createTransactionChain(this);
               cleanDataStoreOperQueue();
           }
           catch (final Exception e) {
               LOG.trace("Stat DataStore Operation executor fail!", e);
           }
       }
       // Drain all events, making sure any blocked threads are unblocked
       cleanDataStoreOperQueue();
   }

   private void cleanDataStoreOperQueue() {
       // Drain all events, making sure any blocked threads are unblocked
       while (! dataStoreOperQueue.isEmpty()) {
           dataStoreOperQueue.poll();
       }
   }

   @Override
   public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
           final Throwable cause) {
       LOG.warn("Failed to export Flow Capable Statistics, Transaction {} failed.",transaction.getIdentifier(),cause);
   }

   @Override
   public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
       // NOOP
   }

   /* Getter internal Statistic Manager Job Classes */
   @Override
   public StatRpcMsgManager getRpcMsgManager() {
       return rpcMsgManager;
   }

   @Override
   public StatPermCollector getStatCollector() {
       return statCollector;
   }

   @Override
   public StatNodeRegistration getNodeRegistrator() {
       return nodeRegistrator;
   }

   @Override
   public StatListeningCommiter<Flow, OpendaylightFlowStatisticsListener> getFlowListenComit() {
       return flowListeningCommiter;
   }

   @Override
   public StatListeningCommiter<Meter, OpendaylightMeterStatisticsListener> getMeterListenCommit() {
       return meterListeningCommiter;
   }

   @Override
   public StatListeningCommiter<Group, OpendaylightGroupStatisticsListener> getGroupListenCommit() {
       return groupListeningCommiter;
   }

   @Override
   public StatListeningCommiter<Queue, OpendaylightQueueStatisticsListener> getQueueNotifyCommit() {
       return queueNotifyCommiter;
   }


   @Override
   public StatNotifyCommiter<OpendaylightFlowTableStatisticsListener> getTableNotifCommit() {
       return tableNotifCommiter;
   }

   @Override
   public StatNotifyCommiter<OpendaylightPortStatisticsListener> getPortNotifyCommit() {
       return portNotifyCommiter;
   }
}

