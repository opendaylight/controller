/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.hosttracker.HostIdFactory;
import org.opendaylight.controller.hosttracker.IHostId;
import org.opendaylight.controller.hosttracker.IPHostId;
import org.opendaylight.controller.hosttracker.IPMacHostId;
import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.hosttracker.hostAware.IHostFinder;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file HostTracker.java This class tracks the location of IP Hosts as to which
 *       Switch, Port, VLAN, they are connected to, as well as their MAC
 *       address. This is done dynamically as well as statically. The dynamic
 *       mechanism consists of listening to ARP messages as well sending ARP
 *       requests. Static mechanism consists of Northbound APIs to add or remove
 *       the hosts from the local database. ARP aging is also implemented to age
 *       out dynamically learned hosts. Interface methods are provided for other
 *       applications to 1. Query the local database for a single host 2. Get a
 *       list of all hosts 3. Get notification if a host is learned/added or
 *       removed the database
 */

/***
 *
 * HostTracker db key scheme implementation support. Support has been added for
 * IP only or IP + MAC scheme as of now. User can use either of the schemes
 * based on the configuration done in config.ini file. By default IP only key
 * scheme is choosen. The attribute to be set in config.ini is
 * hosttracker.keyscheme. It could have a value of 0 or 1 as of now. 0 is for IP
 * only scheme. 1 is for IP + MAC scheme.
 *
 *
 */

public class HostTracker implements IfIptoHost, IfHostListener, ISwitchManagerAware, IInventoryListener,
        ITopologyManagerAware, ICacheUpdateAware<IHostId, HostNodeConnector>, CommandProvider {
    static final String ACTIVE_HOST_CACHE = "hosttracker.ActiveHosts";
    static final String INACTIVE_HOST_CACHE = "hosttracker.InactiveHosts";
    private static final Logger logger = LoggerFactory.getLogger(HostTracker.class);
    protected final Set<IHostFinder> hostFinder = new CopyOnWriteArraySet<IHostFinder>();;
    protected ConcurrentMap<IHostId, HostNodeConnector> hostsDB;
    /*
     * Following is a list of hosts which have been requested by NB APIs to be
     * added, but either the switch or the port is not sup, so they will be
     * added here until both come up
     */
    private ConcurrentMap<NodeConnector, HostNodeConnector> inactiveStaticHosts;
    private final Set<IfNewHostNotify> newHostNotify = Collections.synchronizedSet(new HashSet<IfNewHostNotify>());

    private ITopologyManager topologyManager;
    protected IClusterContainerServices clusterContainerService = null;
    protected ISwitchManager switchManager = null;
    private Timer timer;
    private Timer arpRefreshTimer;
    private String containerName = null;
    private ExecutorService executor;
    protected boolean stopping;
    private static boolean hostRefresh = true;
    private static int hostRetryCount = 5;
    private String keyScheme = null;

    private static class ARPPending {
        protected IHostId hostId;
        protected short sent_count;
        protected HostTrackerCallable hostTrackerCallable;

        public IHostId getHostId() {
            return hostId;
        }

        public short getSent_count() {
            return sent_count;
        }

        public HostTrackerCallable getHostTrackerCallable() {
            return hostTrackerCallable;
        }

        public void setHostId(IHostId id) {
            this.hostId = id;
        }

        public void setSent_count(short count) {
            this.sent_count = count;
        }

        public void setHostTrackerCallable(HostTrackerCallable callable) {
            hostTrackerCallable = callable;
        }
    }

    // This list contains the hosts for which ARP requests are being sent
    // periodically
    ConcurrentMap<IHostId, ARPPending> ARPPendingList;
    /*
     * This list below contains the hosts which were initially in ARPPendingList
     * above, but ARP response didn't come from there hosts after multiple
     * attempts over 8 seconds. The assumption is that the response didn't come
     * back due to one of the following possibilities: 1. The L3 interface
     * wasn't created for this host in the controller. This would cause
     * arphandler not to know where to send the ARP 2. The host facing port is
     * down 3. The IP host doesn't exist or is not responding to ARP requests
     *
     * Conditions 1 and 2 above can be recovered if ARP is sent when the
     * relevant L3 interface is added or the port facing host comes up. Whenever
     * L3 interface is added or host facing port comes up, ARP will be sent to
     * hosts in this list.
     *
     * We can't recover from condition 3 above
     */
    ConcurrentMap<IHostId, ARPPending> failedARPReqList;

    public HostTracker() {
    }

    private void startUp() {
        nonClusterObjectCreate();
        allocateCache();
        retrieveCache();
        stopping = false;

        timer = new Timer();
        timer.schedule(new OutStandingARPHandler(), 4000, 4000);
        executor = Executors.newFixedThreadPool(2);
        /* ARP Refresh Timer to go off every 5 seconds to implement ARP aging */
        arpRefreshTimer = new Timer();
        arpRefreshTimer.schedule(new ARPRefreshHandler(), 5000, 5000);
        keyScheme = HostIdFactory.getScheme();
        logger.debug("startUp: Caches created, timers started");
    }

    private void allocateCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't create cache");
            return;
        }
        logger.debug("Creating Cache for HostTracker");
        try {
            this.clusterContainerService.createCache(ACTIVE_HOST_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            this.clusterContainerService.createCache(INACTIVE_HOST_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for HostTracker -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for HostTracker already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for HostTracker");
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }
        logger.debug("Retrieving cache for HostTrackerAH");
        hostsDB = (ConcurrentMap<IHostId, HostNodeConnector>) this.clusterContainerService.getCache(ACTIVE_HOST_CACHE);
        if (hostsDB == null) {
            logger.error("Cache couldn't be retrieved for HostTracker");
        }
        logger.debug("Cache was successfully retrieved for HostTracker");
        logger.debug("Retrieving cache for HostTrackerIH");
        inactiveStaticHosts = (ConcurrentMap<NodeConnector, HostNodeConnector>) this.clusterContainerService
                .getCache(INACTIVE_HOST_CACHE);
        if (inactiveStaticHosts == null) {
            logger.error("Cache couldn't be retrieved for HostTrackerIH");
        }
        logger.debug("Cache was successfully retrieved for HostTrackerIH");
    }

    public void nonClusterObjectCreate() {
        hostsDB = new ConcurrentHashMap<IHostId, HostNodeConnector>();
        inactiveStaticHosts = new ConcurrentHashMap<NodeConnector, HostNodeConnector>();
        ARPPendingList = new ConcurrentHashMap<IHostId, ARPPending>();
        failedARPReqList = new ConcurrentHashMap<IHostId, ARPPending>();
    }

    public void shutDown() {
    }

    public void setnewHostNotify(IfNewHostNotify obj) {
        this.newHostNotify.add(obj);
    }

    public void unsetnewHostNotify(IfNewHostNotify obj) {
        this.newHostNotify.remove(obj);
    }

    public void setArpHandler(IHostFinder hostFinder) {
        if (this.hostFinder != null) {
            this.hostFinder.add(hostFinder);
        }
    }

    public void unsetArpHandler(IHostFinder hostFinder) {
        if (this.hostFinder != null) {
            logger.debug("Arp Handler Service removed!");
            this.hostFinder.remove(hostFinder);
        }
    }

    public void setTopologyManager(ITopologyManager s) {
        this.topologyManager = s;
    }

    public void unsetTopologyManager(ITopologyManager s) {
        if (this.topologyManager == s) {
            logger.debug("Topology Manager Service removed!");
            this.topologyManager = null;
        }
    }

    private boolean hostExists(HostNodeConnector host) {
        IHostId id = HostIdFactory.create(host.getNetworkAddress(), host.getDataLayerAddress());
        HostNodeConnector lhost = hostsDB.get(id);
        return host.equals(lhost);
    }

    private HostNodeConnector getHostFromOnActiveDB(IHostId id) {
        return hostsDB.get(id);
    }

    private Entry<NodeConnector, HostNodeConnector> getHostFromInactiveDB(IHostId id) {
        for (Entry<NodeConnector, HostNodeConnector> entry : inactiveStaticHosts.entrySet()) {
            HostNodeConnector hnc = entry.getValue();
            IHostId cmpId = HostIdFactory.create(hnc.getNetworkAddress(), hnc.getDataLayerAddress());
            if (cmpId.equals(id)) {
                logger.debug("getHostFromInactiveDB(): Inactive Host found for ID:{} ", decodeIPFromId(id));
                return entry;
            }
        }
        logger.debug("getHostFromInactiveDB() Inactive Host Not found for ID: {}", decodeIPFromId(id));
        return null;
    }

    private void removeHostFromInactiveDB(IHostId id) {
        NodeConnector nodeConnector = null;
        for (Entry<NodeConnector, HostNodeConnector> entry : inactiveStaticHosts.entrySet()) {
            HostNodeConnector hnc = entry.getValue();
            IHostId cmpId = HostIdFactory.create(hnc.getNetworkAddress(), hnc.getDataLayerAddress());
            if (cmpId.equals(id)) {
                nodeConnector = entry.getKey();
                break;
            }
        }
        if (nodeConnector != null) {
            inactiveStaticHosts.remove(nodeConnector);
            logger.debug("removeHostFromInactiveDB(): Host Removed for IP: {}", decodeIPFromId(id));
            return;
        }
        logger.debug("removeHostFromInactiveDB(): Host Not found for IP: {}", decodeIPFromId(id));
    }

    protected boolean hostMoved(HostNodeConnector host) {
        IHostId id = HostIdFactory.create(host.getNetworkAddress(), host.getDataLayerAddress());
        if (hostQuery(id) != null) {
            return true;
        }
        return false;
    }

    @Override
    public HostNodeConnector hostQuery(IHostId id) {
        return hostsDB.get(id);
    }

    @Override
    public Future<HostNodeConnector> discoverHost(IHostId id) {
        if (executor == null) {
            logger.debug("discoverHost: Null executor");
            return null;
        }
        Callable<HostNodeConnector> worker = new HostTrackerCallable(this, id);
        Future<HostNodeConnector> submit = executor.submit(worker);
        return submit;
    }

    @Override
    public HostNodeConnector hostFind(IHostId id) {
        /*
         * Sometimes at boot with containers configured in the startup we hit
         * this path (from TIF) when hostFinder has not been set yet Caller
         * already handles the null return
         */

        if (hostFinder == null) {
            logger.debug("Exiting hostFind, null hostFinder");
            return null;
        }

        HostNodeConnector host = hostQuery(id);
        if (host != null) {
            logger.debug("hostFind(): Host found for IP: {}", id);
            return host;
        }

        /* Add this host to ARPPending List for any potential retries */

        addToARPPendingList(id);
        logger.debug("hostFind(): Host Not Found for IP: {}, Inititated Host Discovery ...", id);

        /* host is not found, initiate a discovery */
        for (IHostFinder hf : hostFinder) {
            InetAddress addr = decodeIPFromId(id);
            hf.find(addr);
        }
        return null;
    }

    @Override
    public Set<HostNodeConnector> getAllHosts() {
        Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(hostsDB.values());
        return allHosts;
    }

    @Override
    public Set<HostNodeConnector> getActiveStaticHosts() {
        Set<HostNodeConnector> list = new HashSet<HostNodeConnector>();
        for (Entry<IHostId, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            if (host.isStaticHost()) {
                list.add(host);
            }
        }
        return list;
    }

    @Override
    public Set<HostNodeConnector> getInactiveStaticHosts() {
        Set<HostNodeConnector> list = new HashSet<HostNodeConnector>(inactiveStaticHosts.values());
        return list;
    }

    private void addToARPPendingList(IHostId id) {
        ARPPending arphost = new ARPPending();

        arphost.setHostId(id);
        arphost.setSent_count((short) 1);
        ARPPendingList.put(id, arphost);
        logger.debug("Host Added to ARPPending List, IP: {}", decodeIPFromId(id));

    }

    public void setCallableOnPendingARP(IHostId id, HostTrackerCallable callable) {
        ARPPending arphost;
        for (Entry<IHostId, ARPPending> entry : ARPPendingList.entrySet()) {
            arphost = entry.getValue();
            if (arphost.getHostId().equals(id)) {
                arphost.setHostTrackerCallable(callable);
            }
        }
    }

    private void processPendingARPReqs(IHostId id) {
        ARPPending arphost;

        if ((arphost = ARPPendingList.remove(id)) != null) {
            // Remove the arphost from ARPPendingList as it has been learned now
            logger.debug("Host Removed from ARPPending List, IP: {}", id);
            HostTrackerCallable htCallable = arphost.getHostTrackerCallable();
            if (htCallable != null) {
                htCallable.wakeup();
            }
            return;
        }

        /*
         * It could have been a host from the FailedARPReqList
         */

        if (failedARPReqList.containsKey(id)) {
            failedARPReqList.remove(id);
            logger.debug("Host Removed from FailedARPReqList List, IP: {}", decodeIPFromId(id));
        }
    }

    // Learn a new Host
    private void learnNewHost(HostNodeConnector host) {
        IHostId id = HostIdFactory.create(host.getNetworkAddress(), host.getDataLayerAddress());
        host.initArpSendCountDown();
        HostNodeConnector rHost = hostsDB.putIfAbsent(id, host);
        if (rHost != null) {
            // Another host is already learned for this IP address, replace it
            replaceHost(id, rHost, host);
        } else {
            logger.debug("New Host Learned: MAC: {}  IP: {}", HexEncode.bytesToHexString(host
                    .getDataLayerAddressBytes()), host.getNetworkAddress().getHostAddress());
        }
    }

    private void replaceHost(IHostId id, HostNodeConnector removedHost, HostNodeConnector newHost) {
        // Ignore ARP messages from internal nodes
        NodeConnector newHostNc = newHost.getnodeConnector();
        boolean newHostIsInternal = topologyManager.isInternal(newHostNc);
        if (newHostIsInternal) {
            return;
        }

        newHost.initArpSendCountDown();

        if (hostsDB.replace(id, removedHost, newHost)) {
            logger.debug("Host move occurred: Old Host IP:{}, New Host IP: {}", removedHost.getNetworkAddress()
                    .getHostAddress(), newHost.getNetworkAddress().getHostAddress());
            logger.debug("Old Host MAC: {}, New Host MAC: {}",
                    HexEncode.bytesToHexString(removedHost.getDataLayerAddressBytes()),
                    HexEncode.bytesToHexString(newHost.getDataLayerAddressBytes()));
            // Display the Old and New HostNodeConnectors also
            logger.debug("Old {}, New {}", removedHost, newHost);
        } else {
            /*
             * Host replacement has failed, do the recovery
             */
            hostsDB.put(id, newHost);
            logger.error("Host replacement failed. Overwrite the host. Repalced Host: {}, New Host: {}", removedHost,
                    newHost);
        }
        notifyHostLearnedOrRemoved(removedHost, false);
        notifyHostLearnedOrRemoved(newHost, true);
        if (!newHost.isStaticHost()) {
            processPendingARPReqs(id);
        }
    }

    // Remove known Host
    private void removeKnownHost(IHostId key) {
        HostNodeConnector host = hostsDB.get(key);
        if (host != null) {
            logger.debug("Removing Host: IP:{}", host.getNetworkAddress().getHostAddress());
            hostsDB.remove(key);
        } else {
            logger.error("removeKnownHost(): Host for IP address {} not found in hostsDB", decodeIPFromId(key));
        }
    }

    private class NotifyHostThread extends Thread {

        private final HostNodeConnector host;

        public NotifyHostThread(HostNodeConnector h) {
            this.host = h;
        }

        @Override
        public void run() {
            HostNodeConnector removedHost = null;
            InetAddress networkAddr = host.getNetworkAddress();
            IHostId id = HostIdFactory.create(networkAddr, host.getDataLayerAddress());
            /* Check for Host Move case */
            if (hostMoved(host)) {
                /*
                 * Host has been moved from one location (switch,port, MAC, or
                 * VLAN) to another. Replace the existing host and its previous
                 * location parameters with new information, and notify the
                 * applications listening to host move.
                 */

                removedHost = hostsDB.get(id);
                if (removedHost != null) {
                    replaceHost(id, removedHost, host);
                    return;
                } else {
                    logger.error("Host to be removed not found in hostsDB");
                }
            }

            // It is a new host
            learnNewHost(host);

            /* check if there is an outstanding request for this host */
            processPendingARPReqs(id);
            notifyHostLearnedOrRemoved(host, true);
        }
    }

    @Override
    public void hostListener(HostNodeConnector host) {
        logger.debug("Received for Host: IP {}, MAC {}, {}", host.getNetworkAddress().getHostAddress(),
                HexEncode.bytesToHexString(host.getDataLayerAddressBytes()), host);
        if (hostExists(host)) {
            IHostId id = HostIdFactory.create(host.getNetworkAddress(), host.getDataLayerAddress());
            HostNodeConnector existinghost = hostsDB.get(id);
            existinghost.initArpSendCountDown();
            // Update the host

            hostsDB.put(id, existinghost);
            logger.debug("hostListener returned without adding the host");
            return;
        }
        new NotifyHostThread(host).start();
    }

    // Notify whoever is interested that a new host was learned (dynamically or
    // statically)
    private void notifyHostLearnedOrRemoved(HostNodeConnector host, boolean add) {
        // Update listeners if any
        if (newHostNotify != null) {
            logger.debug("Notifying Applications for Host {} Being {}", host.getNetworkAddress().getHostAddress(),
                    add ? "Added" : "Deleted");
            synchronized (this.newHostNotify) {
                for (IfNewHostNotify ta : newHostNotify) {
                    try {
                        if (add) {
                            ta.notifyHTClient(host);
                        } else {
                            ta.notifyHTClientHostRemoved(host);
                        }
                    } catch (Exception e) {
                        logger.error("Exception on new host notification", e);
                    }
                }
            }
        } else {
            logger.error("notifyHostLearnedOrRemoved(): New host notify is null");
        }

        // Topology update is for some reason outside of listeners registry
        // logic
        Node node = host.getnodeconnectorNode();
        Host h = null;
        NodeConnector p = host.getnodeConnector();
        try {
            DataLinkAddress dla = new EthernetAddress(host.getDataLayerAddressBytes());
            h = new Host(dla, host.getNetworkAddress());
        } catch (ConstructionException ce) {
            p = null;
            h = null;
        }

        if (topologyManager != null && p != null && h != null) {
            logger.debug("Notifying Topology Manager for Host {} Being {}", h.getNetworkAddress().getHostAddress(),
                    add ? "Added" : "Deleted");
            if (add == true) {
                Tier tier = new Tier(1);
                switchManager.setNodeProp(node, tier);
                topologyManager.updateHostLink(p, h, UpdateType.ADDED, null);
            } else {
                // No need to reset the tiering if no other hosts are currently
                // connected
                // If this switch was discovered to be an access switch, it
                // still is even if the host is down
                Tier tier = new Tier(0);
                switchManager.setNodeProp(node, tier);
                topologyManager.updateHostLink(p, h, UpdateType.REMOVED, null);
            }
        }
    }

    /**
     * When a new Host is learnt by the hosttracker module, it places the
     * directly connected Node in Tier-1 & using this function, updates the Tier
     * value for all other Nodes in the network hierarchy.
     *
     * This is a recursive function and it takes care of updating the Tier value
     * for all the connected and eligible Nodes.
     *
     * @param n
     *            Node that represents one of the Vertex in the Topology Graph.
     * @param currentTier
     *            The Tier on which n belongs
     */
    @SuppressWarnings("unused")
    private void updateSwitchTiers(Node n, int currentTier) {
        Map<Node, Set<Edge>> ndlinks = topologyManager.getNodeEdges();
        if (ndlinks == null) {
            logger.debug("updateSwitchTiers(): ndlinks null for Node: {}, Tier:{}", n, currentTier);
            return;
        }
        Set<Edge> links = ndlinks.get(n);
        if (links == null) {
            logger.debug("updateSwitchTiers(): links null for ndlinks:{}", ndlinks);
            return;
        }
        ArrayList<Node> needsVisiting = new ArrayList<Node>();
        for (Edge lt : links) {
            if (!lt.getHeadNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                // We don't want to work on Node that are not openflow
                // for now
                continue;
            }
            Node dstNode = lt.getHeadNodeConnector().getNode();
            if (switchNeedsTieringUpdate(dstNode, currentTier + 1)) {
                Tier t = new Tier(currentTier + 1);
                switchManager.setNodeProp(dstNode, t);
                needsVisiting.add(dstNode);
            }
        }

        /*
         * Due to the nature of the problem, having a separate loop for nodes
         * that needs visiting provides a decent walk optimization.
         */
        for (Node node : needsVisiting) {
            updateSwitchTiers(node, currentTier + 1);
        }
    }

    /**
     * Internal convenience routine to check the eligibility of a Switch for a
     * Tier update. Any Node with Tier=0 or a Tier value that is greater than
     * the new Tier Value is eligible for the update.
     *
     * @param n
     *            Node for which the Tier update eligibility is checked
     * @param tier
     *            new Tier Value
     * @return <code>true</code> if the Node is eligible for Tier Update
     *         <code>false</code> otherwise
     */

    private boolean switchNeedsTieringUpdate(Node n, int tier) {
        if (n == null) {
            logger.error("switchNeedsTieringUpdate(): Null node for tier: {}", tier);
            return false;
        }
        /*
         * Node could have gone down
         */
        if (!switchManager.getNodes().contains(n)) {
            return false;
        }
        // This is the case where Tier was never set for this node
        Tier t = (Tier) switchManager.getNodeProp(n, Tier.TierPropName);
        if (t == null) {
            return true;
        }
        if (t.getValue() == 0) {
            return true;
        } else if (t.getValue() > tier) {
            return true;
        }
        return false;
    }

    /**
     * Internal convenience routine to clear all the Tier values to 0. This
     * cleanup is performed during cases such as Topology Change where the
     * existing Tier values might become incorrect
     */
    @SuppressWarnings("unused")
    private void clearTiers() {
        Set<Node> nodes = null;
        if (switchManager == null) {
            logger.error("clearTiers(): Null switchManager");
            return;
        }
        nodes = switchManager.getNodes();

        for (Node n : nodes) {
            Tier t = new Tier(0);
            switchManager.setNodeProp(n, t);
        }
    }

    /**
     * Internal convenience routine to print the hierarchies of switches.
     */
    @SuppressWarnings("unused")
    private void logHierarchies(ArrayList<ArrayList<String>> hierarchies) {
        String hierarchyString = null;
        int num = 1;
        for (ArrayList<String> hierarchy : hierarchies) {
            StringBuffer buf = new StringBuffer();
            buf.append("Hierarchy#").append(num).append(" : ");
            for (String switchName : hierarchy) {
                buf.append(switchName).append("/");
            }
            logger.debug("{} -> {}", getContainerName(), buf);
            num++;
        }
    }

    /**
     * getHostNetworkHierarchy is the Back-end routine for the North-Bound API
     * that returns the Network Hierarchy for a given Host. This API is
     * typically used by applications like Hadoop for Rack Awareness
     * functionality.
     *
     * @param hostAddress
     *            IP-Address of the host/node.
     * @return Network Hierarchies represented by an Array of Array (of
     *         Switch-Ids as String).
     */
    @Override
    public List<List<String>> getHostNetworkHierarchy(IHostId id) {
        HostNodeConnector host = hostQuery(id);
        if (host == null) {
            return null;
        }

        List<List<String>> hierarchies = new ArrayList<List<String>>();
        ArrayList<String> currHierarchy = new ArrayList<String>();
        hierarchies.add(currHierarchy);

        Node node = host.getnodeconnectorNode();
        updateCurrentHierarchy(node, currHierarchy, hierarchies);
        return hierarchies;
    }

    /**
     * dpidToHostNameHack is a hack function for Cisco Live Hadoop Demo. Mininet
     * is used as the network for Hadoop Demos & in order to give a meaningful
     * rack-awareness switch names, the DPID is organized in ASCII Characters
     * and retrieved as string.
     *
     * @param dpid
     *            Switch DataPath Id
     * @return Ascii String represented by the DPID.
     */
    private String dpidToHostNameHack(long dpid) {
        String hex = Long.toHexString(dpid);

        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < hex.length(); i++) {
            result = (int) ((dpid >> (i * 8)) & 0xff);
            if (result == 0) {
                continue;
            }
            if (result < 0x30) {
                result += 0x40;
            }
            sb.append(String.format("%c", result));
        }
        return sb.reverse().toString();
    }

    /**
     * A convenient recursive routine to obtain the Hierarchy of Switches.
     *
     * @param node
     *            Current Node in the Recursive routine.
     * @param currHierarchy
     *            Array of Nodes that make this hierarchy on which the Current
     *            Switch belong
     * @param fullHierarchy
     *            Array of multiple Hierarchies that represent a given host.
     */
    @SuppressWarnings("unchecked")
    private void updateCurrentHierarchy(Node node, ArrayList<String> currHierarchy, List<List<String>> fullHierarchy) {
        currHierarchy.add(dpidToHostNameHack((Long) node.getID()));
        // Shallow copy as required
        ArrayList<String> currHierarchyClone = (ArrayList<String>) currHierarchy.clone();

        Map<Node, Set<Edge>> ndlinks = topologyManager.getNodeEdges();
        if (ndlinks == null) {
            logger.debug("updateCurrentHierarchy(): topologyManager returned null ndlinks for node: {}", node);
            return;
        }
        Node n = NodeCreator.createOFNode((Long) node.getID());
        Set<Edge> links = ndlinks.get(n);
        if (links == null) {
            logger.debug("updateCurrentHierarchy(): Null links for ndlinks");
            return;
        }
        for (Edge lt : links) {
            if (!lt.getHeadNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                // We don't want to work on Node that are not openflow
                // for now
                continue;
            }
            Node dstNode = lt.getHeadNodeConnector().getNode();

            Tier nodeTier = (Tier) switchManager.getNodeProp(node, Tier.TierPropName);
            /*
             * If the host is directly attached to the src node, then the node
             * should have been assigned the "Access" tier in
             * notifyHostLearnedOrRemoved. If not, it would be assigned
             * "Unknown" tier. Thus the tier of host attached node cannot be
             * null. If the src node here, is the next node in the hierarchy of
             * the nodes, then its tier cannot be null
             */

            Tier dstNodeTier = (Tier) switchManager.getNodeProp(dstNode, Tier.TierPropName);
            /*
             * Skip if the tier of the destination node is null
             */
            if (dstNodeTier == null) {
                continue;
            }
            if (dstNodeTier.getValue() > nodeTier.getValue()) {
                ArrayList<String> buildHierarchy = currHierarchy;
                if (currHierarchy.size() > currHierarchyClone.size()) {
                    // Shallow copy as required
                    buildHierarchy = (ArrayList<String>) currHierarchyClone.clone();
                    fullHierarchy.add(buildHierarchy);
                }
                updateCurrentHierarchy(dstNode, buildHierarchy, fullHierarchy);
            }
        }
    }

    private void debugEdgeUpdate(Edge e, UpdateType type, Set<Property> props) {
        Long srcNid = null;
        Short srcPort = null;
        Long dstNid = null;
        Short dstPort = null;
        boolean added = false;
        String srcType = null;
        String dstType = null;

        if (e == null || type == null) {
            logger.error("Edge or Update type are null!");
            return;
        } else {
            srcType = e.getTailNodeConnector().getType();
            dstType = e.getHeadNodeConnector().getType();

            if (srcType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                logger.debug("Skip updates for {}", e);
                return;
            }

            if (!srcType.equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                logger.debug("For now we cannot handle updates for non-openflow nodes");
                return;
            }

            if (dstType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                logger.debug("Skip updates for {}", e);
                return;
            }

            if (!dstType.equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                logger.debug("For now we cannot handle updates for non-openflow nodes");
                return;
            }

            // At this point we know we got an openflow update, so
            // lets fill everything accordingly.
            srcNid = (Long) e.getTailNodeConnector().getNode().getID();
            srcPort = (Short) e.getTailNodeConnector().getID();
            dstNid = (Long) e.getHeadNodeConnector().getNode().getID();
            dstPort = (Short) e.getHeadNodeConnector().getID();

            // Now lets update the added flag
            switch (type) {
            case ADDED:
            case CHANGED:
                added = true;
                break;
            case REMOVED:
                added = false;
            }
        }

        logger.debug("HostTracker Topology linkUpdate handling src:{}[port {}] dst:{}[port {}] added: {}",
                new Object[] { srcNid, srcPort, dstNid, dstPort, added });
    }

    @Override
    public void edgeUpdate(List<TopoEdgeUpdate> topoedgeupdateList) {
        if (logger.isDebugEnabled()) {
            for (TopoEdgeUpdate topoEdgeUpdate : topoedgeupdateList) {
                Edge e = topoEdgeUpdate.getEdge();
                Set<Property> p = topoEdgeUpdate.getProperty();
                UpdateType type = topoEdgeUpdate.getUpdateType();

                debugEdgeUpdate(e, type, p);
            }
        }
    }

    @Override
    public void subnetNotify(Subnet sub, boolean add) {
        logger.debug("Received subnet notification: {}  add={}", sub, add);
        if (add) {
            for (Entry<IHostId, ARPPending> entry : failedARPReqList.entrySet()) {
                ARPPending arphost;
                arphost = entry.getValue();
                if (hostFinder == null) {
                    logger.warn("ARPHandler Services are not available on subnet addition");
                    continue;
                }
                logger.debug("Sending the ARP from FailedARPReqList fors IP: {}", decodeIPFromId(arphost.getHostId()));
                for (IHostFinder hf : hostFinder) {
                    hf.find(decodeIPFromId(arphost.getHostId()));
                }
            }
        }
    }

    class OutStandingARPHandler extends TimerTask {
        @Override
        public void run() {
            if (stopping) {
                return;
            }
            ARPPending arphost;
            /* This routine runs every 4 seconds */
            logger.trace("Number of Entries in ARP Pending/Failed Lists: ARPPendingList = {}, failedARPReqList = {}",
                    ARPPendingList.size(), failedARPReqList.size());
            for (Entry<IHostId, ARPPending> entry : ARPPendingList.entrySet()) {
                arphost = entry.getValue();

                if (hostsDB.containsKey(arphost.getHostId())) {
                    // this host is already learned, shouldn't be in
                    // ARPPendingList
                    // Remove it and continue
                    logger.warn("Learned Host {} found in ARPPendingList", decodeIPFromId(arphost.getHostId()));
                    ARPPendingList.remove(entry.getKey());
                    continue;
                }
                if (arphost.getSent_count() < hostRetryCount) {
                    /*
                     * No reply has been received of first ARP Req, send the
                     * next one. Before sending the ARP, check if ARPHandler is
                     * available or not
                     */
                    if (hostFinder == null) {
                        logger.warn("ARPHandler Services are not available for Outstanding ARPs");
                        continue;
                    }
                    for (IHostFinder hf : hostFinder) {
                        hf.find(decodeIPFromId(arphost.getHostId()));
                    }
                    arphost.sent_count++;
                    logger.debug("ARP Sent from ARPPending List, IP: {}", decodeIPFromId(arphost.getHostId()));
                } else if (arphost.getSent_count() >= hostRetryCount) {
                    /*
                     * ARP requests have been sent without receiving a reply,
                     * remove this from the pending list
                     */
                    ARPPendingList.remove(entry.getKey());
                    logger.debug("ARP reply not received after multiple attempts, removing from Pending List IP: {}",
                            decodeIPFromId(arphost.getHostId()));
                    /*
                     * Add this host to a different list which will be processed
                     * on link up events
                     */
                    logger.debug("Adding the host to FailedARPReqList IP: {}", decodeIPFromId(arphost.getHostId()));
                    failedARPReqList.put(entry.getKey(), arphost);

                } else {
                    logger.error("Inavlid arp_sent count for entry: {}", entry);
                }
            }
        }
    }

    private class ARPRefreshHandler extends TimerTask {
        @Override
        public void run() {
            if (stopping) {
                return;
            }
            if ((clusterContainerService != null) && !clusterContainerService.amICoordinator()) {
                return;
            }
            if (!hostRefresh) {
                /*
                 * The host probe procedure is turned off
                 */
                return;
            }
            if (hostsDB == null) {
                /* hostsDB is not allocated yet */
                logger.error("ARPRefreshHandler(): hostsDB is not allocated yet:");
                return;
            }
            for (Entry<IHostId, HostNodeConnector> entry : hostsDB.entrySet()) {
                HostNodeConnector host = entry.getValue();
                if (host.isStaticHost()) {
                    /* this host was learned via API3, don't age it out */
                    continue;
                }

                short arp_cntdown = host.getArpSendCountDown();
                arp_cntdown--;
                if (arp_cntdown > hostRetryCount) {
                    host.setArpSendCountDown(arp_cntdown);
                } else if (arp_cntdown <= 0) {
                    /*
                     * No ARP Reply received in last 2 minutes, remove this host
                     * and inform applications
                     */
                    removeKnownHost(entry.getKey());
                    notifyHostLearnedOrRemoved(host, false);
                } else if (arp_cntdown <= hostRetryCount) {
                    /*
                     * Use the services of arphandler to check if host is still
                     * there
                     */
                    if (logger.isTraceEnabled()) {
                        logger.trace(
                                "ARP Probing ({}) for {}({})",
                                new Object[] { arp_cntdown, host.getNetworkAddress().getHostAddress(),
                                        HexEncode.bytesToHexString(host.getDataLayerAddressBytes()) });
                    }
                    host.setArpSendCountDown(arp_cntdown);
                    if (hostFinder == null) {
                        /*
                         * If hostfinder is not available, then can't send the
                         * probe. However, continue the age out the hosts since
                         * we don't know if the host is indeed out there or not.
                         */
                        logger.trace("ARPHandler is not avaialable, can't send the probe");
                        continue;
                    }
                    for (IHostFinder hf : hostFinder) {
                        hf.probe(host);
                    }
                }
            }
        }
    }

    /**
     * Inform the controller IP to MAC binding of a host and its connectivity to
     * an openflow switch in terms of Node, port, and VLAN.
     *
     * @param networkAddr
     *            IP address of the host
     * @param dataLayer
     *            Address MAC address of the host
     * @param nc
     *            NodeConnector to which host is connected
     * @param port
     *            Port of the switch to which host is connected
     * @param vlan
     *            Vlan of which this host is member of
     *
     * @return Status The status object as described in {@code Status}
     *         indicating the result of this action.
     */

    protected Status addStaticHostReq(InetAddress networkAddr, byte[] dataLayerAddress, NodeConnector nc, short vlan) {
        if (dataLayerAddress.length != NetUtils.MACAddrLengthInBytes) {
            return new Status(StatusCode.BADREQUEST, "Invalid MAC address");
        }

        if (nc == null) {
            return new Status(StatusCode.BADREQUEST, "Invalid NodeConnector");
        }
        HostNodeConnector host = null;
        try {
            host = new HostNodeConnector(dataLayerAddress, networkAddr, nc, vlan);
            IHostId id = HostIdFactory.create(networkAddr, new EthernetAddress(dataLayerAddress));
            if (hostExists(host)) {
                // This host is already learned either via ARP or through a
                // northbound request
                HostNodeConnector transHost = hostsDB.get(networkAddr);
                transHost.setStaticHost(true);
                return new Status(StatusCode.SUCCESS);
            }

            if (hostsDB.get(id) != null) {
                // There is already a host with this IP address (but behind
                // a different (switch, port, vlan) tuple. Return an error
                return new Status(StatusCode.CONFLICT, "Host with this IP already exists.");
            }
            host.setStaticHost(true);
            /*
             * Check if the nc is an ISL port
             */
            if (topologyManager != null) {
                if (topologyManager.isInternal(nc)) {
                    return new Status(StatusCode.BADREQUEST, "Cannot add host on ISL port");
                }
            }
            /*
             * Before adding host, Check if the switch and the port have already
             * come up
             */
            if (switchManager.isNodeConnectorEnabled(nc)) {
                learnNewHost(host);
                processPendingARPReqs(id);
                notifyHostLearnedOrRemoved(host, true);
            } else {
                inactiveStaticHosts.put(nc, host);
                logger.debug("Switch or switchport is not up, adding host {} to inactive list",
                        networkAddr.getHostName());
            }
            return new Status(StatusCode.SUCCESS);
        } catch (ConstructionException e) {
            logger.error("", e);
            return new Status(StatusCode.INTERNALERROR, "Host could not be created");
        }

    }

    /**
     * Update the controller IP to MAC binding of a host and its connectivity to
     * an openflow switch in terms of switch id, switch port, and VLAN.
     *
     * @param networkAddr
     *            IP address of the host
     * @param dataLayer
     *            Address MAC address of the host
     * @param nc
     *            NodeConnector to which host is connected
     * @param port
     *            Port of the switch to which host is connected
     * @param vlan
     *            Vlan of which this host is member of
     *
     * @return Status The status object as described in {@code Status}
     *         indicating the result of this action.
     */
    public Status updateHostReq(InetAddress networkAddr, byte[] dataLayerAddress, NodeConnector nc, short vlan) {
        HostNodeConnector tobeUpdatedHost;
        HostNodeConnector host = null;

        if (dataLayerAddress.length != NetUtils.MACAddrLengthInBytes) {
            return new Status(StatusCode.BADREQUEST, "Invalid MAC address");
        }

        if (nc == null) {
            return new Status(StatusCode.BADREQUEST, "Invalid NodeConnector");
        }

        try {
            host = new HostNodeConnector(dataLayerAddress, networkAddr, nc, vlan);
            if (hostExists(host)) {
                return new Status(StatusCode.BADREQUEST, "Host already exists");
            }

            IHostId id = HostIdFactory.create(networkAddr, new EthernetAddress(dataLayerAddress));

            if ((tobeUpdatedHost = hostsDB.get(networkAddr)) != null) {
                if (hostsDB.replace(id, tobeUpdatedHost, host)) {
                    logger.debug("Host replaced from hostsDB. Old host: {} New Host: {}", tobeUpdatedHost, host);
                    notifyHostLearnedOrRemoved(tobeUpdatedHost, false);
                    notifyHostLearnedOrRemoved(host, true);
                    return new Status(StatusCode.SUCCESS);
                } else {
                    logger.error("Static host replacement failed from hostsDB, Replaced Host: {}, New Host: {}",
                            tobeUpdatedHost, host);
                    return new Status(StatusCode.INTERNALERROR,
                            "Host Replacement Failed due to presence of another host with same IP");
                }
            }

            // Check if the host exists in inactive hosts database
            if ((tobeUpdatedHost = inactiveStaticHosts.get(nc)) != null) {
                if (inactiveStaticHosts.replace(nc, tobeUpdatedHost, host)) {
                    logger.debug("Host replaced from inactive hostsDB. Old host: {} New Host: {}", tobeUpdatedHost,
                            host);
                    return new Status(StatusCode.SUCCESS);
                } else {
                    logger.error("Static host replacement failed, Replaced Host: {}, New Host: {}", tobeUpdatedHost,
                            host);
                    return new Status(StatusCode.INTERNALERROR,
                            "Host Replacement Failed due to presence of another host with same IP");
                }
            }

            // Host doesn't exist
            return new Status(StatusCode.BADREQUEST, "Host doesn't exists, can't update");
        } catch (ConstructionException e) {
            logger.error("", e);
            return new Status(StatusCode.INTERNALERROR, "host object creation failure");
        }
    }

    /**
     * Remove from the controller IP to MAC binding of a host and its
     * connectivity to an openflow switch
     *
     * @param networkAddr
     *            IP address of the host
     *
     * @return boolean true if the host was removed successfully, false
     *         otherwise
     */

    public Status removeStaticHostReq(InetAddress networkAddress, DataLinkAddress mac) {
        // Check if host is in active hosts database
        IHostId id = HostIdFactory.create(networkAddress, mac);
        HostNodeConnector host = getHostFromOnActiveDB(id);
        if (host != null) {
            // Validation check
            if (!host.isStaticHost()) {
                return new Status(StatusCode.FORBIDDEN, "Host " + networkAddress.getHostName() + " is not static");
            }
            // Remove and notify
            notifyHostLearnedOrRemoved(host, false);
            removeKnownHost(id);
            return new Status(StatusCode.SUCCESS, null);
        }

        // Check if host is in inactive hosts database
        Entry<NodeConnector, HostNodeConnector> entry = getHostFromInactiveDB(id);
        if (entry != null) {
            host = entry.getValue();
            // Validation check
            if (!host.isStaticHost()) {
                return new Status(StatusCode.FORBIDDEN, "Host " + networkAddress.getHostName() + " is not static");
            }
            this.removeHostFromInactiveDB(id);
            return new Status(StatusCode.SUCCESS, null);
        }

        // Host is neither in active nor inactive hosts database
        return new Status(StatusCode.NOTFOUND, "Host does not exist");
    }

    @Override
    public void modeChangeNotify(Node node, boolean proactive) {
        logger.debug("Set Switch {} Mode to {}", node.getID(), proactive);
    }

    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        if (node == null) {
            return;
        }

        switch (type) {
        case REMOVED:
            logger.debug("Received removed node {}", node);
            for (Entry<IHostId, HostNodeConnector> entry : hostsDB.entrySet()) {
                HostNodeConnector host = entry.getValue();
                if (host.getnodeconnectorNode().equals(node)) {
                    logger.debug("Node: {} is down, remove from Hosts_DB", node);
                    removeKnownHost(entry.getKey());
                    notifyHostLearnedOrRemoved(host, false);
                }
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null) {
            return;
        }

        boolean up = false;
        switch (type) {
        case ADDED:
            up = true;
            break;
        case REMOVED:
            break;
        case CHANGED:
            State state = (State) propMap.get(State.StatePropName);
            if ((state != null) && (state.getValue() == State.EDGE_UP)) {
                up = true;
            }
            break;
        default:
            return;
        }

        if (up) {
            handleNodeConnectorStatusUp(nodeConnector);
        } else {
            handleNodeConnectorStatusDown(nodeConnector);
        }
    }

    @Override
    public Status addStaticHost(String networkAddress, String dataLayerAddress, NodeConnector nc, String vlan) {
        try {
            InetAddress ip = InetAddress.getByName(networkAddress);
            short vl = 0;
            if (vlan != null && !vlan.isEmpty()) {
                vl = Short.decode(vlan);
                if (vl < 1 || vl > 4095) {
                    return new Status(StatusCode.BADREQUEST, "Host vlan out of range [1 - 4095]");
                }
            }

            return addStaticHostReq(ip, HexEncode.bytesFromHexString(dataLayerAddress), nc, vl);

        } catch (UnknownHostException e) {
            logger.debug("Invalid host IP specified when adding static host", e);
            return new Status(StatusCode.BADREQUEST, "Invalid Host IP Address");
        } catch (NumberFormatException nfe) {
            logger.debug("Invalid host vlan or MAC specified when adding static host", nfe);
            return new Status(StatusCode.BADREQUEST, "Invalid Host vLan/MAC");
        }
    }

    @Override
    public Status removeStaticHost(String networkAddress) {
        try {
            if ((keyScheme != null) && (!keyScheme.equals(HostIdFactory.DEFAULT_IP_KEY_SCHEME))) {
                return new Status(StatusCode.NOTALLOWED, "Host DB Key scheme used is not IP only scheme.");
            }
            InetAddress address = InetAddress.getByName(networkAddress);
            return removeStaticHostReq(address, null);
        } catch (UnknownHostException e) {
            logger.debug("Invalid IP Address when trying to remove host", e);
            return new Status(StatusCode.BADREQUEST, "Invalid IP Address when trying to remove host");
        }
    }

    @Override
    public Status removeStaticHostUsingIPAndMac(String networkAddress, String macAddress) {
        try {
            if ((keyScheme != null) && (keyScheme.equals(HostIdFactory.DEFAULT_IP_KEY_SCHEME))) {
                return new Status(StatusCode.NOTALLOWED, "Host DB Key scheme used is not IP only scheme.");
            }
            InetAddress address = InetAddress.getByName(networkAddress);
            DataLinkAddress mac = new EthernetAddress(HexEncode.bytesFromHexString(macAddress));
            return removeStaticHostReq(address, mac);
        } catch (UnknownHostException e) {
            logger.debug("Invalid IP Address when trying to remove host", e);
            return new Status(StatusCode.BADREQUEST, "Invalid IP Address when trying to remove host");
        } catch (ConstructionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new Status(StatusCode.BADREQUEST, "Invalid Input parameters have been passed.");
        }
    }

    private InetAddress decodeIPFromId(IHostId id) {
        if ((keyScheme != null) && (keyScheme.equals(HostIdFactory.DEFAULT_IP_KEY_SCHEME))) {
            IPHostId ipId = (IPHostId) id;
            return (ipId.getIpAddress());
        } else if ((keyScheme != null) && (keyScheme.equals(HostIdFactory.IP_MAC_KEY_SCHEME))) {
            IPMacHostId ipMacId = (IPMacHostId) id;
            return (ipMacId.getIpAddress());
        }
        return null;
    }

    private DataLinkAddress decodeMacFromId(IHostId id) {
        if ((keyScheme != null) && (!keyScheme.equals(HostIdFactory.DEFAULT_IP_KEY_SCHEME))) {
            IPMacHostId ipMacId = (IPMacHostId) id;
            return (ipMacId.getMacAddr());
        }

        return null;
    }

    private void handleNodeConnectorStatusUp(NodeConnector nodeConnector) {
        ARPPending arphost;
        HostNodeConnector host = null;

        logger.trace("handleNodeConnectorStatusUp {}", nodeConnector);

        for (Entry<IHostId, ARPPending> entry : failedARPReqList.entrySet()) {
            arphost = entry.getValue();
            logger.trace("Sending the ARP from FailedARPReqList fors IP: {}", arphost.getHostId());
            if (hostFinder == null) {
                logger.warn("ARPHandler is not available at interface  up");
                logger.warn("Since this event is missed, host(s) connected to interface {} may not be discovered",
                        nodeConnector);
                continue;
            }

            // Send a broadcast ARP only on the interface which just came up.
            // Use hostFinder's "probe" method
            try {
                byte[] dataLayerAddress = NetUtils.getBroadcastMACAddr();
                host = new HostNodeConnector(dataLayerAddress, decodeIPFromId(arphost.getHostId()), nodeConnector,
                        (short) 0);
                for (IHostFinder hf : hostFinder) {
                    hf.probe(host);
                }
            } catch (ConstructionException e) {
                logger.debug("HostNodeConnector couldn't be created for Host: {}, NodeConnector: {}",
                        arphost.getHostId(), nodeConnector);
                logger.error("", e);
            }
        }

        host = inactiveStaticHosts.get(nodeConnector);
        if (host != null) {
            inactiveStaticHosts.remove(nodeConnector);
            learnNewHost(host);
            IHostId id = HostIdFactory.create(host.getNetworkAddress(), host.getDataLayerAddress());
            processPendingARPReqs(id);
            notifyHostLearnedOrRemoved(host, true);
        }
    }

    private void handleNodeConnectorStatusDown(NodeConnector nodeConnector) {
        logger.trace("handleNodeConnectorStatusDown {}", nodeConnector);

        for (Entry<IHostId, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            if (host.getnodeConnector().equals(nodeConnector)) {
                logger.debug(" NodeConnector: {} is down, remove from Hosts_DB", nodeConnector);
                removeKnownHost(entry.getKey());
                notifyHostLearnedOrRemoved(host, false);
            }
        }
    }

    void setClusterContainerService(IClusterContainerServices s) {
        logger.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            logger.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    public String getContainerName() {
        if (containerName == null) {
            return GlobalConstants.DEFAULT.toString();
        }
        return containerName;
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            this.containerName = (String) props.get("containerName");
            logger.debug("Running containerName: {}", this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        startUp();

        logger.debug("key Scheme in hosttracker is {}", keyScheme);
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    void stopping() {
        stopping = true;
        arpRefreshTimer.cancel();
        timer.cancel();
        executor.shutdownNow();
    }

    @Override
    public void edgeOverUtilized(Edge edge) {

    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {

    }

    @Override
    public void entryCreated(IHostId key, String cacheName, boolean originLocal) {
        if (originLocal) {
            return;
        }
        processPendingARPReqs(key);
    }

    @Override
    public void entryUpdated(IHostId key, HostNodeConnector new_value, String cacheName, boolean originLocal) {
    }

    @Override
    public void entryDeleted(IHostId key, String cacheName, boolean originLocal) {
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
    }

    @Override
    public String getHelp() {
        return null;
    }

    public void _dumpPendingARPReqList(CommandInterpreter ci) {
        ARPPending arphost;
        for (Entry<IHostId, ARPPending> entry : ARPPendingList.entrySet()) {
            arphost = entry.getValue();
            ci.println(arphost.getHostId().toString());
        }
    }

    public void _dumpFailedARPReqList(CommandInterpreter ci) {
        ARPPending arphost;
        for (Entry<IHostId, ARPPending> entry : failedARPReqList.entrySet()) {
            arphost = entry.getValue();
            ci.println(arphost.getHostId().toString());
        }
    }

    @Override
    public HostNodeConnector hostFind(InetAddress addr) {
        IHostId id = HostIdFactory.create(addr, null);
        return (hostFind(id));
    }

    @Override
    public HostNodeConnector hostQuery(InetAddress addr) {
        IHostId id = HostIdFactory.create(addr, null);
        return (hostQuery(id));
    }

    @Override
    public Future<HostNodeConnector> discoverHost(InetAddress addr) {
        IHostId id = HostIdFactory.create(addr, null);
        return discoverHost(id);
    }

    @Override
    public List<List<String>> getHostNetworkHierarchy(InetAddress addr) {
        IHostId id = HostIdFactory.create(addr, null);
        return getHostNetworkHierarchy(id);
    }
}
