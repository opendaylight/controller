/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager;
import org.opendaylight.controller.md.statistics.manager.StatListeningCommiter;
import org.opendaylight.controller.md.statistics.manager.StatNotifyCommiter;
import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
* statistics-manager
* org.opendaylight.controller.md.statistics.manager.impl
*
* StatisticsManagerImpl
* It represent a central point for whole module. Implementation
* StatisticsManager registers all Operation/DS {@link StatNotifyCommiter} and
* Config/DS {@StatListeningCommiter}, as well as {@link StatRepeatedlyEnforcer}
* for statistic collecting and {@StatDeviceCollecto} as Device RPCs provider.
* In next, StatisticsManager provides all DS contact Transaction services.
*
* @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
*
*/
public class StatisticsManagerImpl implements StatisticsManager {

   private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerImpl.class);

   private final DataBroker dataBroker;
   private final ExecutorService statNetCollectorServ;
   private StatDeviceMsgManager deviceMsgManager;
   private StatRepeatedlyEnforcer repeatedlyEnforcer;

   private StatListeningCommiter<FlowCapableNode> nodeRegistrator;
   private StatListeningCommiter<Flow> flowListeningCommiter;
   private StatListeningCommiter<Meter> meterListeningCommiter;
   private StatListeningCommiter<Group> groupListeningCommiter;
   private StatNotifyCommiter<OpendaylightFlowTableStatisticsListener> tableNotifCommiter;
   private StatNotifyCommiter<OpendaylightPortStatisticsListener> portNotifyCommiter;
   private StatNotifyCommiter<OpendaylightQueueStatisticsListener> queueNotifyCommiter;

   public StatisticsManagerImpl (final DataBroker dataBroker) {
       this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
       statNetCollectorServ = Executors.newSingleThreadExecutor();
   }

   @Override
   public void start(final NotificationProviderService notifService,
           final RpcConsumerRegistry rpcRegistry, final long minReqNetMonitInt) {
       Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
       deviceMsgManager = new StatDeviceMsgManagerImpl(this, rpcRegistry, minReqNetMonitInt);
       repeatedlyEnforcer = new StatRepeatedlyEnforcerImpl(StatisticsManagerImpl.this, minReqNetMonitInt);
       nodeRegistrator = new StatNodeRegistImpl(this, dataBroker);
       flowListeningCommiter = new StatListenCommitFlow(this, dataBroker, notifService);
       meterListeningCommiter = new StatListenCommitMeter(this, dataBroker, notifService);
       groupListeningCommiter = new StatListenCommitGroup(this, dataBroker, notifService);
       tableNotifCommiter = new StatNotifyCommitTable(this, notifService);
       portNotifyCommiter = new StatNotifyCommitPort(this, notifService);
       queueNotifyCommiter = new StatListenCommitQueue(this, dataBroker, notifService);

       statNetCollectorServ.execute(repeatedlyEnforcer);
       LOG.info("Statistics Manager started successfully!");
   }

   @Override
   public void close() throws Exception {
       statNetCollectorServ.shutdown();
       if (repeatedlyEnforcer != null) {
           repeatedlyEnforcer = null;
       }
       if (deviceMsgManager != null) {
           deviceMsgManager.close();
           deviceMsgManager = null;
       }
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
   }

   @Override
   public ReadWriteTransaction getReadWriteTransaction() {
       return dataBroker.newReadWriteTransaction();
   }

   @Override
   public WriteTransaction getWriteTransaction() {
       return dataBroker.newWriteOnlyTransaction();
   }

   @Override
   public ReadTransaction getReadTransaction() {
       return dataBroker.newReadOnlyTransaction();
   }

   /* Getter internal Statistic Manager Job Classes */
   @Override
   public StatDeviceMsgManager getDeviceMsgManager() {
       return deviceMsgManager;
   }

   @Override
   public StatRepeatedlyEnforcer getRepeatedlyEnforcer() {
       return repeatedlyEnforcer;
   }

   @Override
   public StatListeningCommiter<FlowCapableNode> getFlowNodeListenRegistrator() {
       return nodeRegistrator;
   }

   @Override
   public StatListeningCommiter<Flow> getFlowListenComit() {
       return flowListeningCommiter;
   }

   @Override
   public StatListeningCommiter<Meter> getMeterListenCommit() {
       return meterListeningCommiter;
   }

   @Override
   public StatListeningCommiter<Group> getGroupListenCommit() {
       return groupListeningCommiter;
   }

   @Override
   public StatNotifyCommiter<OpendaylightFlowTableStatisticsListener> getTableNotifCommit() {
       return tableNotifCommiter;
   }

   @Override
   public StatNotifyCommiter<OpendaylightPortStatisticsListener> getPortNotifyCommit() {
       return portNotifyCommiter;
   }

   @Override
   public synchronized final StatNotifyCommiter<OpendaylightQueueStatisticsListener> getQueueNotifyCommit() {
       return queueNotifyCommiter;
   }
}

