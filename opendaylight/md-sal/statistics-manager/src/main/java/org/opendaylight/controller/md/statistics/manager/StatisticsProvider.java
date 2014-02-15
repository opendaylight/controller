/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Following are main responsibilities of the class:
 * 1) Invoke statistics request thread to send periodic statistics request to all the
 * flow capable switch connected to the controller. It sends statistics request for
 * Group,Meter,Table,Flow,Queue,Aggregate stats.
 *
 * 2) Invoke statistics ager thread, to clean up all the stale statistics data from
 * operational data store.
 *
 * @author avishnoi@in.ibm.com
 *
 */
public class StatisticsProvider implements AutoCloseable {
    private static final Logger spLogger = LoggerFactory.getLogger(StatisticsProvider.class);

    private final ConcurrentMap<NodeId, NodeStatisticsHandler> handlers = new ConcurrentHashMap<>();
    private final Timer timer = new Timer("statistics-manager", true);
    private final DataProviderService dps;

    private OpendaylightGroupStatisticsService groupStatsService;

    private OpendaylightMeterStatisticsService meterStatsService;

    private OpendaylightFlowStatisticsService flowStatsService;

    private OpendaylightPortStatisticsService portStatsService;

    private OpendaylightFlowTableStatisticsService flowTableStatsService;

    private OpendaylightQueueStatisticsService queueStatsService;

    public StatisticsProvider(final DataProviderService dataService) {
        this.dps = Preconditions.checkNotNull(dataService);
    }

    private final StatisticsListener updateCommiter = new StatisticsListener(StatisticsProvider.this);

    private Registration<NotificationListener> listenerRegistration;

    private ListenerRegistration<DataChangeListener> flowCapableTrackerRegistration;

    public void start(final NotificationProviderService nps, final RpcConsumerRegistry rpcRegistry) {

        // Get Group/Meter statistics service instances
        groupStatsService = rpcRegistry.getRpcService(OpendaylightGroupStatisticsService.class);
        meterStatsService = rpcRegistry.getRpcService(OpendaylightMeterStatisticsService.class);
        flowStatsService = rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class);
        portStatsService = rpcRegistry.getRpcService(OpendaylightPortStatisticsService.class);
        flowTableStatsService = rpcRegistry.getRpcService(OpendaylightFlowTableStatisticsService.class);
        queueStatsService = rpcRegistry.getRpcService(OpendaylightQueueStatisticsService.class);

        // Start receiving notifications
        this.listenerRegistration = nps.registerNotificationListener(this.updateCommiter);

        // Register for switch connect/disconnect notifications
        final InstanceIdentifier<FlowCapableNode> fcnId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).build();
        spLogger.debug("Registering FlowCapable tracker to {}", fcnId);
        this.flowCapableTrackerRegistration = dps.registerDataChangeListener(fcnId,
                new FlowCapableTracker(this, fcnId));

        spLogger.info("Statistics Provider started.");
    }

    /**
     * Get the handler for a particular node.
     *
     * @param nodeId source node
     * @return Node statistics handler for that node. Null if the statistics should
     *         not handled.
     */
    public final NodeStatisticsHandler getStatisticsHandler(final NodeId nodeId) {
        Preconditions.checkNotNull(nodeId);
        NodeStatisticsHandler handler = handlers.get(nodeId);
        if (handler == null) {
            spLogger.info("Attempted to get non-existing handler for {}", nodeId);
        }
        return handler;
    }

    @Override
    public void close() {
        try {
            if (this.listenerRegistration != null) {
                this.listenerRegistration.close();
                this.listenerRegistration = null;
            }
            if (this.flowCapableTrackerRegistration != null) {
                this.flowCapableTrackerRegistration.close();
                this.flowCapableTrackerRegistration = null;
            }
            timer.cancel();
        } catch (Exception e) {
            spLogger.warn("Failed to stop Statistics Provider completely", e);
        } finally {
            spLogger.info("Statistics Provider stopped.");
        }
    }

    void startNodeHandlers(final Collection<NodeKey> addedNodes) {
        for (NodeKey key : addedNodes) {
            if (handlers.containsKey(key.getId())) {
                spLogger.warn("Attempted to start already-existing handler for {}, very strange", key.getId());
                continue;
            }

            final NodeStatisticsHandler h = new NodeStatisticsHandler(dps, key,
                    flowStatsService, flowTableStatsService, groupStatsService,
                    meterStatsService, portStatsService, queueStatsService);
            final NodeStatisticsHandler old = handlers.putIfAbsent(key.getId(), h);
            if (old == null) {
                spLogger.debug("Started node handler for {}", key.getId());
                h.start(timer);
            } else {
                spLogger.debug("Prevented race on handler for {}", key.getId());
            }
        }
    }

    void stopNodeHandlers(final Collection<NodeKey> removedNodes) {
        for (NodeKey key : removedNodes) {
            final NodeStatisticsHandler s = handlers.remove(key.getId());
            if (s != null) {
                spLogger.debug("Stopping node handler for {}", key.getId());
                s.close();
            } else {
                spLogger.warn("Attempted to remove non-existing handler for {}, very strange", key.getId());
            }
        }
    }
}
