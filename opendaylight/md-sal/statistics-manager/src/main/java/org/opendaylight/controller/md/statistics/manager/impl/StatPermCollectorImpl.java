package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
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
    private final StatisticsManager manager;

    private final int maxNodeForCollector;
    private final long minReqNetInterval;
    private final String name;

    private final Object statCollectorLock = new Object();
    private final Object statNodeHolderLock = new Object();

    private Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNodeHolder =
            Collections.<InstanceIdentifier<Node>, StatNodeInfoHolder> emptyMap();

    private volatile boolean wakeMe = false;
    private volatile boolean finishing = false;

    public StatPermCollectorImpl(final StatisticsManager manager, final long minReqNetInterv, final int nr,
            final int maxNodeForCollectors) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticsManager can not be null!");
        name = "odl-stat-collector-" + nr;
        minReqNetInterval = minReqNetInterv;
        final ThreadFactory threadFact = new ThreadFactoryBuilder()
            .setNameFormat(name + "-thread-%d").build();
        statNetCollectorServ = Executors.newSingleThreadExecutor(threadFact);
        maxNodeForCollector = maxNodeForCollectors;
        LOG.trace("StatCollector {} start successfull!", name);
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

    @Override
    public boolean hasActiveNodes() {
        return ( ! statNodeHolder.isEmpty());
    }

    @Override
    public boolean isProvidedFlowNodeActive(
            final InstanceIdentifier<Node> flowNode) {
        return statNodeHolder.containsKey(flowNode);
    }

    @Override
    public boolean connectedNodeRegistration(final InstanceIdentifier<Node> ident,
            final List<StatCapabTypes> statTypes, final Short nrOfSwitchTables) {
        if (isNodeIdentValidForUse(ident)) {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (statNodeHolderLock) {
                    final boolean startStatCollecting = statNodeHolder.size() == 0;
                    if ( ! statNodeHolder.containsKey(ident)) {
                        if (statNodeHolder.size() >= maxNodeForCollector) {
                            return false;
                        }
                        final Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNode =
                                new HashMap<>(statNodeHolder);
                        final NodeRef nodeRef = new NodeRef(ident);
                        final StatNodeInfoHolder nodeInfoHolder = new StatNodeInfoHolder(nodeRef,
                                statTypes, nrOfSwitchTables);
                        statNode.put(ident, nodeInfoHolder);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                    if (startStatCollecting) {
                        finishing = false;
                        statNetCollectorServ.execute(this);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean disconnectedNodeUnregistration(final InstanceIdentifier<Node> ident) {
        if (isNodeIdentValidForUse(ident)) {
            if (statNodeHolder.containsKey(ident)) {
                synchronized (statNodeHolderLock) {
                    if (statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.remove(ident);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                    if (statNodeHolder.isEmpty()) {
                        finishing = true;
                        collectNextStatistics();
                        statNetCollectorServ.shutdown();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean registerAdditionalNodeFeature(final InstanceIdentifier<Node> ident,
            final StatCapabTypes statCapab) {
        if (isNodeIdentValidForUse(ident)) {
            if ( ! statNodeHolder.containsKey(ident)) {
                return false;
            }
            final StatNodeInfoHolder statNode = statNodeHolder.get(ident);
            if ( ! statNode.getStatMarkers().contains(statCapab)) {
                synchronized (statNodeHolderLock) {
                    if ( ! statNode.getStatMarkers().contains(statCapab)) {
                        final List<StatCapabTypes> statCapabForEdit = new ArrayList<>(statNode.getStatMarkers());
                        statCapabForEdit.add(statCapab);
                        final StatNodeInfoHolder nodeInfoHolder = new StatNodeInfoHolder(statNode.getNodeRef(),
                                Collections.unmodifiableList(statCapabForEdit), statNode.getMaxTables());

                        final Map<InstanceIdentifier<Node>, StatNodeInfoHolder> statNodes =
                                new HashMap<>(statNodeHolder);
                        statNodes.put(ident, nodeInfoHolder);
                        statNodeHolder = Collections.unmodifiableMap(statNodes);
                    }
                }
            }
        }
        return true;
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
        try {
            Thread.sleep(5000);
        }
        catch (final InterruptedException e1) {
            // NOOP
        }
        LOG.debug("StatCollector {} Start collecting!", name);
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
                LOG.debug("STAT-MANAGER {}: last all NET statistics collection cost {} ms", name, statFinalTime);
                if (statFinalTime < minReqNetInterval) {
                    LOG.trace("statCollector is about to make a collecting sleep");
                    synchronized (statCollectorLock) {
                        wakeMe = true;
                        try {
                            final long waitTime = minReqNetInterval - statFinalTime;
                            statCollectorLock.wait(waitTime);
                            LOG.trace("STAT-MANAGER : statCollector {} is waking up from a collecting sleep for {} ms", name, waitTime);
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
                    manager.getRpcMsgManager().getAllGroupsConfStats(actualNodeRef);
                    waitingForNotification();
                    manager.getRpcMsgManager().getAllGroupsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case METER_STATS:
                    LOG.trace("STAT-MANAGER-collecting METER-STATS for NodeRef {}", actualNodeRef);
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

    private boolean isNodeIdentValidForUse(final InstanceIdentifier<Node> ident) {
        if (ident == null) {
            LOG.warn("FlowCapableNode InstanceIdentifier {} can not be null!");
            return false;
        }
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode InstanceIdentifier {} can not be wildcarded!", ident);
            return false;
        }
        return true;
    }
}

