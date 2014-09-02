/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.impl.helper.FlowComparator;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowHashIdMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatListenCommitFlow
 * Class is a NotifyListener for FlowStatistics and DataChangeListener for Config/DataStore for Flow node.
 * All expected (registered) FlowStatistics will be builded and commit to Operational/DataStore.
 * DataChangeEven should call create/delete Flow in Operational/DS create process needs to pair
 * Device Flow HashCode and FlowId from Config/DS
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatListenCommitFlow extends StatAbstractListenCommit<Flow, OpendaylightFlowStatisticsListener>
                                            implements OpendaylightFlowStatisticsListener {

    private static final Logger LOG = LoggerFactory.getLogger(StatListenCommitFlow.class);

    private static final String ALIEN_SYSTEM_FLOW_ID = "#UF$TABLE*";

    private final AtomicInteger unaccountedFlowsCounter = new AtomicInteger(0);

    public StatListenCommitFlow (final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService nps){
        super(manager, db, nps, Flow.class);
    }

    @Override
    protected OpendaylightFlowStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<Flow> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Flow> keyIdent, final Flow data,
            final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        /* make light-weight DataObject for identification */
        final FlowBuilder flowBl = new FlowBuilder();
        flowBl.setId(data.getId());
        flowBl.setKey(data.getKey());
        flowBl.setCookie(data.getCookie());
        flowBl.setPriority(data.getPriority());
        flowBl.setMatch(data.getMatch());
        /* RPC call */
        manager.getDeviceMsgManager().getFlowStat(new NodeRef(nodeIdent), flowBl.build());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Flow> keyIdent) {
        if ( ! manager.getRepeatedlyEnforcer().isProvidedIdentLocked(keyIdent)) {
            final ReadWriteTransaction trans = manager.getReadWriteTransaction();
            try {
                final Optional<Flow> flowForDelete = trans.read(LogicalDatastoreType.OPERATIONAL, keyIdent).get();
                if (flowForDelete.isPresent()) {
                    final FlowBuilder builder = new FlowBuilder();
                    builder.setMatch(flowForDelete.get().getMatch());
                    builder.setCookie(flowForDelete.get().getCookie());
                    builder.setPriority(flowForDelete.get().getPriority());
                    final Flow flowForHashCode = builder.build();
                    final FlowHashIdMapKey key = new FlowHashIdMapKey(String.valueOf(flowForHashCode.hashCode()));
                    final InstanceIdentifier<Table> tableIdent = keyIdent.firstIdentifierOf(Table.class);
                    final InstanceIdentifier<FlowHashIdMap> flowHashIdRemoveIdent = tableIdent
                            .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
                    trans.delete(LogicalDatastoreType.OPERATIONAL, flowHashIdRemoveIdent);
                }
            }
            catch (InterruptedException | ExecutionException e) {
                LOG.error("Read flow {} fail!", keyIdent, e);
            }
            trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
            trans.submit();
        }
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(final AggregateFlowStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - AggregateFlowStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
        }
        final NodeId nodeId = notification.getId();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if ( ! txContainer.isPresent()) {
            return;
        }
        final Optional<DataObject> inputObj = txContainer.get().getConfInput();
        if ( ! inputObj.isPresent() || inputObj.get() instanceof Table) {
            return;
        }
        final Table table = (Table) inputObj;
        final WriteTransaction trans = manager.getWriteTransaction();
        final InstanceIdentifier<Table> tableRef = InstanceIdentifier.create(Nodes.class).child(Node.class, new NodeKey(nodeId))
                .augmentation(FlowCapableNode.class).child(Table.class, table.getKey());
        final AggregateFlowStatisticsDataBuilder aggregateFlowStatisticsDataBuilder = new AggregateFlowStatisticsDataBuilder();
        final AggregateFlowStatisticsBuilder aggregateFlowStatisticsBuilder = new AggregateFlowStatisticsBuilder(notification);
        aggregateFlowStatisticsDataBuilder.setAggregateFlowStatistics(aggregateFlowStatisticsBuilder.build());
        LOG.debug("Augment aggregate statistics: {} for table {} on Node {}",
                aggregateFlowStatisticsBuilder.build().toString(), table.getId(), nodeId);

        final TableBuilder tableBuilder = new TableBuilder();
        tableBuilder.setKey(new TableKey(table.getId()));
        tableBuilder.addAugmentation(AggregateFlowStatisticsData.class, aggregateFlowStatisticsDataBuilder.build());
        trans.merge(LogicalDatastoreType.OPERATIONAL, tableRef, tableBuilder.build(), true);
        trans.submit();
    }

    @Override
    public void onFlowsStatisticsUpdate(final FlowsStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - FlowsStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<FlowAndStatisticsMapList> flowStats =
                new ArrayList<>(notification.getFlowAndStatisticsMapList());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        Optional<Flow> flow = Optional.absent();
        if (txContainer.isPresent()) {
            final Optional<DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Flow) {
                flow = Optional.<Flow> of((Flow)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof FlowsStatisticsUpdate) {
                    flowStats.addAll(((FlowsStatisticsUpdate) notif).getFlowAndStatisticsMapList());
                }
            }
        }
        statsFlowCommit(flowStats, nodeId, flow);
    }

    private void repeaterForFlow(final NodeId id, final Optional<Flow> flow) {
        if (! flow.isPresent()) {
            return;
        }
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(id)).augmentation(FlowCapableNode.class);
        final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = nodeIdent
                .child(Table.class, new TableKey(flow.get().getTableId())).child(Flow.class, flow.get().getKey());
        if (repeaterMap.containsKey(flowIdent)) {
            final Integer nrOfLoops = repeaterMap.get(flowIdent);
            if (nrOfLoops.intValue() < 1) {
                /* FLOW probably not add to device correctly */
                repeaterMap.remove(flowIdent);
                return;
            }
            repeaterMap.put(flowIdent, Integer.valueOf(nrOfLoops.intValue()-1));
        }
        repeaterMap.put(flowIdent, Integer.valueOf(3));
        createStat(flowIdent, flow.get(), nodeIdent);
    }

    private void statsFlowCommit(final List<FlowAndStatisticsMapList> list, final NodeId id, final Optional<Flow> flow) {
        final ReadWriteTransaction trans = manager.getReadWriteTransaction();
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(id))
                .augmentation(FlowCapableNode.class);

        if (flow.isPresent()) {
            for (final FlowAndStatisticsMapList deviceFlow : list) {
                final KeyedInstanceIdentifier<Table, TableKey> tableRef =
                        nodeIdent.child(Table.class, new TableKey(deviceFlow.getTableId()));
                final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = tableRef.child(Flow.class, flow.get().getKey());
                if (deviceFlow.getPriority().equals(flow.get().getPriority())) {
                    final FlowHashIdMapKey key = new FlowHashIdMapKey(buildHashCode(deviceFlow));
                    final FlowHashIdMap flHashIdMap = applyNewFlowKey(flow.get().getKey(), key);
                    final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
                            .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
                    trans.merge(LogicalDatastoreType.OPERATIONAL, flHashIdent, flHashIdMap, true);
                    final FlowBuilder flowBuld = new FlowBuilder(deviceFlow);
                    flowBuld.setKey(flow.get().getKey());
                    flowBuld.setId(flow.get().getId());
                    trans.merge(LogicalDatastoreType.OPERATIONAL, flowIdent, flowBuld.build(), true);
                    trans.submit();
                    return;
                }
            }
            repeaterForFlow(id, flow);
            return;
        }

        final List<InstanceIdentifier<Flow>> flowPaths = new ArrayList<>();

        for (final FlowAndStatisticsMapList flowStat : list) {
            final FlowBuilder flowBuilder = new FlowBuilder(flowStat);
            final KeyedInstanceIdentifier<Table, TableKey> tableRef =
                        nodeIdent.child(Table.class, new TableKey(flowStat.getTableId()));
            final FlowHashIdMapKey key = new FlowHashIdMapKey(buildHashCode(flowStat));
            final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
                    .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
            Optional<FlowHashIdMap> hashIdMap = Optional.absent();
            try {
                hashIdMap = trans.read(LogicalDatastoreType.OPERATIONAL, flHashIdent).get();
            }
            catch (InterruptedException | ExecutionException e) {
                LOG.error("Read Config table {} fail!", tableRef, e);
            }
            Optional<FlowKey> flowKey = Optional.absent();
            if (hashIdMap.isPresent()) {
                flowKey = Optional.<FlowKey> of(new FlowKey(hashIdMap.get().getFlowId()));
            }
            if ( ! flowKey.isPresent()) {
                final Flow flowRule = flowBuilder.build();
                flowKey = getFlowKeyFromExistFlow(flowRule, tableRef, trans);
                if ( ! flowKey.isPresent()) {
                    /* Alien flow */
                    flowKey = makeAlienFlowKey(flowRule);
                }
                hashIdMap = Optional.of(applyNewFlowKey(flowKey.get(), key));
                trans.merge(LogicalDatastoreType.OPERATIONAL, flHashIdent, hashIdMap.get(), true);
            }
            flowBuilder.setKey(flowKey.get());
            flowBuilder.setId(flowKey.get().getId());
            final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = tableRef.child(Flow.class, flow.get().getKey());
            trans.merge(LogicalDatastoreType.OPERATIONAL, flowIdent, flowBuilder.build(), true);
            flowPaths.add(flowIdent);
        }
        cleaningOperationalDS(flowPaths, trans);
    }

    /*
     * build pseudoUnique hashCode for flow in table
     * for future easy identification */
    private String buildHashCode(final FlowAndStatisticsMapList deviceFlow) {
        final FlowBuilder builder = new FlowBuilder();
        builder.setMatch(deviceFlow.getMatch());
        builder.setCookie(deviceFlow.getCookie());
        builder.setPriority(deviceFlow.getPriority());
        final Flow flowForHashCode = builder.build();
        return String.valueOf(flowForHashCode.hashCode());
    }

    /* Returns FlowKey from existing Flow in DataStore/CONFIGURATIONAL which is identified by cookie
     * and by switch flow identification (priority and match) */
    private Optional<FlowKey> getFlowKeyFromExistFlow(final Flow flowRule,
            final InstanceIdentifier<Table> tableRef, final ReadWriteTransaction trans) {
        /* Try to find it in DataSotre/CONFIG */
        Optional<Table> table = Optional.absent();
        try {
            table = trans.read(LogicalDatastoreType.CONFIGURATION, tableRef).get();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Read Config table {} fail!", tableRef, e);
        }
        if(table.isPresent()) {
            for(final Flow existingFlow : table.get().getFlow()) {
//                LOG.debug("Existing flow in data store : {}",existingFlow.toString());
                if(FlowComparator.flowEquals(flowRule,existingFlow)){
                    return Optional.<FlowKey> of(new FlowKey(existingFlow.getId()));
                }
            }
        }
        return Optional.absent();
    }

    /* Returns FlowKey which doesn't exist in any DataStore for now */
    private Optional<FlowKey> makeAlienFlowKey(final Flow flowRule) {
        final StringBuilder sBuilder = new StringBuilder(ALIEN_SYSTEM_FLOW_ID)
            .append(flowRule.getTableId()).append("-").append(unaccountedFlowsCounter.incrementAndGet());
        final FlowId flowId = new FlowId(sBuilder.toString());
        return Optional.<FlowKey> of(new FlowKey(flowId));
    }

    /* Build new whole FlowCookieMap or add new flowKey */
    private FlowHashIdMap applyNewFlowKey(final FlowKey flowKey, final FlowHashIdMapKey flowHashKey) {
        final FlowHashIdMapBuilder flHashIdBl = new FlowHashIdMapBuilder();
        flHashIdBl.setFlowId(flowKey.getId());
        flHashIdBl.setHash(flowHashKey.getHash());
        flHashIdBl.setKey(flowHashKey);
        return flHashIdBl.build();
    }
}

