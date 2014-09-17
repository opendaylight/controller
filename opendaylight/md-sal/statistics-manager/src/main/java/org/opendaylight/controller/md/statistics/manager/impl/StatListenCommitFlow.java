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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.md.statistics.manager.impl.helper.FlowComparator;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowHashIdMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowHashIdMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
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

import com.google.common.base.Optional;

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
public class StatListenCommitFlow extends StatAbstractListenCommit<Flow, OpendaylightFlowStatisticsListener>
                                            implements OpendaylightFlowStatisticsListener {

    private static final Logger LOG = LoggerFactory.getLogger(StatListenCommitFlow.class);

    private final Map<InstanceIdentifier<Flow>, Integer> repeaterMap = new ConcurrentHashMap<>();

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
            final InstanceIdentifier<Node> nodeIdent) {
        /* RPC call */
        manager.getRpcMsgManager().getFlowStat(new NodeRef(nodeIdent), data);
    }

    @Override
    public void removeStat(final InstanceIdentifier<Flow> keyIdent) {
        manager.equals(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<Flow> flow = Optional.absent();
                try {
                    flow = tx.read(LogicalDatastoreType.OPERATIONAL, keyIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.warn("Operational/DS for Flow fail! {}", keyIdent, e);
                    return;
                }
                if ( ! flow.isPresent()) {
                    return;
                }
                final FlowBuilder builder = new FlowBuilder();
                builder.setMatch(flow.get().getMatch());
                builder.setCookie(flow.get().getCookie());
                builder.setPriority(flow.get().getPriority());
                final String hashCode = String.valueOf(builder.build().hashCode());

                final InstanceIdentifier<FlowHashIdMap> hashIdIdent = keyIdent.firstIdentifierOf(Table.class)
                        .augmentation(FlowHashIdMapping.class)
                        .child(FlowHashIdMap.class, new FlowHashIdMapKey(hashCode));
                tx.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
                tx.delete(LogicalDatastoreType.OPERATIONAL, hashIdIdent);
            }
        });
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(final AggregateFlowStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - AggregateFlowStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        if ( ! txContainer.isPresent()) {
            return;
        }
        final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
        if (( ! inputObj.isPresent()) || ( ! (inputObj.get() instanceof Table))) {
            return;
        }
        final Table table = (Table) inputObj.get();
        final AggregateFlowStatistics stats = new AggregateFlowStatisticsBuilder(notification).build();
        /* check flow Capable Node and write statistics */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                        .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);
                final InstanceIdentifier<AggregateFlowStatistics> tableStatRef = fNodeIdent
                        .child(Table.class, table.getKey()).augmentation(AggregateFlowStatisticsData.class)
                        .child(AggregateFlowStatistics.class);
                Optional<FlowCapableNode> fNode = Optional.absent();
                try {
                    fNode = tx.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                    return;
                }
                if (fNode.isPresent()) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, tableStatRef, stats, true);
                }
            }
        });
    }

    @Override
    public void onFlowsStatisticsUpdate(final FlowsStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - FlowsStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            LOG.trace("Next notification for join txId {}", transId);
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<FlowAndStatisticsMapList> flowStats = notification.getFlowAndStatisticsMapList() != null
                ? new ArrayList<FlowAndStatisticsMapList>(notification.getFlowAndStatisticsMapList())
                        : new ArrayList<FlowAndStatisticsMapList>();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        Optional<Flow> flowNotif = Optional.absent();
        if (txContainer.isPresent()) {
            final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Flow) {
                flowNotif = Optional.<Flow> of((Flow)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof FlowsStatisticsUpdate) {
                    flowStats.addAll(((FlowsStatisticsUpdate) notif).getFlowAndStatisticsMapList());
                }
            }
        }
        final Optional<Flow> flow = flowNotif;
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        /* add flow's statistics */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                if (flow.isPresent()) {
                    statsFlowCommitForOne(flowStats, tx, nodeIdent, flow);
                } else {
                    statsFlowCommitAll(flowStats, nodeIdent, tx);
                }
            }
        });
//        if (flow.isPresent()) {
//            statsFlowCommitForOne(flowStats, nodeIdent, flow);
//        } else {
//            statsFlowCommitAll(flowStats, nodeIdent);
//        }
    }

    private void repeaterForFlow(final InstanceIdentifier<Node> nodeIdent, final Optional<Flow> flow) {
        if ( ! flow.isPresent()) {
            LOG.warn("Light-weight flow can not be null for Node {} stat update", nodeIdent);
            return;
        }
        final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = nodeIdent.augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.get().getTableId())).child(Flow.class, flow.get().getKey());
        if (repeaterMap.containsKey(flowIdent)) {
            final Optional<Integer> nrOfLoops = Optional.fromNullable(repeaterMap.get(flowIdent));
            if ( ! nrOfLoops.isPresent()) {
                repeaterMap.put(flowIdent, Integer.valueOf(3));
                createStat(flowIdent, flow.get(), nodeIdent);
            }
            if (nrOfLoops.get().intValue() < 1) {
                /* FLOW probably not add to device correctly */
                repeaterMap.remove(flowIdent);
                return;
            }
            final int i = nrOfLoops.get().intValue() - 1;
            repeaterMap.put(flowIdent, Integer.valueOf(i));
        }
        repeaterMap.put(flowIdent, Integer.valueOf(3));
        createStat(flowIdent, flow.get(), nodeIdent);
    }

    private void statsFlowCommitForOne(final List<FlowAndStatisticsMapList> list, final ReadWriteTransaction trans,
            final InstanceIdentifier<Node> nodeIdent, final Optional<Flow> flow) {
        if ( ! manager.getStatCollector().isProvidedFlowNodeActive(nodeIdent)) {
            LOG.debug("FlowCapableNode {} has been disconnected - FlowStatistics are ignored!", nodeIdent);
            return;
        }

        for (final FlowAndStatisticsMapList deviceFlow : list) {
            /* check priority because Rpc use for identification match and flowCookie only */
            if (deviceFlow.getPriority().equals(flow.get().getPriority())) {
                final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
                final KeyedInstanceIdentifier<Table, TableKey> tableRef = fNodeIdent
                        .child(Table.class, new TableKey(deviceFlow.getTableId()));
                final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = tableRef.child(Flow.class, flow.get().getKey());
                final FlowHashIdMapKey key = new FlowHashIdMapKey(buildHashCode(deviceFlow));
                final FlowHashIdMap flHashIdMap = applyNewFlowKey(flow.get().getKey(), key);
                final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
                        .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);

//                manager.enqueue(new StatDataStoreOperation() {
//                    @Override
//                    public void applyOperation(final ReadWriteTransaction trans) {
                        Optional<FlowHashIdMap> readHashId = Optional.absent();
                        try {
                            readHashId = trans.read(LogicalDatastoreType.OPERATIONAL, flHashIdent).checkedGet();
                        }
                        catch (final ReadFailedException e) {
                            LOG.debug("Operational/DS for FlowHashMap fail! {}", flHashIdent, e);
                        }
                        if (readHashId.isPresent()) {
                            /* remove if exist some */
                            final KeyedInstanceIdentifier<Flow, FlowKey> flowIdentRemove =
                                    tableRef.child(Flow.class, new FlowKey(readHashId.get().getFlowId()));
                            Optional<Flow> checkFlow = Optional.absent();
                            try {
                                checkFlow = trans.read(LogicalDatastoreType.OPERATIONAL, flowIdentRemove).checkedGet();
                            }
                            catch (final ReadFailedException e) {
                                LOG.debug("Operational/DS for Flow fail! {}", flowIdentRemove, e);
                            }
                            if (checkFlow.isPresent()) {
                                trans.delete(LogicalDatastoreType.OPERATIONAL, flowIdentRemove);
                            }
                            trans.delete(LogicalDatastoreType.OPERATIONAL, flHashIdent);
                        }
                        /* add flow and hash*/
                        final FlowBuilder flowBuld = new FlowBuilder(deviceFlow);
                        flowBuld.setKey(flow.get().getKey());
                        flowBuld.setId(flow.get().getId());
//                        addStatistics(flowBuld, deviceFlow);
                        Optional<FlowCapableNode> fNode = Optional.absent();
                        try {
                            fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                        }
                        catch (final ReadFailedException e) {
                            LOG.debug("Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                        }
                        if (fNode.isPresent()) {
                            trans.put(LogicalDatastoreType.OPERATIONAL, flowIdent, flowBuld.build(), true);
                            trans.put(LogicalDatastoreType.OPERATIONAL, flHashIdent, flHashIdMap, true);
                        }
//                    }
//                });
                /* prevent repeat request */
                repeaterMap.remove(flowIdent);
                return;
            }
        }
        /* not found */
        repeaterForFlow(nodeIdent, flow);
    }

//    private void statsFlowCommitAll(final List<FlowAndStatisticsMapList> list,
//            final InstanceIdentifier<Node> nodeIdent) {
//        /* add flow stats */
//        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
//        final Map<TableKey, Set<FlowHashIdMapKey>> deviceFlowHashIdMap = new HashMap<>();
//
//        for (final FlowAndStatisticsMapList flowStat : list) {
//            final FlowBuilder flowBuilder = new FlowBuilder(flowStat);
//            final TableKey tableKey = new TableKey(flowStat.getTableId());
//            final KeyedInstanceIdentifier<Table, TableKey> tableRef = nodeIdent.augmentation(FlowCapableNode.class)
//                    .child(Table.class, new TableKey(flowStat.getTableId()));
//            final FlowHashIdMapKey key = new FlowHashIdMapKey(buildHashCode(flowStat));
//            if (deviceFlowHashIdMap.containsKey(tableKey)) {
//                deviceFlowHashIdMap.get(tableKey).add(key);
//            } else {
//                final HashSet<FlowHashIdMapKey> hashIdMapKeySet = new HashSet<>();
//                hashIdMapKeySet.add(key);
//                deviceFlowHashIdMap.put(tableKey, hashIdMapKeySet);
//            }
//            final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
//                    .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
//
//            manager.enqueue(new StatDataStoreOperation() {
//                @Override
//                public void applyOperation(final ReadWriteTransaction trans) {
//
//                    Optional<FlowCapableNode> checkFlowNode = Optional.absent();
//                    try {
//                        checkFlowNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
//                    }
//                    catch (final ReadFailedException ex) {
//                        LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, ex);
//                        return;
//                    }
//                    if ( ! checkFlowNode.isPresent()) {
//                        LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
//                        return;
//                    }
//                    Optional<FlowHashIdMap> readHashId = Optional.absent();
//                    try {
//                        readHashId = trans.read(LogicalDatastoreType.OPERATIONAL, flHashIdent).checkedGet();
//                    }
//                    catch (final ReadFailedException e) {
//                        LOG.debug("Operational/DS for FlowHashMap fail! {}", flHashIdent, e);
//                        return;
//                    }
//                    Optional<FlowKey> flowKey = Optional.absent();
//                    if (readHashId.isPresent()) {
//                        flowKey = Optional.<FlowKey> of(new FlowKey(readHashId.get().getFlowId()));
//                    } else {
//                        /* try to find FlowId in actual Config/DS by etalon (flowRule) */
//                        final Flow flowRule = flowBuilder.build();
//                        flowKey = getFlowKeyFromExistFlow(flowRule, tableRef, trans);
//                        if ( ! flowKey.isPresent()) {
//                            /* Alien flow */
//                            flowKey = makeAlienFlowKey(flowRule);
//                        }
//                        /* Build and deploy new FlowHashId map */
//                        final FlowHashIdMapBuilder flHashIdMap = new FlowHashIdMapBuilder();
//                        flHashIdMap.setFlowId(flowKey.get().getId());
//                        flHashIdMap.setHash(key.getHash());
//                        flHashIdMap.setKey(key);
//                        final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
//                                .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
//                        /* Add new FlowHashIdMap */
//                        trans.put(LogicalDatastoreType.OPERATIONAL, flHashIdent, flHashIdMap.build(), true);
//                    }
//                    flowBuilder.setKey(flowKey.get());
//                    flowBuilder.setId(flowKey.get().getId());
//                    addStatistics(flowBuilder, flowStat);
//                    /* add flow */
//                    final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent = tableRef.child(Flow.class, flowKey.get());
//                    trans.put(LogicalDatastoreType.OPERATIONAL, flowIdent, flowBuilder.build());
//                }
//            });
//        }
//
//        /* Check Flow for remove */
//        manager.enqueue(new StatDataStoreOperation() {
//            @Override
//            public void applyOperation(final ReadWriteTransaction trans) {
//                /* Notification for continue collecting statistics */
//                manager.getStatCollector().collectNextStatistics();
//
//                Optional<FlowCapableNode> fNode = Optional.absent();
//                try {
//                    fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
//                }
//                catch (final ReadFailedException e1) {
//                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent);
//                    return;
//                }
//                if ( ! fNode.isPresent()) {
//                    LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
//                    return;
//                }
//                final List<Table> existTables = fNode.get().getTable() == null
//                        ? Collections.<Table> emptyList() : fNode.get().getTable();
//                /* Add all existed flows paths - not updated paths has to be removed */
//                for (final Table table : existTables) {
//                    final InstanceIdentifier<Table> tableIdent = fNodeIdent.child(Table.class, table.getKey());
//                    final TableKey tableKey = table.getKey();
//                    final FlowHashIdMapping flowHashMapping = table.getAugmentation(FlowHashIdMapping.class);
//                    if (flowHashMapping != null) {
//                        final Set<FlowHashIdMapKey> deviceHashMap = deviceFlowHashIdMap.get(tableKey) != null
//                                ? deviceFlowHashIdMap.get(tableKey) : Collections.<FlowHashIdMapKey> emptySet();
//                        final List<FlowHashIdMap>  flowHashMap = flowHashMapping.getFlowHashIdMap() != null
//                                ? flowHashMapping.getFlowHashIdMap() : Collections.<FlowHashIdMap> emptyList();
//                        for (final FlowHashIdMap flowHashId : flowHashMap) {
//                            if ( ! deviceHashMap.contains(flowHashId.getKey())
//                                    && flowHashId.getFlowId().getValue().startsWith(ALIEN_SYSTEM_FLOW_ID)) {
//                                // delete alien flow if exist
//                                final InstanceIdentifier<Flow> delFlowIdent = tableIdent
//                                        .child(Flow.class, new FlowKey(flowHashId.getFlowId()));
//                                final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> delHashIdIdent = tableIdent
//                                        .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, flowHashId.getKey());
//                                Optional<Table> parentTable = Optional.absent();
//                                try {
//                                    parentTable = trans.read(LogicalDatastoreType.OPERATIONAL, tableIdent).checkedGet();
//                                }
//                                catch (final ReadFailedException e) {
//                                    // NOOP - probably another transaction delete that node
//                                    break;
//                                }
//                                if (parentTable.isPresent()) {
//                                    trans.delete(LogicalDatastoreType.OPERATIONAL, delHashIdIdent);
//                                    trans.delete(LogicalDatastoreType.OPERATIONAL, delFlowIdent);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        });
//    }


    private void statsFlowCommitAll(final List<FlowAndStatisticsMapList> list,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction trans) {

        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);

        Optional<FlowCapableNode> fNode = Optional.absent();
        try {
            fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
        }
        catch (final ReadFailedException e) {
            LOG.debug("Read FlowCapableNode {} in Operational/DS fail! Statisticscan not be updated.", nodeIdent, e);
            return;
        }
        if ( ! fNode.isPresent()) {
            LOG.trace("FlowCapableNode {} is not presented in Operational/DS. Statisticscan not be updated.", nodeIdent);
            return;
        }

        final Map<TableKey, Map<FlowHashIdMapKey, FlowId>> tableFlowHashIdMap = new HashMap<>();
        final List<Table> existTables = fNode.get().getTable() != null
                ? fNode.get().getTable() : Collections.<Table> emptyList();
        /* Add all existed flows paths - not updated paths has to be removed */
        for (final Table table : existTables) {
            final TableKey tableKey = table.getKey();
            final FlowHashIdMapping flowHashMapping = table.getAugmentation(FlowHashIdMapping.class);
            if (flowHashMapping != null) {
                final List<FlowHashIdMap>  flowHashMap = flowHashMapping.getFlowHashIdMap() != null
                        ? flowHashMapping.getFlowHashIdMap() : Collections.<FlowHashIdMap> emptyList();
                final Map<FlowHashIdMapKey, FlowId> neededFlowHash = new HashMap<>();
                for (final FlowHashIdMap flowHashId : flowHashMap) {
                    neededFlowHash.put(flowHashId.getKey(), flowHashId.getFlowId());
                }
            tableFlowHashIdMap.put(tableKey, neededFlowHash);
            }
        }

        for (final FlowAndStatisticsMapList flowStat : list) {
            final FlowBuilder flowBuilder = new FlowBuilder(flowStat);
            final TableKey tableKey = new TableKey(flowStat.getTableId());
            final KeyedInstanceIdentifier<Table, TableKey> tableRef = nodeIdent.augmentation(FlowCapableNode.class)
                    .child(Table.class, new TableKey(flowStat.getTableId()));
            final FlowHashIdMapKey key = new FlowHashIdMapKey(buildHashCode(flowStat));
            Optional<FlowKey> flowKey = Optional.absent();

            final Optional<FlowId> flowId = tableFlowHashIdMap.get(tableKey) == null ? Optional.<FlowId> absent()
                    : Optional.<FlowId> fromNullable(tableFlowHashIdMap.get(tableKey).remove(key));
            if (flowId.isPresent()) {
                flowKey = Optional.<FlowKey> of(new FlowKey(flowId.get()));
            }
            if ( ! flowKey.isPresent()) {
                /* try to find FlowId in actual Config/DS by etalon (flowRule) */
                final Flow flowRule = flowBuilder.build();
                flowKey = getFlowKeyFromExistFlow(flowRule, tableRef, trans);
                if ( ! flowKey.isPresent()) {
                    /* Alien flow */
                    flowKey = makeAlienFlowKey(flowRule);
                }
                /* Build and deploy new FlowHashId map */
                final FlowHashIdMapBuilder flHashIdMap = new FlowHashIdMapBuilder();
                flHashIdMap.setFlowId(flowKey.get().getId());
                flHashIdMap.setHash(key.getHash());
                flHashIdMap.setKey(key);
                final KeyedInstanceIdentifier<FlowHashIdMap, FlowHashIdMapKey> flHashIdent = tableRef
                        .augmentation(FlowHashIdMapping.class).child(FlowHashIdMap.class, key);
                /* Add new FlowHashIdMap */
                trans.put(LogicalDatastoreType.OPERATIONAL, flHashIdent, flHashIdMap.build(), true);
            }
            flowBuilder.setKey(flowKey.get());
            flowBuilder.setId(flowKey.get().getId());
            addStatistics(flowBuilder, flowStat);
            /* add flow */
            final InstanceIdentifier<Flow> flowIdent = tableRef.child(Flow.class, flowKey.get());
            trans.put(LogicalDatastoreType.OPERATIONAL, flowIdent, flowBuilder.build());
        }

        // Removing unused flow's paths
        for (final TableKey tableKey : tableFlowHashIdMap.keySet()) {
            final Map<FlowHashIdMapKey, FlowId> listForRemove = tableFlowHashIdMap.get(tableKey);
            final KeyedInstanceIdentifier<Table, TableKey> tabIdent = nodeIdent
                    .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
            for (final Entry<FlowHashIdMapKey, FlowId> entryForRemove : listForRemove.entrySet()) {
                final InstanceIdentifier<FlowStatisticsData> flowStatIdent = tabIdent
                        .child(Flow.class, new FlowKey(entryForRemove.getValue()))
                        .augmentation(FlowStatisticsData.class);
                Optional<FlowStatisticsData> flowStatNodeCheck = Optional.absent();
                try {
                    flowStatNodeCheck = trans
                            .read(LogicalDatastoreType.OPERATIONAL, flowStatIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read FlowStatistics {} in Operational/DS fail! Statisticscan not be updated.", flowStatIdent, e);
                    break;
                }
                if (flowStatNodeCheck.isPresent()) {
                    /* Node isn't new and it has not been removed yet */
                    final InstanceIdentifier<Flow> delFlowIdent = tabIdent
                            .child(Flow.class, new FlowKey(entryForRemove.getValue()));
                    final InstanceIdentifier<FlowHashIdMap> flHashIdent = tabIdent
                            .augmentation(FlowHashIdMapping.class)
                            .child(FlowHashIdMap.class, entryForRemove.getKey());
                    trans.delete(LogicalDatastoreType.OPERATIONAL, delFlowIdent);
                    trans.delete(LogicalDatastoreType.OPERATIONAL, flHashIdent);
                }
            }
        }
        manager.getStatCollector().collectNextStatistics();
    }

    /**
     * Method adds statistics to Flow
     *
     * @param flowBuilder
     * @param deviceFlow
     */
    private void addStatistics(final FlowBuilder flowBuilder, final FlowAndStatisticsMapList deviceFlow) {
        final FlowAndStatisticsMapListBuilder stats = new FlowAndStatisticsMapListBuilder(deviceFlow);
        final FlowStatisticsBuilder flowStatisticsBuilder = new FlowStatisticsBuilder(stats.build());
        final FlowStatisticsDataBuilder flowStatisticsData =new FlowStatisticsDataBuilder();
        flowStatisticsData.setFlowStatistics(flowStatisticsBuilder.build());
        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
    }

    /**
     * build pseudoUnique hashCode for flow in table
     * for future easy identification
     */
    private String buildHashCode(final FlowAndStatisticsMapList deviceFlow) {
        final FlowBuilder builder = new FlowBuilder();
        builder.setMatch(deviceFlow.getMatch());
        builder.setCookie(deviceFlow.getCookie());
        builder.setPriority(deviceFlow.getPriority());
        final Flow flowForHashCode = builder.build();
        return String.valueOf(flowForHashCode.hashCode());
    }

    /**
     * Returns FlowKey from existing Flow in DataStore/CONFIGURATIONAL which is identified by cookie
     * and by switch flow identification (priority and match)
     */
    private Optional<FlowKey> getFlowKeyFromExistFlow(final Flow flowRule,
            final InstanceIdentifier<Table> tableRef, final ReadWriteTransaction trans) {
        /* Try to find it in DataSotre/CONFIG */
        Optional<Table> table = Optional.absent();
        try {
            table = trans.read(LogicalDatastoreType.CONFIGURATION, tableRef).checkedGet();
        }
        catch (final ReadFailedException e) {
            LOG.warn("Read Config table {} fail!", tableRef, e);
        }
        if(table.isPresent()) {
            final List<Flow> tableFlows = table.get().getFlow() != null
                    ? table.get().getFlow() : Collections.<Flow> emptyList();
            for(final Flow existingFlow : tableFlows) {
                LOG.debug("Existing flow in data store : {}",existingFlow.toString());
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

