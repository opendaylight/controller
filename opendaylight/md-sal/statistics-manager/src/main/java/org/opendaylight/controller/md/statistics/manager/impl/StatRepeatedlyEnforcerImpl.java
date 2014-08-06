package org.opendaylight.controller.md.statistics.manager.impl;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * Thread base statistic collector. Class holds internal map for all registered
 * (means connected) nodes with List of Switch capabilities;
 * Statistics collecting process get cross whole Network Device by device
 * and statistic by statistic (follow Switch capabilities to prevent unnecessary
 * ask) Next statistic start collecting by notification or by timeout.
 *
 * @author @author avishnoi@in.ibm.com <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatRepeatedlyEnforcerImpl implements StatRepeatedlyEnforcer {

    private final static Logger LOG = LoggerFactory.getLogger(StatRepeatedlyEnforcerImpl.class);

    private final static long STAT_COLLECT_TIME_OUT = 20000L;

    private final Object statCollectorLock = new Object();
    private final StatisticsManager manager;
    private final long minReqNetInterv;



    private Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNodeHolder =
            Collections.<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> emptyMap();

    private volatile boolean finishing = false;
    private volatile boolean wakeMe = false;

    private InstanceIdentifier<? extends DataObject> actualLockedNode;

    public StatRepeatedlyEnforcerImpl(final StatisticsManager manager, final long minReqNetInterv) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticsManager can not be null!");
        this.minReqNetInterv = minReqNetInterv;
    }

    /**
     * finish collecting statistics
     */
    @Override
    public void shutdown() {
        finishing = true;
        collectNextStatistics();
        statNodeHolder = null;
    }

    @Override
    public boolean isProvidedIdentLocked(
            final InstanceIdentifier<? extends DataObject> nodeIdent) {
        if (actualLockedNode != null && nodeIdent != null) {
            return nodeIdent.contains(actualLockedNode);
        }
        return false;
    }

    @Override
    public boolean isProvidedFlowNodeActive(
            final InstanceIdentifier<FlowCapableNode> flowNode) {
        return statNodeHolder.containsKey(flowNode);
    }

    @Override
    public void connectedNodeRegistration(final InstanceIdentifier<FlowCapableNode> ident,
            final List<StatCapabTypes> statTypes) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} registration can not be wildcarded!", ident);
        } else {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (statCollectorLock) {
                    if ( ! statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.put(ident, statTypes);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                }
            }
        }
    }

    @Override
    public void disconnectedNodeUnregistration(final InstanceIdentifier<FlowCapableNode> ident) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} unregistration can not be wildcarded!", ident);
        } else {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (statCollectorLock) {
                    if ( ! statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.remove(ident);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
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
                    LOG.debug("pinging message harvester in starve status");
                    statCollectorLock.notify();
                }
            }
        }
    }

    @Override
    public void run() {
         /* Neverending cyle - wait for interruption */
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
                if (statFinalTime < minReqNetInterv) {
                    LOG.trace("statCollector is about to make a collecting sleep");
                    synchronized (statCollectorLock) {
                        wakeMe = true;
                        try {
                            statCollectorLock.wait(minReqNetInterv - statFinalTime);
                            LOG.trace("statCollector is waking up from a collecting sleep");
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
        for (final Entry<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> nodeEntity : statNodeHolder.entrySet()) {
            final List<StatCapabTypes> listNeededStat = nodeEntity.getValue();
            final InstanceIdentifier<FlowCapableNode> actualNodeIdent = nodeEntity.getKey();
            setActualLockedNode(actualNodeIdent);
            final NodeRef actualNodeRef = new NodeRef(actualNodeIdent);
            for (final StatCapabTypes statMarker : listNeededStat) {
                switch (statMarker) {
                case PORT_STATS:
                    manager.getDeviceMsgManager().getAllPortsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case QUEUE_STATS:
                    manager.getDeviceMsgManager().getAllQueueStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case TABLE_STATS:
                    manager.getDeviceMsgManager().getAllTablesStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case GROUP_STATS:
                    manager.getDeviceMsgManager().getAllGroupsConfStats(actualNodeRef);
                    waitingForNotification();
                    manager.getDeviceMsgManager().getAllGroupsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case METER_STATS:
                    manager.getDeviceMsgManager().getAllMeterConfigStat(actualNodeRef);
                    waitingForNotification();
                    manager.getDeviceMsgManager().getAllMetersStat(actualNodeRef);
                    waitingForNotification();
                    break;
                case FLOW_STATS:
                    manager.getDeviceMsgManager().getAggregateFlowStat(actualNodeRef);
                    waitingForNotification();
                    manager.getDeviceMsgManager().getAllFlowsStat(actualNodeRef);
                    waitingForNotification();
                    break;
                default:
                    /* Exception for programmers in implementation cycle */
                    throw new IllegalStateException("Not implemented ASK for " + statMarker);
                }
            }
        }
    }

    private synchronized void setActualLockedNode (final InstanceIdentifier<? extends DataObject> ident) {
        actualLockedNode = ident;
    }
}

