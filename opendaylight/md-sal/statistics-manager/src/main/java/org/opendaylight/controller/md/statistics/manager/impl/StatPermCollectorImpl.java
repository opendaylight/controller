package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.opendaylight.controller.md.statistics.manager.StatPermCollector;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatPermCollectorImpl
 * Thread base statistic collector. Class holds internal map for all registered
 * (means connected) nodes with List of Switch capabilities;
 * Statistics collecting process get cross whole Network Device by device
 * and statistic by statistic (follow Switch capabilities to prevent unnecessary
 * ask) Next statistic start collecting by notification or by timeout.
 *
 * @author @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class StatPermCollectorImpl implements StatPermCollector {

    private final static Logger LOG = LoggerFactory.getLogger(StatPermCollectorImpl.class);

    private final static long STAT_COLLECT_TIME_OUT = 30000L;

    private final ExecutorService statNetCollectorServ;

    private final Object statCollectorLock = new Object();
    private final Object statNodeHolderLock = new Object();
    private final StatisticsManager manager;
    private final long minReqNetInterv;

    private Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNodeHolder =
            Collections.<InstanceIdentifier<Node>, StatNodeInfoHolder> emptyMap();

    private volatile boolean finishing = false;
    private volatile boolean wakeMe = false;

    public StatPermCollectorImpl(final StatisticsManager manager, final long minReqNetInterv) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticsManager can not be null!");
        this.minReqNetInterv = minReqNetInterv;
        final ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("odl-stat-collector-thread-%d").build();
        statNetCollectorServ = Executors.newSingleThreadExecutor(threadFact);
    }

    /**
     * finish collecting statistics
     */
    @Override
    public void close() {
        statNodeHolder = Collections.<InstanceIdentifier<Node>, StatNodeInfoHolder> emptyMap();
        finishing = true;
        collectNextStatistics();
        statNetCollectorServ.shutdown();
    }

    public String getName() {
        return "StatPermanentCollector";
    }

    @Override
    public boolean isProvidedFlowNodeActive(
            final InstanceIdentifier<Node> flowNode) {
        return statNodeHolder.containsKey(flowNode);
    }

    @Override
    public void connectedNodeRegistration(final InstanceIdentifier<Node> ident,
            final List<StatCapabTypes> statTypes, final Short nrOfSwitchTables) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} registration can not be wildcarded!", ident);
        } else {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (statNodeHolderLock) {
                    final boolean startStatCollecting = statNodeHolder.size() == 0;
                    if ( ! statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNode =
                                new HashMap<>(statNodeHolder);
                        final NodeRef nodeRef = new NodeRef(ident);
                        final StatNodeInfoHolder nodeInfoHolder = new StatNodeInfoHolder(nodeRef,
                                statTypes, nrOfSwitchTables);
                        statNode.put(ident, nodeInfoHolder);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                    if (startStatCollecting) {
                        statNetCollectorServ.execute(this);
                    }
                }
            }
        }
    }

    @Override
    public void disconnectedNodeUnregistration(final InstanceIdentifier<Node> ident) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} unregistration can not be wildcarded!", ident);
        } else {
            if (statNodeHolder.containsKey(ident)) {
                synchronized (statNodeHolderLock) {
                    if (statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.remove(ident);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                    if (statNodeHolder.size() == 0) {
                        statNetCollectorServ.shutdown();
                    }
                }
            }
        }
    }

    @Override
    public void collectNextStatistics() {
        if (wakeMe) {
            synchronized (statCollectorLock) {
                if (wakeMe) {
                    LOG.trace("STAT-COLLECTOR is notified to conntinue");
                    statCollectorLock.notify();
                }
            }
        }
    }

    @Override
    public void run() {
         /* Neverending cyle - wait for finishing */
        while ( ! finishing) {
            boolean collecting = false;
            final long startTime = System.currentTimeMillis();

            if ( ! statNodeHolder.isEmpty()) {
                collecting = true;
                collectStatCrossNetwork();
                collecting = false;
            }

            if ( ! collecting) {
                final long statFinalTime = System.currentTimeMillis() - startTime;
                LOG.info("STAT-MANAGER : last all NET statistics collection cost {} ms", statFinalTime);
                if (statFinalTime < minReqNetInterv) {
                    LOG.trace("statCollector is about to make a collecting sleep");
                    synchronized (statCollectorLock) {
                        wakeMe = true;
                        try {
                            final long waitTime = minReqNetInterv - statFinalTime;
                            statCollectorLock.wait(waitTime);
                            LOG.trace("STAT-MANAGER : statCollector is waking up from a collecting sleep for {} ms", waitTime);
                        } catch (final InterruptedException e) {
                            LOG.warn("statCollector has been interrupted during collecting sleep", e);
                        } finally {
                            wakeMe = false;
                        }
                    }
                }
            }
        }
    }

    private void waitingForNotification() {
        synchronized (statCollectorLock) {
            wakeMe = true;
            try {
                statCollectorLock.wait(STAT_COLLECT_TIME_OUT);
                LOG.trace("statCollector is waking up from a wait stat Response sleep");
            } catch (final InterruptedException e) {
                LOG.warn("statCollector has been interrupted waiting stat Response sleep", e);
            } finally {
                wakeMe = false;
            }
        }
    }


    private void collectStatCrossNetwork() {
        for (final Entry<InstanceIdentifier<Node>, StatNodeInfoHolder> nodeEntity : statNodeHolder.entrySet()) {
            final List<StatCapabTypes> listNeededStat = nodeEntity.getValue().getStatMarkers();
            final NodeRef actualNodeRef = nodeEntity.getValue().getNodeRef();
            final Short maxTables = nodeEntity.getValue().getMaxTables();
            for (final StatCapabTypes statMarker : listNeededStat) {
                if ( ! isProvidedFlowNodeActive(nodeEntity.getKey())) {
                    break;
                }
                switch (statMarker) {
                case PORT_STATS:
                    LOG.trace("STAT-MANAGER-collecting PORT-STATS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getAllPortsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case QUEUE_STATS:
                    LOG.trace("STAT-MANAGER-collecting QUEUE-STATS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getAllQueueStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case TABLE_STATS:
                    LOG.trace("STAT-MANAGER-collecting TABLE-STATS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getAllTablesStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case GROUP_STATS:
                    LOG.trace("STAT-MANAGER-collecting GROUP-STATS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getGroupFeaturesStat(actualNodeRef);
                    waitingForNotification();
                    manager.getRpcMsgManager().getAllGroupsConfStats(actualNodeRef);
                    waitingForNotification();
                    manager.getRpcMsgManager().getAllGroupsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case METER_STATS:
                    LOG.trace("STAT-MANAGER-collecting METER-STATS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getMeterFeaturesStat(actualNodeRef);
                    waitingForNotification();
                    manager.getRpcMsgManager().getAllMeterConfigStat(actualNodeRef);
                    waitingForNotification();
                    manager.getRpcMsgManager().getAllMetersStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case FLOW_STATS:
                    LOG.trace("STAT-MANAGER-collecting FLOW-STATS-ALL_FLOWS for NodeRef {}", actualNodeRef);
                    manager.getRpcMsgManager().getAllFlowsStat(actualNodeRef);
                    waitingForNotification();
                    LOG.trace("STAT-MANAGER-collecting FLOW-AGGREGATE-STATS for NodeRef {}", actualNodeRef);
                    for (short i = 0; i < maxTables; i++) {
                        final TableId tableId = new TableId(i);
                        manager.getRpcMsgManager().getAggregateFlowStat(actualNodeRef, tableId);
                    }
                    break;
                default:
                    /* Exception for programmers in implementation cycle */
                    throw new IllegalStateException("Not implemented ASK for " + statMarker);
                }
            }
        }
    }

    private class StatNodeInfoHolder {
        private final NodeRef nodeRef;
        private final List<StatCapabTypes> statMarkers;
        private final Short maxTables;

        public StatNodeInfoHolder(final NodeRef nodeRef,
                final List<StatCapabTypes> statMarkers, final Short maxTables) {
            this.nodeRef = nodeRef;
            this.maxTables = maxTables;
            this.statMarkers = statMarkers;
        }

        public final NodeRef getNodeRef() {
            return nodeRef;
        }

        public final List<StatCapabTypes> getStatMarkers() {
            return statMarkers;
        }

        public final Short getMaxTables() {
            return maxTables;
        }
    }
}

