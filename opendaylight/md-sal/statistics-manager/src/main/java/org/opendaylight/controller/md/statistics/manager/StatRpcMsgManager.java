/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import com.google.common.base.Optional;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.statistics.manager.impl.TaskRunManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 * <p/>
 * StatRpcMsgManager
 * It represent access point for Device statistics RPC services which are
 * filtered for needed methods only and they are wrapped in simply way.
 * Many statistics responses are Multipart messages, so StatRpcMsgManager
 * provide a functionality to add all multipart msg and provides back whole
 * stack to listener when listener catch the last Multipart msg.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *         <p/>
 *         Created: Aug 29, 2014
 */
public interface StatRpcMsgManager extends TaskRunManager, Runnable, AutoCloseable {

    interface RpcJobsQueue extends Callable<Void> {
    }

    /**
     * Transaction container is definition for Multipart transaction
     * join container for all Multipart msg with same TransactionId
     * Input {@link DataObject} is a possible light-weight DataObject
     * which is used for identification (e.g. Flow-> Priority,Match,Cookie,FlowId)
     *
     * @param <T> extends TransactionAware -
     */
    interface TransactionCacheContainer<T extends TransactionAware> {

        void addNotif(T notification);

        TransactionId getId();

        NodeId getNodeId();

        Optional<? extends DataObject> getConfInput();

        List<T> getNotifications();
    }

    /**
     * Method is used for check a transaction registration
     * for multipart cache holder
     *
     * @param TransactionId id
     * @return true if the transaction has been correctly registered
     */
    Future<Boolean> isExpectedStatistics(TransactionId id, NodeId nodeId);

    /**
     * Method converts {@link java.util.concurrent.Future} object to listenenable future which
     * is registered for Multipart Notification Statistics Collecting processing.
     *
     * @param future - result every Device RPC call
     */
    <T extends TransactionAware, D extends DataObject> void registrationRpcFutureCallBack(Future<RpcResult<T>> future, D inputObj, NodeRef ref);

    /**
     * Method adds Notification which is marked as Multipart to the transaction cash
     * to wait for the last one.
     *
     * @param notification
     */
    <T extends TransactionAware> void addNotification(T notification, NodeId nodeId);

    /**
     * The last Multipart should inform code about possibility to take all previous
     * messages for next processing. The method take all msg and possible input object
     * and build all to TransactionCacheContainer Object to return. This process clean
     * all instances in Cache.
     *
     * @param TransactionId id
     * @return TransactionCacheContainer
     */
    Future<Optional<TransactionCacheContainer<?>>> getTransactionCacheContainer(TransactionId id, NodeId nodeId);

    /**
     * Method wraps OpendaylightGroupStatisticsService.getAllGroupStatistics
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllGroupsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightGroupStatisticsService.getGroupDescription
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllGroupsConfStats(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getGroupFeatures
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getGroupFeaturesStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getAllMeterStatistics
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllMetersStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getAllMeterConfigStatistics
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllMeterConfigStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getMeterFeatures
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getMeterFeaturesStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightFlowStatisticsService.getAllFlowsStatisticsFromAllFlowTables
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllFlowsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightFlowStatisticsService.getAggregateFlowStatisticsFromFlowTableForAllFlows
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     * @param TableId tableId
     */
    void getAggregateFlowStat(NodeRef nodeRef, TableId tableId);

    /**
     * Method wraps OpendaylightPortStatisticsService.getAllNodeConnectorsStatistics
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllPortsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightFlowTableStatisticsService.getFlowTablesStatistics
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllTablesStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightQueueStatisticsService.getAllQueuesStatisticsFromAllPorts
     * and registers to Transaction Cache
     *
     * @param NodeRef nodeRef
     */
    void getAllQueueStat(NodeRef nodeRef);


}

