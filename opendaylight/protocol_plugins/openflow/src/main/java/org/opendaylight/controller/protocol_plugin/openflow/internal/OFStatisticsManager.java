
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsManager;
import org.opendaylight.controller.protocol_plugin.openflow.IStatisticsListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6Match;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6StatsReply;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6StatsRequest;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFVendorStatistics;
import org.openflow.util.HexString;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It periodically polls the different OF statistics from the OF switches
 * and caches them for quick retrieval for the above layers' modules
 * It also provides an API to directly query the switch about the statistics
 */
public class OFStatisticsManager implements IOFStatisticsManager,
        IInventoryShimExternalListener, CommandProvider {
    private static final Logger log = LoggerFactory
            .getLogger(OFStatisticsManager.class);
    private static final int initialSize = 64;
    private static final long flowStatsPeriod = 10000;
    private static final long descriptionStatsPeriod = 60000;
    private static final long portStatsPeriod = 5000;
    private static final long tickPeriod = 1000;
    private static short statisticsTickNumber = (short) (flowStatsPeriod / tickPeriod);
    private static short descriptionTickNumber = (short) (descriptionStatsPeriod / tickPeriod);
    private static short portTickNumber = (short) (portStatsPeriod / tickPeriod);
    private static short factoredSamples = (short) 2;
    private static short counter = 1;
    private IController controller = null;
    private ConcurrentMap<Long, List<OFStatistics>> flowStatistics;
    private ConcurrentMap<Long, List<OFStatistics>> descStatistics;
    private ConcurrentMap<Long, List<OFStatistics>> portStatistics;
    private List<OFStatistics> dummyList;
    private ConcurrentMap<Long, StatisticsTicks> statisticsTimerTicks;
    protected BlockingQueue<StatsRequest> pendingStatsRequests;
    protected BlockingQueue<Long> switchPortStatsUpdated;
    private Thread statisticsCollector;
    private Thread txRatesUpdater;
    private Timer statisticsTimer;
    private TimerTask statisticsTimerTask;
    private ConcurrentMap<Long, Boolean> switchSupportsVendorExtStats;
    private Map<Long, Map<Short, TxRates>> txRates; // Per port sampled (every portStatsPeriod) transmit rate
    private Set<IStatisticsListener> descriptionListeners;

    /**
     * The object containing the latest factoredSamples tx rate samples
     * for a given switch port
     */
    protected class TxRates {
        Deque<Long> sampledTxBytes; // contains the latest factoredSamples sampled transmitted bytes

        public TxRates() {
            sampledTxBytes = new LinkedBlockingDeque<Long>();
        }

        public void update(Long txBytes) {
            /*
             * Based on how many samples our average works on,
             * we might have to remove the oldest sample
             */
            if (sampledTxBytes.size() == factoredSamples) {
                sampledTxBytes.removeLast();
            }

            // Add the latest sample to the top of the queue
            sampledTxBytes.addFirst(txBytes);
        }

        /**
         * Returns the average transmit rate in bps
         * @return the average transmit rate [bps]
         */
        public long getAverageTxRate() {
            long average = 0;
            /*
             * If we cannot provide the value for the time window length set
             */
            if (sampledTxBytes.size() < factoredSamples) {
                return average;
            }
            long increment = (long) (sampledTxBytes.getFirst() - sampledTxBytes
                    .getLast());
            long timePeriod = (long) (factoredSamples * portStatsPeriod)
                    / (long) tickPeriod;
            average = (8 * increment) / timePeriod;
            return average;
        }
    }

    public void setController(IController core) {
        this.controller = core;
    }

    public void unsetController(IController core) {
        if (this.controller == core) {
            this.controller = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    	flowStatistics = new ConcurrentHashMap<Long, List<OFStatistics>>();
        descStatistics = new ConcurrentHashMap<Long, List<OFStatistics>>();
        portStatistics = new ConcurrentHashMap<Long, List<OFStatistics>>();
        dummyList = new ArrayList<OFStatistics>(1);
        statisticsTimerTicks = new ConcurrentHashMap<Long, StatisticsTicks>(
                initialSize);
        pendingStatsRequests = new LinkedBlockingQueue<StatsRequest>(
                initialSize);
        switchPortStatsUpdated = new LinkedBlockingQueue<Long>(initialSize);
        switchSupportsVendorExtStats = new ConcurrentHashMap<Long, Boolean>(
                initialSize);
        txRates = new HashMap<Long, Map<Short, TxRates>>(initialSize);
        descriptionListeners = new HashSet<IStatisticsListener>();

        // Initialize managed timers
        statisticsTimer = new Timer();
        statisticsTimerTask = new TimerTask() {
            @Override
            public void run() {
                decrementTicks();
            }
        };

        // Initialize Statistics collector thread
        statisticsCollector = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        StatsRequest req = pendingStatsRequests.take();
                        acquireStatistics(req.switchId, req.type);
                    } catch (InterruptedException e) {
                        log.warn("Flow Statistics Collector thread " +
                        		"interrupted");
                    }
                }
            }
        }, "Statistics Collector");

        // Initialize Tx Rate Updater thread
        txRatesUpdater = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        long switchId = switchPortStatsUpdated.take();
                        updatePortsTxRate(switchId);
                    } catch (InterruptedException e) {
                        log.warn("TX Rate Updater thread interrupted");
                    }
                }
            }
        }, "TX Rate Updater");
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        // Start managed timers
        statisticsTimer.scheduleAtFixedRate(statisticsTimerTask, 0, tickPeriod);

        // Start statistics collector thread
        statisticsCollector.start();

        // Start bandwidth utilization computer thread
        txRatesUpdater.start();

        // OSGI console
        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        // Stop managed timers
        statisticsTimer.cancel();
    }

    public void setStatisticsListener(IStatisticsListener s) {
    	this.descriptionListeners.add(s);
    }
    
    public void unsetStatisticsListener(IStatisticsListener s) {
    	if (s != null) {
    		this.descriptionListeners.remove(s);
    	}
    }
    
    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    private static class StatsRequest {
        protected Long switchId;
        protected OFStatisticsType type;

        public StatsRequest(Long d, OFStatisticsType t) {
            switchId = d;
            type = t;
        }

        public String toString() {
            return "SReq = {switchId=" + switchId + ", type=" + type + "}";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((switchId == null) ? 0 : switchId.hashCode());
            result = prime * result + ((type == null) ? 0 : type.ordinal());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            StatsRequest other = (StatsRequest) obj;
            if (switchId == null) {
                if (other.switchId != null) {
                    return false;
                }
            } else if (!switchId.equals(other.switchId)) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            return true;
        }
    }

    private void addStatisticsTicks(Long switchId) {
        switchSupportsVendorExtStats.put(switchId, Boolean.TRUE); // Assume switch supports Vendor extension stats
        statisticsTimerTicks.put(switchId, new StatisticsTicks(true));
        log.info("Added Switch {} to target pool", HexString
                .toHexString(switchId.longValue()));
    }

    protected static class StatisticsTicks {
        private short flowStatisticsTicks;
        private short descriptionTicks;
        private short portStatisticsTicks;

        public StatisticsTicks(boolean scattered) {
            if (scattered) {
                // scatter bursts by statisticsTickPeriod
                if (++counter < 0) {
                    counter = 0;
                } // being paranoid here
                flowStatisticsTicks = (short) (1 + counter
                        % statisticsTickNumber);
                descriptionTicks = (short) (1 + counter % descriptionTickNumber);
                portStatisticsTicks = (short) (1 + counter % portTickNumber);
            } else {
                flowStatisticsTicks = statisticsTickNumber;
                descriptionTicks = descriptionTickNumber;
                portStatisticsTicks = portTickNumber;
            }
        }

        public boolean decrementFlowTicksIsZero() {
            // Please ensure no code is inserted between the if check and the flowStatisticsTicks reset
            if (--flowStatisticsTicks == 0) {
                flowStatisticsTicks = statisticsTickNumber;
                return true;
            }
            return false;
        }

        public boolean decrementDescTicksIsZero() {
            // Please ensure no code is inserted between the if check and the descriptionTicks reset
            if (--descriptionTicks == 0) {
                descriptionTicks = descriptionTickNumber;
                return true;
            }
            return false;
        }

        public boolean decrementPortTicksIsZero() {
            // Please ensure no code is inserted between the if check and the descriptionTicks reset
            if (--portStatisticsTicks == 0) {
                portStatisticsTicks = portTickNumber;
                return true;
            }
            return false;
        }

        public String toString() {
            return "{fT=" + flowStatisticsTicks + ",dT=" + descriptionTicks
                    + ",pT=" + portStatisticsTicks + "}";
        }
    }

    private void printInfoMessage(String type, StatsRequest request) {
        log
                .info(
                        type
                                + " stats request not inserted for switch: {}. Queue size: {}. Collector state: {}.",
                        new Object[] { HexString.toHexString(request.switchId),
                                pendingStatsRequests.size(),
                                statisticsCollector.getState().toString() });
    }

    protected void decrementTicks() {
        StatsRequest request = null;
        for (Map.Entry<Long, StatisticsTicks> entry : statisticsTimerTicks
                .entrySet()) {
            StatisticsTicks clock = entry.getValue();
            Long switchId = entry.getKey();
            if (clock.decrementFlowTicksIsZero() == true) {
                request = (switchSupportsVendorExtStats.get(switchId) == Boolean.TRUE) ? new StatsRequest(
                        switchId, OFStatisticsType.VENDOR)
                        : new StatsRequest(switchId, OFStatisticsType.FLOW);
                // If a request for this switch is already in the queue, skip to add this new request
                if (!pendingStatsRequests.contains(request)
                        && false == pendingStatsRequests.offer(request)) {
                    printInfoMessage("Flow", request);
                }
            }

            if (clock.decrementDescTicksIsZero() == true) {
                request = new StatsRequest(switchId, OFStatisticsType.DESC);
                // If a request for this switch is already in the queue, skip to add this new request
                if (!pendingStatsRequests.contains(request)
                        && false == pendingStatsRequests.offer(request)) {
                    printInfoMessage("Description", request);
                }
            }

            if (clock.decrementPortTicksIsZero() == true) {
                request = new StatsRequest(switchId, OFStatisticsType.PORT);
                // If a request for this switch is already in the queue, skip to add this new request
                if (!pendingStatsRequests.contains(request)
                        && false == pendingStatsRequests.offer(request)) {
                    printInfoMessage("Port", request);
                }
            }
        }
    }

    private void removeStatsRequestTasks(Long switchId) {
        log.info("Cleaning Statistics database for switch "
                + HexEncode.longToHexString(switchId));
        // To be safe, let's attempt removal of both VENDOR and FLOW request. It does not hurt
        pendingStatsRequests.remove(new StatsRequest(switchId,
                OFStatisticsType.VENDOR));
        pendingStatsRequests.remove(new StatsRequest(switchId,
                OFStatisticsType.FLOW));
        pendingStatsRequests.remove(new StatsRequest(switchId,
                OFStatisticsType.DESC));
        pendingStatsRequests.remove(new StatsRequest(switchId,
                OFStatisticsType.PORT));
        // Take care of the TX rate databases
        switchPortStatsUpdated.remove(switchId);
        txRates.remove(switchId);
    }

    private void clearFlowStatsAndTicks(Long switchId) {
        statisticsTimerTicks.remove(switchId);
        removeStatsRequestTasks(switchId);
        flowStatistics.remove(switchId);
        log.info("Statistics removed for switch "
                + HexString.toHexString(switchId));
    }

    private void acquireStatistics(Long switchId, OFStatisticsType statType) {

        // Query the switch on all matches
        List<OFStatistics> values = this.acquireStatistics(switchId, statType,
                null);

        // Update local caching database if got a valid response
        if (values != null && !values.isEmpty()) {
            if ((statType == OFStatisticsType.FLOW)
                    || (statType == OFStatisticsType.VENDOR)) {
                flowStatistics.put(switchId, values);
            } else if (statType == OFStatisticsType.DESC) {
            	// Notify who may be interested in a description change
           		notifyDescriptionListeners(switchId, values);
            	
                // Overwrite cache
                descStatistics.put(switchId, values);
            } else if (statType == OFStatisticsType.PORT) {
                // Overwrite cache with new port statistics for this switch
                portStatistics.put(switchId, values);

                // Wake up the thread which maintains the TX byte counters for each port
                switchPortStatsUpdated.offer(switchId);
            }
        }
    }

    private void notifyDescriptionListeners(Long switchId,
    								List<OFStatistics> values) {
		for (IStatisticsListener l : this.descriptionListeners) {
			l.descriptionRefreshed(switchId, 
					((OFDescriptionStatistics)values.get(0)));
		}
    }
    
    /*
     * Generic function to get the statistics form a OF switch
     */
    @SuppressWarnings("unchecked")
    private List<OFStatistics> acquireStatistics(Long switchId,
            OFStatisticsType statsType, Object target) {
        List<OFStatistics> values = null;
        String type = null;
        ISwitch sw = controller.getSwitch(switchId);

        if (sw != null) {
            OFStatisticsRequest req = new OFStatisticsRequest();
            req.setStatisticType(statsType);
            int requestLength = req.getLengthU();

            if (statsType == OFStatisticsType.FLOW) {
                OFMatch match = null;
                if (target == null) {
                    // All flows request
                    match = new OFMatch();
                    match.setWildcards(0xffffffff);
                } else if (!(target instanceof OFMatch)) {
                    // Malformed request
                    log.warn("Invalid target type for Flow stats request: "
                            + target.getClass());
                    return null;
                } else {
                    // Specific flow request
                    match = (OFMatch) target;
                }
                OFFlowStatisticsRequest specificReq = new OFFlowStatisticsRequest();
                specificReq.setMatch(match);
                specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
                specificReq.setTableId((byte) 0xff);
                req.setStatistics(Collections
                        .singletonList((OFStatistics) specificReq));
                requestLength += specificReq.getLength();
                type = "FLOW";
            } else if (statsType == OFStatisticsType.VENDOR) {
                V6StatsRequest specificReq = new V6StatsRequest();
                specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
                specificReq.setTableId((byte) 0xff);
                req.setStatistics(Collections
                        .singletonList((OFStatistics) specificReq));
                requestLength += specificReq.getLength();
                type = "VENDOR";
            } else if (statsType == OFStatisticsType.AGGREGATE) {
                OFAggregateStatisticsRequest specificReq = new OFAggregateStatisticsRequest();
                OFMatch match = new OFMatch();
                match.setWildcards(0xffffffff);
                specificReq.setMatch(match);
                specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
                specificReq.setTableId((byte) 0xff);
                req.setStatistics(Collections
                        .singletonList((OFStatistics) specificReq));
                requestLength += specificReq.getLength();
                type = "AGGREGATE";
            } else if (statsType == OFStatisticsType.PORT) {
                short targetPort;
                if (target == null) {
                    // All ports request
                    targetPort = (short) OFPort.OFPP_NONE.getValue();
                } else if (!(target instanceof Short)) {
                    // Malformed request
                    log.warn("Invalid target type for Port stats request: "
                            + target.getClass());
                    return null;
                } else {
                    // Specific port request
                    targetPort = (Short) target;
                }
                OFPortStatisticsRequest specificReq = new OFPortStatisticsRequest();
                specificReq.setPortNumber(targetPort);
                req.setStatistics(Collections
                        .singletonList((OFStatistics) specificReq));
                requestLength += specificReq.getLength();
                type = "PORT";
            } else if (statsType == OFStatisticsType.QUEUE) {
                OFQueueStatisticsRequest specificReq = new OFQueueStatisticsRequest();
                specificReq.setPortNumber((short) OFPort.OFPP_ALL.getValue());
                specificReq.setQueueId(0xffffffff);
                req.setStatistics(Collections
                        .singletonList((OFStatistics) specificReq));
                requestLength += specificReq.getLength();
                type = "QUEUE";
            } else if (statsType == OFStatisticsType.DESC) {
                type = "DESC";
            } else if (statsType == OFStatisticsType.TABLE) {
                type = "TABLE";
            }
            req.setLengthU(requestLength);
            Object result = sw.getStatistics(req);

            if (result == null) {
                log.warn("Request Timed Out for ({}) from switch {}", type,
                        HexString.toHexString(switchId));
            } else if (result instanceof OFError) {
                log.warn("Switch {} failed to handle ({}) stats request: "
                        + Utils.getOFErrorString((OFError) result), HexString
                        .toHexString(switchId), type);
                if (this.switchSupportsVendorExtStats.get(switchId) == Boolean.TRUE) {
                    log
                            .warn(
                                    "Switching back to regular Flow stats requests for switch {}",
                                    HexString.toHexString(switchId));
                    this.switchSupportsVendorExtStats.put(switchId,
                            Boolean.FALSE);
                }
            } else {
                values = (List<OFStatistics>) result;
            }
        }
        return values;
    }

    @Override
    public List<OFStatistics> getOFFlowStatistics(Long switchId) {
        List<OFStatistics> list = flowStatistics.get(switchId);

        /*
         *  Check on emptiness as interference between add and get is still
         *  possible on the inner list (the concurrentMap entry's value)
         */
        return (list == null || list.isEmpty()) ? this.dummyList
                : (list.get(0) instanceof OFVendorStatistics) ? this
                        .v6StatsListToOFStatsList(list) : list;
    }

    @Override
    public List<OFStatistics> getOFFlowStatistics(Long switchId, OFMatch ofMatch) {
        List<OFStatistics> statsList = flowStatistics.get(switchId);

        /*
         *  Check on emptiness as interference between add and get is still
         *  possible on the inner list (the concurrentMap entry's value)
         */
        if (statsList == null || statsList.isEmpty()) {
            return this.dummyList;
        }

        if (statsList.get(0) instanceof OFVendorStatistics) {
            /*
             * Caller could provide regular OF match when we
             * instead pull the vendor statistics from this node
             * Caller is not supposed to know whether this switch supports
             * vendor extensions statistics requests
             */
            V6Match targetMatch = (ofMatch instanceof V6Match) ? (V6Match) ofMatch
                    : new V6Match(ofMatch);

            List<OFStatistics> targetList = v6StatsListToOFStatsList(statsList);
            for (OFStatistics stats : targetList) {
                V6StatsReply v6Stats = (V6StatsReply) stats;
                V6Match v6Match = v6Stats.getMatch();
                if (v6Match.equals(targetMatch)) {
                    List<OFStatistics> list = new ArrayList<OFStatistics>();
                    list.add(stats);
                    return list;
                }
            }
        } else {
            for (OFStatistics stats : statsList) {
                OFFlowStatisticsReply flowStats = (OFFlowStatisticsReply) stats;
                if (flowStats.getMatch().equals(ofMatch)) {
                    List<OFStatistics> list = new ArrayList<OFStatistics>();
                    list.add(stats);
                    return list;
                }
            }
        }
        return this.dummyList;
    }

    /*
     * Converts the v6 vendor statistics to the OFStatistics
     */
    private List<OFStatistics> v6StatsListToOFStatsList(
            List<OFStatistics> statistics) {
        List<OFStatistics> v6statistics = new ArrayList<OFStatistics>();
        if (statistics != null && !statistics.isEmpty()) {
            for (OFStatistics stats : statistics) {
                if (stats instanceof OFVendorStatistics) {
                    List<OFStatistics> r = getV6ReplyStatistics((OFVendorStatistics) stats);
                    if (r != null) {
                        v6statistics.addAll(r);
                    }
                }
            }
        }
        return v6statistics;
    }

    private static List<OFStatistics> getV6ReplyStatistics(
            OFVendorStatistics stat) {
        int length = stat.getLength();
        List<OFStatistics> results = new ArrayList<OFStatistics>();
        if (length < 12)
            return null; // Nicira Hdr is 12 bytes. We need atleast that much
        ByteBuffer data = ByteBuffer.allocate(length);
        stat.writeTo(data);
        data.rewind();
        log.trace("getV6ReplyStatistics: Buffer BYTES ARE {}", HexString
                .toHexString(data.array()));

        int vendor = data.getInt(); //first 4 bytes is vendor id.
        if (vendor != V6StatsRequest.NICIRA_VENDOR_ID) {
            log
                    .debug("Unexpected vendor id: 0x{}", Integer
                            .toHexString(vendor));
            return null;
        } else {
            //go ahead by 8 bytes which is 8 bytes of 0
            data.getLong(); //should be all 0's
            length -= 12; // 4 bytes Nicira Hdr + 8 bytes from above line have been consumed
        }

        V6StatsReply v6statsreply;
        int min_len;
        while (length > 0) {
            v6statsreply = new V6StatsReply();
            min_len = v6statsreply.getLength();
            if (length < v6statsreply.getLength())
                break;
            v6statsreply.setActionFactory(stat.getActionFactory());
            v6statsreply.readFrom(data);
            if (v6statsreply.getLength() < min_len)
                break;
            v6statsreply.setVendorId(vendor);
            log.trace("V6StatsReply: {}", v6statsreply);
            length -= v6statsreply.getLength();
            results.add(v6statsreply);
        }
        return results;
    }

    @Override
    public List<OFStatistics> queryStatistics(Long switchId,
            OFStatisticsType statType, Object target) {
        /*
         * Caller does not know and it is not supposed to know whether
         * this switch supports vendor extension. We adjust the target for him
         */
        if (statType == OFStatisticsType.FLOW) {
            if (switchSupportsVendorExtStats.get(switchId) == Boolean.TRUE) {
                statType = OFStatisticsType.VENDOR;
            }
        }

        List<OFStatistics> list = this.acquireStatistics(switchId, statType,
                target);

        return (list == null) ? null
                : (statType == OFStatisticsType.VENDOR) ? v6StatsListToOFStatsList(list)
                        : list;
    }

    @Override
    public List<OFStatistics> getOFDescStatistics(Long switchId) {
        if (!descStatistics.containsKey(switchId))
            return this.dummyList;

        return descStatistics.get(switchId);
    }

    @Override
    public List<OFStatistics> getOFPortStatistics(Long switchId) {
        if (!portStatistics.containsKey(switchId)) {
            return this.dummyList;
        }

        return portStatistics.get(switchId);
    }

    @Override
    public List<OFStatistics> getOFPortStatistics(Long switchId, short portId) {
        if (!portStatistics.containsKey(switchId)) {
            return this.dummyList;
        }
        List<OFStatistics> list = new ArrayList<OFStatistics>(1);
        for (OFStatistics stats : portStatistics.get(switchId)) {
            if (((OFPortStatisticsReply) stats).getPortNumber() == portId) {
                list.add(stats);
                break;
            }
        }
        return list;
    }

    @Override
    public int getFlowsNumber(long switchId) {
        return this.flowStatistics.get(switchId).size();
    }

    /*
     * InventoryShim replay for us all the switch addition which happened before we were brought up
     */
    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        Long switchId = (Long) node.getID();
        switch (type) {
        case ADDED:
            addStatisticsTicks(switchId);
            break;
        case REMOVED:
            clearFlowStatsAndTicks(switchId);
        default:
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        // No action
    }

    /**
     * Update the cached port rates for this switch with the latest
     * retrieved port transmit byte count
     * @param switchId
     */
    private synchronized void updatePortsTxRate(long switchId) {
        List<OFStatistics> newPortStatistics = this.portStatistics
                .get(switchId);
        if (newPortStatistics == null) {
            return;
        }
        Map<Short, TxRates> rates = this.txRates.get(switchId);
        if (rates == null) {
            // First time rates for this switch are added
            rates = new HashMap<Short, TxRates>();
            txRates.put(switchId, rates);
        }
        for (OFStatistics stats : newPortStatistics) {
            OFPortStatisticsReply newPortStat = (OFPortStatisticsReply) stats;
            short port = newPortStat.getPortNumber();
            TxRates portRatesHolder = rates.get(port);
            if (portRatesHolder == null) {
                // First time rates for this port are added
                portRatesHolder = new TxRates();
                rates.put(port, portRatesHolder);
            }
            // Get and store the number of transmitted bytes for this port
            // And handle the case where agent does not support the counter
            long transmitBytes = newPortStat.getTransmitBytes();
            long value = (transmitBytes < 0) ? 0 : transmitBytes;
            portRatesHolder.update(value);
        }
    }

    @Override
    public synchronized long getTransmitRate(Long switchId, Short port) {
        long average = 0;
        if (switchId == null || port == null) {
            return average;
        }
        Map<Short, TxRates> perSwitch = txRates.get(switchId);
        if (perSwitch == null) {
            return average;
        }
        TxRates portRates = perSwitch.get(port);
        if (portRates == null) {
            return average;
        }
        return portRates.getAverageTxRate();
    }

    /*
     * Manual switch name configuration code
     */
    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---OF Statistics Manager utilities---\n");
        help.append("\t ofdumpstatsmgr         - " + 
        			"Print Internal Stats Mgr db\n");
        return help.toString();
    }

    private boolean isValidSwitchId(String switchId) {
        String regexDatapathID = "^([0-9a-fA-F]{1,2}[:-]){7}[0-9a-fA-F]{1,2}$";
        String regexDatapathIDLong = "^[0-9a-fA-F]{1,16}$";

        return (switchId != null && (switchId.matches(regexDatapathID) || switchId
                .matches(regexDatapathIDLong)));
    }

    public long getSwitchIDLong(String switchId) {
        int radix = 16;
        String switchString = "0";

        if (isValidSwitchId(switchId)) {
            if (switchId.contains(":")) {
                // Handle the 00:00:AA:BB:CC:DD:EE:FF notation
                switchString = switchId.replace(":", "");
            } else if (switchId.contains("-")) {
                // Handle the 00-00-AA-BB-CC-DD-EE-FF notation
                switchString = switchId.replace("-", "");
            } else {
                // Handle the 0123456789ABCDEF notation
                switchString = switchId;
            }
        }
        return Long.parseLong(switchString, radix);
    }

    /*
     * Internal information dump code
     */
    private String prettyPrintSwitchMap(ConcurrentMap<Long, StatisticsTicks> map) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        for (Entry<Long, StatisticsTicks> entry : map.entrySet()) {
            buffer.append(HexString.toHexString(entry.getKey()) + "="
                    + entry.getValue().toString() + " ");
        }
        buffer.append("}");
        return buffer.toString();
    }

    public void _ofdumpstatsmgr(CommandInterpreter ci) {
        ci.println("Global Counter: " + counter);
        ci
                .println("Timer Ticks: "
                        + prettyPrintSwitchMap(statisticsTimerTicks));
        ci.println("PendingStatsQueue: " + pendingStatsRequests);
        ci.println("PendingStatsQueue size: " + pendingStatsRequests.size());
        ci.println("Stats Collector alive: " + statisticsCollector.isAlive());
        ci.println("Stats Collector State: "
                + statisticsCollector.getState().toString());
        ci.println("StatsTimer: " + statisticsTimer.toString());
        ci.println("Flow Stats Period: " + statisticsTickNumber + " s");
        ci.println("Desc Stats Period: " + descriptionTickNumber + " s");
        ci.println("Port Stats Period: " + portTickNumber + " s");
    }

    public void _resetSwitchCapability(CommandInterpreter ci) {
        String sidString = ci.nextArgument();
        Long sid = null;
        if (sidString == null) {
            ci.println("Insert the switch id (numeric value)");
            return;
        }
        try {
            sid = Long.valueOf(sidString);
            this.switchSupportsVendorExtStats.put(sid, Boolean.TRUE);
            ci.println("Vendor capability for switch " + sid + " set to "
                    + this.switchSupportsVendorExtStats.get(sid));
        } catch (NumberFormatException e) {
            ci.println("Invalid switch id. Has to be numeric.");
        }

    }

    public void _ofbw(CommandInterpreter ci) {
        String sidString = ci.nextArgument();
        Long sid = null;
        if (sidString == null) {
            ci.println("Insert the switch id (numeric value)");
            return;
        }
        try {
            sid = Long.valueOf(sidString);
        } catch (NumberFormatException e) {
            ci.println("Invalid switch id. Has to be numeric.");
        }
        if (sid != null) {
            Map<Short, TxRates> thisSwitchRates = txRates.get(sid);
            ci.println("Bandwidth utilization (" + factoredSamples
                    * portTickNumber + " sec average) for switch "
                    + HexEncode.longToHexString(sid) + ":");
            if (thisSwitchRates == null) {
                ci.println("Not available");
            } else {
                for (Entry<Short, TxRates> entry : thisSwitchRates.entrySet()) {
                    ci.println("Port: " + entry.getKey() + ": "
                            + entry.getValue().getAverageTxRate() + " bps");
                }
            }
        }
    }

    public void _txratewindow(CommandInterpreter ci) {
        String averageWindow = ci.nextArgument();
        short seconds = 0;
        if (averageWindow == null) {
            ci.println("Insert the length in seconds of the median " + 
            		"window for tx rate");
            ci.println("Current: " + factoredSamples * portTickNumber
                            + " secs");
            return;
        }
        try {
            seconds = Short.valueOf(averageWindow);
        } catch (NumberFormatException e) {
            ci.println("Invalid period.");
        }
        OFStatisticsManager.factoredSamples = (short) (seconds/portTickNumber);
        ci.println("New: " + factoredSamples * portTickNumber + " secs");
    }

    public void _ofstatsmgrintervals(CommandInterpreter ci) {
        String flowStatsInterv = ci.nextArgument();
        String portStatsInterv = ci.nextArgument();
        
        if (flowStatsInterv == null || portStatsInterv == null) {

            ci.println("Usage: ostatsmgrintervals <fP> <pP> (in seconds)");
            ci.println("Current Values: fP=" + statisticsTickNumber +
            		"s pP=" + portTickNumber + "s");
            return;
        }
        Short fP, pP;
        try {
        	fP = Short.parseShort(flowStatsInterv);
        	pP = Short.parseShort(portStatsInterv);
        } catch (Exception e) {
        	ci.println("Invalid format values: " + e.getMessage());
        	return;
        }

        if (pP <= 1 || fP <=1) {
        	ci.println("Invalid values. fP and pP have to be greater than 1.");
        	return;
        }
        
        statisticsTickNumber = fP;
        portTickNumber = pP;
        
        ci.println("New Values: fP=" + statisticsTickNumber +
        		"s pP=" + portTickNumber + "s");
    }

}
