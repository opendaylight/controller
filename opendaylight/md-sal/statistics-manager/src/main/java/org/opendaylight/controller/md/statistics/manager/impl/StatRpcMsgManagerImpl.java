/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 * <p/>
 * StatRpcMsgManagerImpl
 * Class register and provide all RPC Statistics Device Services and implement pre-defined
 * wrapped methods for prepare easy access to RPC Statistics Device Services like getAllStatisticsFor...
 * <p/>
 * In next Class implement process for joining multipart messages.
 * Class internally use two WeakHashMap and GuavaCache for holding values for joining multipart msg.
 * One Weak map is used for holding all Multipart Messages and second is used for possible input
 * Config/DS light-weight DataObject (DataObject contains only necessary identification fields as
 * TableId, GroupId, MeterId or for flow Match, Priority, FlowCookie, TableId and FlowId ...
 *
 * @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 */
public class StatRpcMsgManagerImpl implements StatRpcMsgManager {

    private final static Logger LOG = LoggerFactory.getLogger(StatRpcMsgManagerImpl.class);

    private final Cache<String, TransactionCacheContainer<? super TransactionAware>> txCache;

    private final long maxLifeForRequest = 50; /* 50 second */
    private final int queueCapacity = 5000;

    private final OpendaylightGroupStatisticsService groupStatsService;
    private final OpendaylightMeterStatisticsService meterStatsService;
    private final OpendaylightFlowStatisticsService flowStatsService;
    private final OpendaylightPortStatisticsService portStatsService;
    private final OpendaylightFlowTableStatisticsService flowTableStatsService;
    private final OpendaylightQueueStatisticsService queueStatsService;

    private BlockingQueue<RpcJobsQueue> statsRpcJobQueue;
    private boolean sleep = false;

    private volatile boolean finishing = false;

    public StatRpcMsgManagerImpl(final StatisticsManager manager,
                                 final RpcConsumerRegistry rpcRegistry, final long minReqNetMonitInt) {
        Preconditions.checkArgument(manager != null, "StatisticManager can not be null!");
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
        groupStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightGroupStatisticsService.class),
                "OpendaylightGroupStatisticsService can not be null!");
        meterStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightMeterStatisticsService.class),
                "OpendaylightMeterStatisticsService can not be null!");
        flowStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class),
                "OpendaylightFlowStatisticsService can not be null!");
        portStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightPortStatisticsService.class),
                "OpendaylightPortStatisticsService can not be null!");
        flowTableStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightFlowTableStatisticsService.class),
                "OpendaylightFlowTableStatisticsService can not be null!");
        queueStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightQueueStatisticsService.class),
                "OpendaylightQueueStatisticsService can not be null!");

        statsRpcJobQueue = new LinkedBlockingQueue<>(queueCapacity);
        txCache = CacheBuilder.newBuilder().expireAfterWrite(maxLifeForRequest, TimeUnit.SECONDS)
                .maximumSize(10000).build();
    }

    @Override
    public void close() {
        finishing = true;
        statsRpcJobQueue = null;
    }

    @Override
    public void run() {
         /* Neverending cyle - wait for finishing */
        while (!finishing) {
            while (sleep) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOG.debug("Can't sleep {}", e.getMessage());
                    }
                }
            }
            try {
                statsRpcJobQueue.take().call();
            } catch (final Exception e) {
                LOG.warn("Stat Element RPC executor fail!", e);
            }

        }
        // Drain all rpcCall, making sure any blocked threads are unblocked
        while (!statsRpcJobQueue.isEmpty()) {
            statsRpcJobQueue.poll();
        }
    }

    private void addGetAllStatJob(final RpcJobsQueue getAllStatJob) {
        final boolean success = statsRpcJobQueue.offer(getAllStatJob);
        if (!success) {
            LOG.warn("Put RPC request getAllStat fail! Queue is full.");
        }
    }

    private void addStatJob(final RpcJobsQueue getStatJob) {
        final boolean success = statsRpcJobQueue.offer(getStatJob);
        if (!success) {
            LOG.debug("Put RPC request for getStat fail! Queue is full.");
        }
    }

    @Override
    public <T extends TransactionAware, D extends DataObject> void registrationRpcFutureCallBack(
            final Future<RpcResult<T>> future, final D inputObj, final NodeRef nodeRef) {

        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future),
                new FutureCallback<RpcResult<? extends TransactionAware>>() {

                    @Override
                    public void onSuccess(final RpcResult<? extends TransactionAware> result) {
                        final TransactionId id = result.getResult().getTransactionId();
                        if (id == null) {
                            LOG.warn("No protocol support");
                        } else {
                            final NodeKey nodeKey = nodeRef.getValue().firstKeyOf(Node.class, NodeKey.class);
                            final String cacheKey = buildCacheKey(id, nodeKey.getId());
                            final TransactionCacheContainer<? super TransactionAware> container =
                                    new TransactionCacheContainerImpl<>(id, inputObj, nodeKey.getId());
                            txCache.put(cacheKey, container);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        LOG.warn("Response Registration for Statistics RPC call fail!", t);
                    }

                });
    }

    private String buildCacheKey(final TransactionId id, final NodeId nodeId) {
        return String.valueOf(id.getValue()) + "-" + nodeId.getValue();
    }

    @Override
    public Future<Optional<TransactionCacheContainer<?>>> getTransactionCacheContainer(
            final TransactionId id, final NodeId nodeId) {
        Preconditions.checkArgument(id != null, "TransactionId can not be null!");
        Preconditions.checkArgument(nodeId != null, "NodeId can not be null!");

        final String key = buildCacheKey(id, nodeId);
        final SettableFuture<Optional<TransactionCacheContainer<?>>> result = SettableFuture.create();

        final RpcJobsQueue getTransactionCacheContainer = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final Optional<TransactionCacheContainer<?>> resultContainer =
                        Optional.<TransactionCacheContainer<?>>fromNullable(txCache.getIfPresent(key));
                if (resultContainer.isPresent()) {
                    txCache.invalidate(key);
                }
                result.set(resultContainer);
                return null;
            }
        };
        addStatJob(getTransactionCacheContainer);
        return result;
    }

    @Override
    public Future<Boolean> isExpectedStatistics(final TransactionId id, final NodeId nodeId) {
        Preconditions.checkArgument(id != null, "TransactionId can not be null!");
        Preconditions.checkArgument(nodeId != null, "NodeId can not be null!");

        final String key = buildCacheKey(id, nodeId);
        final SettableFuture<Boolean> checkStatId = SettableFuture.create();

        final RpcJobsQueue isExpecedStatistics = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final Optional<TransactionCacheContainer<?>> result =
                        Optional.<TransactionCacheContainer<?>>fromNullable(txCache.getIfPresent(key));
                checkStatId.set(Boolean.valueOf(result.isPresent()));
                return null;
            }
        };
        addStatJob(isExpecedStatistics);
        return checkStatId;
    }

    @Override
    public void addNotification(final TransactionAware notification, final NodeId nodeId) {
        Preconditions.checkArgument(notification != null, "TransactionAware can not be null!");
        Preconditions.checkArgument(nodeId != null, "NodeId can not be null!");

        final RpcJobsQueue addNotification = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final TransactionId txId = notification.getTransactionId();
                final String key = buildCacheKey(txId, nodeId);
                final TransactionCacheContainer<? super TransactionAware> container = (txCache.getIfPresent(key));
                if (container != null) {
                    container.addNotif(notification);
                }
                return null;
            }
        };
        addStatJob(addNotification);
    }

    @Override
    public void getAllGroupsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllGroupStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllGroupStatisticsInputBuilder builder =
                        new GetAllGroupStatisticsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(groupStatsService
                        .getAllGroupStatistics(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllGroupStat);
    }

    @Override
    public void getAllMetersStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllMeterStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllMeterStatisticsInputBuilder builder =
                        new GetAllMeterStatisticsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(meterStatsService
                        .getAllMeterStatistics(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllMeterStat);
    }

    @Override
    public void getAllFlowsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllFlowStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder builder =
                        new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(flowStatsService
                        .getAllFlowsStatisticsFromAllFlowTables(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllFlowStat);
    }

    @Override
    public void getAggregateFlowStat(final NodeRef nodeRef, final TableId tableId) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        Preconditions.checkArgument(tableId != null, "TableId can not be null!");
        final RpcJobsQueue getAggregateFlowStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder builder =
                        new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();
                builder.setNode(nodeRef);
                builder.setTableId(tableId);

                final TableBuilder tbuilder = new TableBuilder();
                tbuilder.setId(tableId.getValue());
                tbuilder.setKey(new TableKey(tableId.getValue()));
                registrationRpcFutureCallBack(flowStatsService
                        .getAggregateFlowStatisticsFromFlowTableForAllFlows(builder.build()), tbuilder.build(), nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAggregateFlowStat);
    }

    @Override
    public void getAllPortsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllPortsStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllNodeConnectorsStatisticsInputBuilder builder =
                        new GetAllNodeConnectorsStatisticsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(portStatsService
                        .getAllNodeConnectorsStatistics(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllPortsStat);
    }

    @Override
    public void getAllTablesStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllTableStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetFlowTablesStatisticsInputBuilder builder =
                        new GetFlowTablesStatisticsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(flowTableStatsService
                        .getFlowTablesStatistics(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllTableStat);
    }

    @Override
    public void getAllQueueStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllQueueStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllQueuesStatisticsFromAllPortsInputBuilder builder =
                        new GetAllQueuesStatisticsFromAllPortsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(queueStatsService
                        .getAllQueuesStatisticsFromAllPorts(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(getAllQueueStat);
    }

    @Override
    public void sleep() {
        sleep = true;
    }

    @Override
    public void wakeUp() {
        if (sleep) {
            synchronized (this) {
                sleep = false;
                notify();
            }
        }
    }

    @Override
    public void getAllMeterConfigStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue qetAllMeterConfStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetAllMeterConfigStatisticsInputBuilder builder =
                        new GetAllMeterConfigStatisticsInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(meterStatsService
                        .getAllMeterConfigStatistics(builder.build()), null, nodeRef);
                return null;
            }
        };
        addGetAllStatJob(qetAllMeterConfStat);
    }

    @Override
    public void getGroupFeaturesStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getGroupFeaturesStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                /* RPC input */
                final GetGroupFeaturesInputBuilder input = new GetGroupFeaturesInputBuilder();
                input.setNode(nodeRef);
                registrationRpcFutureCallBack(groupStatsService.getGroupFeatures(input.build()), null, nodeRef);
                return null;
            }
        };
        addStatJob(getGroupFeaturesStat);
    }

    @Override
    public void getMeterFeaturesStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getMeterFeaturesStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                /* RPC input */
                final GetMeterFeaturesInputBuilder input = new GetMeterFeaturesInputBuilder();
                input.setNode(nodeRef);
                registrationRpcFutureCallBack(meterStatsService.getMeterFeatures(input.build()), null, nodeRef);
                return null;
            }
        };
        addStatJob(getMeterFeaturesStat);
    }

    @Override
    public void getAllGroupsConfStats(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final RpcJobsQueue getAllGropConfStat = new RpcJobsQueue() {

            @Override
            public Void call() throws Exception {
                final GetGroupDescriptionInputBuilder builder =
                        new GetGroupDescriptionInputBuilder();
                builder.setNode(nodeRef);
                registrationRpcFutureCallBack(groupStatsService
                        .getGroupDescription(builder.build()), null, nodeRef);

                return null;
            }
        };
        addGetAllStatJob(getAllGropConfStat);
    }

    public class TransactionCacheContainerImpl<T extends TransactionAware> implements TransactionCacheContainer<T> {

        private final TransactionId id;
        private final NodeId nId;
        private final List<T> notifications;
        private final Optional<? extends DataObject> confInput;

        public <D extends DataObject> TransactionCacheContainerImpl(final TransactionId id, final D input, final NodeId nodeId) {
            this.id = Preconditions.checkNotNull(id, "TransactionId can not be null!");
            notifications = new CopyOnWriteArrayList<T>();
            confInput = Optional.fromNullable(input);
            nId = nodeId;
        }

        @Override
        public void addNotif(final T notif) {
            notifications.add(notif);
        }

        @Override
        public TransactionId getId() {
            return id;
        }

        @Override
        public NodeId getNodeId() {
            return nId;
        }

        @Override
        public List<T> getNotifications() {
            return notifications;
        }

        @Override
        public Optional<? extends DataObject> getConfInput() {
            return confInput;
        }
    }
}

