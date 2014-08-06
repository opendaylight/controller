/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.MultipartTransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;


/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatDeviceMsgManagerImpl
 * Class register and provide all RPC Statistics Device Services and implement pre-defined
 * wrapped methods for prepare easy access to RPC Statistics Device Services like getAllStatisticsFor...
 *
 * In next Class implement process for joining multipart messages.
 * Class internally use two WeakHashMap and GuavaCache for holding values for joining multipart msg.
 * One Weak map is used for holding all Multipart Messages and second is used for possible input
 * Config/DS light-weight DataObject (DataObject contains only necessary identification fields as
 * TableId, GroupId, MeterId or for flow Match, Priority, FlowCookie, TableId and FlowId ...
 *
 * @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatDeviceMsgManagerImpl implements StatDeviceMsgManager {

    private final static Logger LOG = LoggerFactory.getLogger(StatDeviceMsgManagerImpl.class);



    private final Map<TransactionId, List<? extends TransactionAware>> txMultiparts =
            Collections.synchronizedMap(new WeakHashMap<TransactionId, List<? extends TransactionAware>>());
    private final Map<TransactionId, DataObject> txRegistredResp =
            Collections.synchronizedMap(new WeakHashMap<TransactionId, DataObject>());
    private LoadingCache<TransactionId, TransactionCacheContainer<? extends TransactionAware>> txCache;

    private FutureCallback<RpcResult<? extends TransactionAware>> callback;

    private final long maxLifeForRequest;
    private final StatisticsManager manager;

    private ListenerRegistration<NotificationListener> notifListener;

    private final OpendaylightGroupStatisticsService groupStatsService;
    private final OpendaylightMeterStatisticsService meterStatsService;
    private final OpendaylightFlowStatisticsService flowStatsService;
    private final OpendaylightPortStatisticsService portStatsService;
    private final OpendaylightFlowTableStatisticsService flowTableStatsService;
    private final OpendaylightQueueStatisticsService queueStatsService;


    public StatDeviceMsgManagerImpl (final StatisticsManager manager,
            final RpcConsumerRegistry rpcRegistry, final long minReqNetMonitInt) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticManager can not be null!");
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
        maxLifeForRequest = 2 * minReqNetMonitInt;
        buildStatisticsRpcResponseCache();
        buildCallBackRpcRequestRegistrator();
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
    }

    @Override
    public void close() {
        if (notifListener != null) {
            try {
                notifListener.close();
            }
            catch (final Exception e) {
                LOG.error("StatDeviceCommiter fail by closing!", e);
            }
            notifListener = null;
        }
    }

    @Override
    public <T extends TransactionAware> void registrationRpcFutureCallBack(
            final Future<RpcResult<T>> future) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future),callback);
    }

    @Override
    public Optional<TransactionCacheContainer<?>> getTransactionCacheContainer(
            final TransactionId id) {
        Optional<TransactionCacheContainer<?>> result = Optional.absent();
        try {
            result = Optional.<TransactionCacheContainer<?>> of(txCache.get(id));
        }
        catch (final Exception e) {
            LOG.error("Get joined Multipart msg fail!", e);
        }
        txCache.invalidate(id);
        return result;
    }

    @Override
    public <T extends DataObject> void registerConfigDataToTransaction(
            final TransactionId id, final T data) {
        txMultiparts.put(id, Collections.<TransactionAware> emptyList());
        txRegistredResp.put(id, data);
        try {
            txCache.get(id);
        }
        catch (final ExecutionException e) {
            LOG.error("Registration resp. with additional Light-Weight object fail!", e);
        }
    }

    @Override
    public boolean isExpectedStatistics(final TransactionId id) {
        return txMultiparts.containsKey(id);
    }

    @Override
    public <T extends TransactionAware> void addNotification(final T notification) {
        final TransactionId txId = notification.getTransactionId();
        if (txMultiparts.containsKey(txId)) {
            final List<? extends TransactionAware> actList = txMultiparts.get(txId);

            final List<? super TransactionAware> newList = actList != null
                    ? new ArrayList<>(actList) : new ArrayList<TransactionAware>(10);

            newList.add(notification);
            txMultiparts.put(txId, Collections.<TransactionAware> unmodifiableList(actList));
        } else {
            txMultiparts.put(txId, Collections.<TransactionAware> singletonList(notification));
            try {
                txCache.get(txId);
            }
            catch (final ExecutionException e) {
                LOG.warn("Multipart Stat msg {} registration to cache fail!", txId, e);
            }
        }
    }

    /*
     * Callback for all Device RPC Futures
     * Method registers TransactionId because without registration, statistic notification
     * payload will not by add to Operational DataStore
     */
    private void buildCallBackRpcRequestRegistrator() {
        callback = new FutureCallback<RpcResult<? extends TransactionAware>>() {
            @Override
            public void onSuccess(final RpcResult<? extends TransactionAware> result) {
                if (result.isSuccessful()) {
                    final TransactionId id = result.getResult().getTransactionId();
                    if (id == null) {
                        final Throwable t = new UnsupportedOperationException("No protocol support");
                        t.fillInStackTrace();
                        onFailure(t);
                    } else {
                        txMultiparts.put(id, Collections.<TransactionAware> emptyList());
                        try {
                            txCache.get(id);
                        }
                        catch (final ExecutionException e) {
                            LOG.warn("Multipart Stat msg {} registration to cache fail!", id, e);
                        }
                    }
                } else {
                    LOG.debug("Statistics request failed: {}", result.getErrors());

                    final Throwable t = new StatRPCFailedException("Failed to send statistics request", result.getErrors());
                    t.fillInStackTrace();
                    onFailure(t);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.debug("Failed to send statistics request", t);
            }
        };
    }


    private void buildStatisticsRpcResponseCache() {
        txCache = CacheBuilder.newBuilder().expireAfterWrite(maxLifeForRequest, TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .build(new CacheLoader<TransactionId, TransactionCacheContainer<? extends TransactionAware>>() {
                    @Override
                    public TransactionCacheContainer<? extends TransactionAware> load(final TransactionId key) {
                        final TransactionCacheContainer<? extends TransactionAware> txContainer =
                                new TransactionCacheContainerImpl<>(key);
                        if (txMultiparts.containsKey(key)) {
                            final List<? extends TransactionAware> notifList = txMultiparts.get(key);
                            for (final TransactionAware notif : notifList) {
                                txContainer.addNotif(notif);
                            }
                        }
                        if (txRegistredResp.containsKey(key)) {
                            final Optional<DataObject> confInp = Optional.<DataObject> of(txRegistredResp.get(key));
                            txContainer.addInput(confInp);
                        }
                        return txContainer;
                    }
                });
    }

    class TransactionCacheContainerImpl<T extends MultipartTransactionAware> implements TransactionCacheContainer<T> {

        private final TransactionId id;
        private final List<T> notifications;
        private Optional<DataObject> confInput;

        public TransactionCacheContainerImpl (final TransactionId id) {
            this.id = Preconditions.checkNotNull(id, "TransactionId can not be null!");
            notifications = new ArrayList<>();
            confInput = Optional.absent();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void addNotif(final TransactionAware notif) {
            notifications.add((T) notif);
        }

        @Override
        public void addInput(final Optional<DataObject> input) {
            confInput = input;
        }

        @Override
        public TransactionId getId() {
            return id;
        }

        @Override
        public List<? extends TransactionAware> getNotifications() {
            return notifications;
        }

        @Override
        public Optional<DataObject> getConfInput() {
            return confInput;
        }
    }

    @Override
    public void getAllGroupsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllGroupStatisticsInputBuilder builder =
                new GetAllGroupStatisticsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(groupStatsService.getAllGroupStatistics(builder.build()));
    }

    @Override
    public void getAllMetersStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllMeterStatisticsInputBuilder builder =
                new GetAllMeterStatisticsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(meterStatsService.getAllMeterStatistics(builder.build()));
    }

    @Override
    public void getAllFlowsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder builder =
                new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(flowStatsService
                .getAllFlowsStatisticsFromAllFlowTables(builder.build()));
    }

    @Override
    public void getAggregateFlowStat(final NodeRef nodeRef) {
        final ReadTransaction trans = manager.getReadTransaction();

        @SuppressWarnings("unchecked")
        final InstanceIdentifier<FlowCapableNode> nodeIdent =
                (InstanceIdentifier<FlowCapableNode>) nodeRef.getValue();

        final InstanceIdentifier<SwitchFeatures> switchFeaturIdent =
                nodeIdent.child(SwitchFeatures.class);
        Optional<SwitchFeatures> features = Optional.absent();
        try {
            features = trans.read(LogicalDatastoreType.OPERATIONAL, switchFeaturIdent).get();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Read SwitchFeatures {} fail!", switchFeaturIdent);
        }
        if ( ! features.isPresent()) {
           return;
        }
        final short cyles = features.get().getMaxTables().shortValue();
        for (short i = 0; i < cyles; i++) {
            final GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder builder =
                    new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();
            builder.setNode(nodeRef);
            builder.setTableId(new TableId(i));

            final TableBuilder tbuilder = new TableBuilder();
            tbuilder.setId(i);
            final Table table = tbuilder.build();

            try {
                final RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput> resp =
                        flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(builder.build()).get();
                final TransactionId trnsId = resp.getResult().getTransactionId();
                registerConfigDataToTransaction(trnsId, table);
            }
            catch (InterruptedException | ExecutionException e) {
                LOG.error("Read Aggreagete Flow Stat Fail!", e);
            }
        }
    }

    @Override
    public void getAllPortsStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllNodeConnectorsStatisticsInputBuilder builder =
                new GetAllNodeConnectorsStatisticsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(portStatsService
                .getAllNodeConnectorsStatistics(builder.build()));
    }

    @Override
    public void getAllTablesStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetFlowTablesStatisticsInputBuilder builder =
                new GetFlowTablesStatisticsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(flowTableStatsService
                .getFlowTablesStatistics(builder.build()));
    }

    @Override
    public void getAllQueueStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllQueuesStatisticsFromAllPortsInputBuilder builder =
                new GetAllQueuesStatisticsFromAllPortsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(queueStatsService
                .getAllQueuesStatisticsFromAllPorts(builder.build()));
    }

    @Override
    public void getAllMeterConfigStat(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetAllMeterConfigStatisticsInputBuilder builder =
                new GetAllMeterConfigStatisticsInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(meterStatsService
                .getAllMeterConfigStatistics(builder.build()));
    }

    @Override
    public void getGroupStat(final NodeRef nodeRef, final GroupId groupId) {
        final GetGroupStatisticsInputBuilder inpBuil = new GetGroupStatisticsInputBuilder();
        inpBuil.setGroupId(groupId);
        inpBuil.setNode(nodeRef);
        final GroupBuilder groupBl = new GroupBuilder();
        groupBl.setGroupId(groupId);

        try {
            final RpcResult<GetGroupStatisticsOutput> resp =
                    groupStatsService.getGroupStatistics(inpBuil.build()).get();
            final TransactionId trnsId = resp.getResult().getTransactionId();
            registerConfigDataToTransaction(trnsId, groupBl.build());
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Read Group {} Stat for Node {} from Device Fail!",groupId, nodeRef, e);
        }
    }

    @Override
    public void getMeterStat(final NodeRef nodeRef, final MeterId meterId) {
        /* make light-weight DataObject for identification */
        final MeterBuilder mBuilder = new MeterBuilder();
        mBuilder.setMeterId(meterId);
        final Meter lighMeter = mBuilder.build();
        /* RPC input */
        final GetMeterStatisticsInputBuilder input = new GetMeterStatisticsInputBuilder();
        input.setNode(nodeRef);
        input.setMeterId(meterId);

        try {
            final RpcResult<GetMeterStatisticsOutput> resp =
                    meterStatsService.getMeterStatistics(input.build()).get();
            final TransactionId trnsId = resp.getResult().getTransactionId();
            registerConfigDataToTransaction(trnsId, lighMeter);
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Read Meter {} Stat for Node {} from Device Fail!", meterId, nodeRef, e);
        }
    }

    @Override
    public void getFlowStat(final NodeRef nodeRef, final Flow lightWeightedFlow) {
        final GetFlowStatisticsFromFlowTableInputBuilder inputBuilder =
                new GetFlowStatisticsFromFlowTableInputBuilder(lightWeightedFlow);
        inputBuilder.setNode(new NodeRef(nodeRef));

        try {
            final RpcResult<GetFlowStatisticsFromFlowTableOutput> resp =
                    flowStatsService.getFlowStatisticsFromFlowTable(inputBuilder.build()).get();
            final TransactionId trnsId = resp.getResult().getTransactionId();
            registerConfigDataToTransaction(trnsId, lightWeightedFlow);
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Read Flow {} Stat for Node {} from Device Fail!", lightWeightedFlow, nodeRef, e);
        }
    }

    @Override
    public void getQueueStatForGivenPort(final NodeRef nodeRef,
            final NodeConnectorId nodeConnectorId, final QueueId queueId) {
        final GetQueueStatisticsFromGivenPortInputBuilder inputBuilder =
                new GetQueueStatisticsFromGivenPortInputBuilder();
        inputBuilder.setNode(nodeRef);
        inputBuilder.setNodeConnectorId(nodeConnectorId);
        inputBuilder.setQueueId(queueId);
        registrationRpcFutureCallBack(queueStatsService
                .getQueueStatisticsFromGivenPort(inputBuilder.build()));
    }

    @Override
    public void getAllGroupsConfStats(final NodeRef nodeRef) {
        Preconditions.checkArgument(nodeRef != null, "NodeRef can not be null!");
        final GetGroupDescriptionInputBuilder builder =
                new GetGroupDescriptionInputBuilder();
        builder.setNode(nodeRef);
        registrationRpcFutureCallBack(groupStatsService
                .getGroupDescription(builder.build()));
    }
}

