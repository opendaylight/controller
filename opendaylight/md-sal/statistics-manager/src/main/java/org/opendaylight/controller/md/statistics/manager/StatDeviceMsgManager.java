/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.MultipartTransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatDeviceMsgManager
 * It represent access point for Device statistics RPC services which are
 * filtered for needed methods only and they are wrapped in simply way.
 * Many statistics responses are Multipart messages, so StatDeviceMsgManager
 * provide a functionality to add all multipart msgs and set back to
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 29, 2014
 */
public interface StatDeviceMsgManager extends AutoCloseable {

    /**
     * Transaction container is definition for Multipart transaction
     * join container for all Multipart msg with same TransactionId
     * Input {@link DataObject} is a possible light-weight DataObject
     * which is used for identification (e.g. Flow-> Priority,Match,Cookie,FlowId)
     *
     * @param <T> extends MultipartTransactionAware -
     */
    interface TransactionCacheContainer<T extends MultipartTransactionAware> {

        TransactionId getId();

        void addInput(Optional<DataObject> input);

        void addNotif(TransactionAware notif);

        Optional<DataObject> getConfInput();

        List<? extends TransactionAware> getNotifications();
    }

    /**
     * Method is used for check a transaction registration
     * for multipart cache holder
     *
     * @param TransactionId id
     * @return true if the transaction has been correctly registred
     */
    boolean isExpectedStatistics(TransactionId id);

    /**
     * Method converts {@link java.util.concurrent.Future} object to listenenable future which
     * is registered for Multipart Notification Statistics Collecting processing.
     *
     * @param future - result every Device RPC call
     */
    <T extends TransactionAware> void registrationRpcFutureCallBack(final Future<RpcResult<T>> future);

    /**
     * Method registers input {@link DataObject} to the Multipart Notification Statistic
     * Transaction identified by TransactionId
     *
     * @param id
     * @param data
     */
    <T extends DataObject>void registerConfigDataToTransaction(TransactionId id, T data);

    /**
     * Method adds Notification which is marked as Multipart to the transaction cash
     * to wait for the last one.
     *
     * @param notification
     */
    <T extends TransactionAware> void addNotification(T notification);

    /**
     * The last Multipart should inform code about possibility to take all previous
     * messages for next processing. The method take all msg and possible input object
     * and build all to TransactionCacheContainer Object to return. This process clean
     * all instances in Cache.
     *
     * @param TransactionId id
     * @return TransactionCacheContainer
     */
    Optional<TransactionCacheContainer<?>> getTransactionCacheContainer(TransactionId id);

    /**
     * Method wraps OpendaylightGroupStatisticsService.getAllGroupStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllGroupsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightGroupStatisticsService.getGroupDescription
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllGroupsConfStats(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightGroupStatisticsService.getGroupStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getGroupStat(NodeRef nodeRef, GroupId groupId);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getAllMeterStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllMetersStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getAllMeterConfigStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllMeterConfigStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightMeterStatisticsService.getMeterStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getMeterStat(NodeRef nodeRef, MeterId meterId);


    /**
     * Method wraps OpendaylightFlowStatisticsService.getAllFlowsStatisticsFromAllFlowTables
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllFlowsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightFlowStatisticsService.getFlowStatisticsFromFlowTable
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getFlowStat(NodeRef nodeRef, Flow lightWeightedFlow);

    /**
     * Method wraps OpendaylightFlowStatisticsService.getAggregateFlowStatisticsFromFlowTableForAllFlows
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAggregateFlowStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightPortStatisticsService.getAllNodeConnectorsStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllPortsStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightFlowTableStatisticsService.getFlowTablesStatistics
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllTablesStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightQueueStatisticsService.getAllQueuesStatisticsFromAllPorts
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getAllQueueStat(NodeRef nodeRef);

    /**
     * Method wraps OpendaylightPortStatisticsService.getQueueStatisticsFromGivenPort
     * and registers to Transaction Cashe
     *
     * @param NodeRef nodeRef
     */
    void getQueueStatForGivenPort(NodeRef nodeRef,
            NodeConnectorId nodeConnectorId, QueueId queueId);

}

