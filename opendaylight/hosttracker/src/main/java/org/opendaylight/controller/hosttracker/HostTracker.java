
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
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
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file   HostTracker.java
 * This class tracks the location of IP Hosts as to which Switch, Port, VLAN, they are 
 * connected to, as well as their MAC address. This is done dynamically as well as statically.
 * The dynamic mechanism consists of listening to ARP messages as well sending ARP requests.
 * Static mechanism consists of Northbound APIs to add or remove the hosts from the local
 * database. ARP aging is also implemented to age out dynamically learned hosts. Interface
 * methods are provided for other applications to
 *  1. Query the local database for a single host
 *  2. Get a list of all hosts
 *  3. Get notification if a host is learned/added or removed the database
 */

public class HostTracker implements IfIptoHost, IfHostListener,
        ISwitchManagerAware, IInventoryListener, ITopologyManagerAware {
    private static final Logger logger = LoggerFactory
            .getLogger(HostTracker.class);
    private IHostFinder hostFinder;
    private ConcurrentMap<InetAddress, HostNodeConnector> hostsDB;
    /* Following is a list of hosts which have been requested by NB APIs to be added,
     * but either the switch or the port is not sup, so they will be added here until
     * both come up
     */
    private ConcurrentMap<NodeConnector, HostNodeConnector> inactiveStaticHosts;
    private Set<IfNewHostNotify> newHostNotify = Collections
            .synchronizedSet(new HashSet<IfNewHostNotify>());

    private ITopologyManager topologyManager;
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager = null;
    private Timer timer;
    private Timer arp_refresh_timer;
    private String containerName = null;

    private static class ARPPending {
        protected InetAddress hostIP;
        protected short sent_count;
        protected HostTrackerCallable hostTrackerCallable;

        public InetAddress getHostIP() {
            return hostIP;
        }

        public short getSent_count() {
            return sent_count;
        }

        public HostTrackerCallable getHostTrackerCallable() {
            return hostTrackerCallable;
        }

        public void setHostIP(InetAddress networkAddr) {
            this.hostIP = networkAddr;
        }

        public void setSent_count(short count) {
            this.sent_count = count;
        }

        public void setHostTrackerCallable(HostTrackerCallable callable) {
            hostTrackerCallable = callable;
        }
    }

    //This list contains the hosts for which ARP requests are being sent periodically
    private List<ARPPending> ARPPendingList = new ArrayList<HostTracker.ARPPending>();
    /*
     * This list below contains the hosts which were initially in ARPPendingList above,
     * but ARP response didn't come from there hosts after multiple attempts over 8
     * seconds. The assumption is that the response didn't come back due to one of the
     * following possibilities:
     *   1. The L3 interface wasn't created for this host in the controller. This would
     *      cause arphandler not to know where to send the ARP
     *   2. The host facing port is down
     *   3. The IP host doesn't exist or is not responding to ARP requests
     *
     * Conditions 1 and 2 above can be recovered if ARP is sent when the relevant L3
     * interface is added or the port facing host comes up. Whenever L3 interface is
     * added or host facing port comes up, ARP will be sent to hosts in this list.
     *
     * We can't recover from condition 3 above
     */
    private ArrayList<ARPPending> failedARPReqList = new ArrayList<HostTracker.ARPPending>();

    public HostTracker() {
    }

    private void startUp() {
        allocateCache();
        retrieveCache();

        timer = new Timer();
        timer.schedule(new OutStandingARPHandler(), 4000, 4000);

        /* ARP Refresh Timer to go off every 5 seconds to implement ARP aging */
        arp_refresh_timer = new Timer();
        arp_refresh_timer.schedule(new ARPRefreshHandler(), 5000, 5000);
        logger.debug("startUp: Caches created, timers started");
    }

    @SuppressWarnings("deprecation")
	private void allocateCache() {
        if (this.clusterContainerService == null) {
            logger
                    .error("un-initialized clusterContainerService, can't create cache");
            return;
        }
        logger.debug("Creating Cache for HostTracker");
        try {
            this.clusterContainerService.createCache("hostTrackerAH", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            this.clusterContainerService.createCache("hostTrackerIH", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger
                    .error("Cache couldn't be created for HostTracker -  check cache mode");
        } catch (CacheExistException cce) {
            logger
                    .error("Cache for HostTracker already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for HostTracker");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            logger
                    .error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }
        logger.debug("Retrieving cache for HostTrackerAH");
        hostsDB = (ConcurrentMap<InetAddress, HostNodeConnector>) this.clusterContainerService
                .getCache("hostTrackerAH");
        if (hostsDB == null) {
            logger.error("Cache couldn't be retrieved for HostTracker");
        }
        logger.debug("Cache was successfully retrieved for HostTracker");
        logger.debug("Retrieving cache for HostTrackerIH");
        inactiveStaticHosts = (ConcurrentMap<NodeConnector, HostNodeConnector>) this.clusterContainerService
                .getCache("hostTrackerIH");
        if (hostsDB == null) {
            logger.error("Cache couldn't be retrieved for HostTrackerIH");
        }
        logger.debug("Cache was successfully retrieved for HostTrackerIH");
    }

    public void nonClusterObjectCreate() {
        hostsDB = new ConcurrentHashMap<InetAddress, HostNodeConnector>();
        inactiveStaticHosts = new ConcurrentHashMap<NodeConnector, HostNodeConnector>();
    }

    @SuppressWarnings("deprecation")
	private void destroyCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        this.clusterContainerService.destroyCache("hostTrackerAH");
        this.clusterContainerService.destroyCache("hostTrackerIH");
        nonClusterObjectCreate();
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
        this.hostFinder = hostFinder;
    }

    public void unsetArpHandler(IHostFinder hostFinder) {
        if (this.hostFinder == hostFinder) {
            logger.debug("Arp Handler Service removed!");
            this.hostFinder = null;
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
        HostNodeConnector lhost = hostsDB.get(host.getNetworkAddress());
        return host.equals(lhost);
    }

    private HostNodeConnector getHostFromOnActiveDB(InetAddress networkAddress) {
        return hostsDB.get(networkAddress);
    }

    private Entry<NodeConnector, HostNodeConnector> getHostFromInactiveDB(
            InetAddress networkAddress) {
        for (Entry<NodeConnector, HostNodeConnector> entry : inactiveStaticHosts
                .entrySet()) {
            if (entry.getValue().equalsByIP(networkAddress)) {
                logger
                        .debug(
                                "getHostFromInactiveDB(): Inactive Host found for IP:{} ",
                                networkAddress.getHostAddress());
                return entry;
            }
        }
        logger.debug(
                "getHostFromInactiveDB() Inactive Host Not found for IP: {}",
                networkAddress.getHostAddress());
        return null;
    }

    private void removeHostFromInactiveDB(InetAddress networkAddress) {
        NodeConnector nodeConnector = null;
        for (Entry<NodeConnector, HostNodeConnector> entry : inactiveStaticHosts
                .entrySet()) {
            if (entry.getValue().equalsByIP(networkAddress)) {
                nodeConnector = entry.getKey();
                break;
            }
        }
        if (nodeConnector != null) {
            inactiveStaticHosts.remove(nodeConnector);
            logger.debug("removeHostFromInactiveDB(): Host Removed for IP: {}",
                    networkAddress.getHostAddress());
            return;
        }
        logger.debug("removeHostFromInactiveDB(): Host Not found for IP: {}",
                networkAddress.getHostAddress());
    }

    protected boolean hostMoved(HostNodeConnector host) {
        if (hostQuery(host.getNetworkAddress()) != null) {
            return true;
        }
        return false;
    }

    public HostNodeConnector hostQuery(InetAddress networkAddress) {
        return hostsDB.get(networkAddress);
    }

    public Future<HostNodeConnector> discoverHost(InetAddress networkAddress) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        if (executor == null) {
            logger.error("discoverHost: Null executor");
            return null;
        }
        Callable<HostNodeConnector> worker = new HostTrackerCallable(this,
                networkAddress);
        Future<HostNodeConnector> submit = executor.submit(worker);
        return submit;
    }

    public HostNodeConnector hostFind(InetAddress networkAddress) {
        /*
         * Sometimes at boot with containers configured in the startup
         * we hit this path (from TIF) when hostFinder has not been set yet
         * Caller already handles the null return
         */

        if (hostFinder == null) {
            logger.debug("Exiting hostFind, null hostFinder");
            return null;
        }

        HostNodeConnector host = hostQuery(networkAddress);
        if (host != null) {
            logger.debug("hostFind(): Host found for IP: {}", networkAddress
                    .getHostAddress());
            return host;
        }
        /* host is not found, initiate a discovery */
        hostFinder.find(networkAddress);
        /* Also add this host to ARPPending List for any potential retries */
        AddtoARPPendingList(networkAddress);
        logger
                .debug(
                        "hostFind(): Host Not Found for IP: {}, Inititated Host Discovery ...",
                        networkAddress.getHostAddress());
        return null;
    }

    public Set<HostNodeConnector> getAllHosts() {
        Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>();
        for (Entry<InetAddress, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            allHosts.add(host);
        }
        logger.debug("Exiting getAllHosts, Found {} Hosts", allHosts.size());
        return allHosts;
    }

    @Override
    public Set<HostNodeConnector> getActiveStaticHosts() {
        Set<HostNodeConnector> list = new HashSet<HostNodeConnector>();
        for (Entry<InetAddress, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            if (host.isStaticHost()) {
                list.add(host);
            }
        }
        logger.debug("getActiveStaticHosts(): Found {} Hosts", list.size());
        return list;
    }

    @Override
    public Set<HostNodeConnector> getInactiveStaticHosts() {
        Set<HostNodeConnector> list = new HashSet<HostNodeConnector>();
        for (Entry<NodeConnector, HostNodeConnector> entry : inactiveStaticHosts
                .entrySet()) {
            list.add(entry.getValue());
        }
        logger.debug("getInactiveStaticHosts(): Found {} Hosts", list.size());
        return list;
    }

    private void AddtoARPPendingList(InetAddress networkAddr) {
        ARPPending arphost = new ARPPending();

        arphost.setHostIP(networkAddr);
        arphost.setSent_count((short) 1);
        ARPPendingList.add(arphost);
        logger.debug("Host Added to ARPPending List, IP: {}", networkAddr
                .toString());
    }

    private void removePendingARPFromList(int index) {
        if (index >= ARPPendingList.size()) {
            logger
                    .warn(
                            "removePendingARPFromList(): index greater than the List. Size:{}, Index:{}",
                            ARPPendingList.size(), index);
            return;
        }
        ARPPending arphost = ARPPendingList.remove(index);
        HostTrackerCallable htCallable = arphost.getHostTrackerCallable();
        if (htCallable != null)
            htCallable.wakeup();
    }

    public void setCallableOnPendingARP(InetAddress networkAddr,
            HostTrackerCallable callable) {
        ARPPending arphost;
        for (int i = 0; i < ARPPendingList.size(); i++) {
            arphost = ARPPendingList.get(i);
            if (arphost.getHostIP().equals(networkAddr)) {
                arphost.setHostTrackerCallable(callable);
            }
        }
    }

    private void ProcPendingARPReqs(InetAddress networkAddr) {
        ARPPending arphost;

        for (int i = 0; i < ARPPendingList.size(); i++) {
            arphost = ARPPendingList.get(i);
            if (arphost.getHostIP().equals(networkAddr)) {
                /* An ARP was sent for this host. The address is learned,
                 * remove the request
                 */
                removePendingARPFromList(i);
                logger.debug("Host Removed from ARPPending List, IP: {}",
                        networkAddr.toString());
                return;
            }
        }

        /*
         * It could have been a host from the FailedARPReqList
         */

        for (int i = 0; i < failedARPReqList.size(); i++) {
            arphost = failedARPReqList.get(i);
            if (arphost.getHostIP().equals(networkAddr)) {
                /* An ARP was sent for this host. The address is learned,
                 * remove the request
                 */
                failedARPReqList.remove(i);
                logger.debug("Host Removed from FailedARPReqList List, IP: {}",
                        networkAddr.toString());
                return;
            }
        }
    }

    // Learn a new Host
    private void learnNewHost(HostNodeConnector host) {
        host.initArpSendCountDown();
        hostsDB.put(host.getNetworkAddress(), host);
        logger.debug("New Host Learned: MAC: {}  IP: {}", HexEncode
                .bytesToHexString(host.getDataLayerAddressBytes()), host
                .getNetworkAddress().getHostAddress());
    }

    // Remove known Host
    private void removeKnownHost(InetAddress key) {
        HostNodeConnector host = hostsDB.get(key);
        if (host != null) {
            logger.debug("Removing Host: IP:{}", host.getNetworkAddress()
                    .getHostAddress());
            hostsDB.remove(key);
        } else {
            logger
                    .error(
                            "removeKnownHost(): Host for IP address {} not found in hostsDB",
                            key.getHostAddress());
        }
    }

    private class NotifyHostThread extends Thread {

        private HostNodeConnector host;

        public NotifyHostThread(HostNodeConnector h) {
            this.host = h;
        }

        public void run() {
            /* Check for Host Move case */
            if (hostMoved(host)) {
                /*
                 * Host has been moved from one location (switch,port, MAC, or VLAN).
                 * Remove the existing host with its previous location parameters,
                 * inform the applications, and add it as a new Host
                 */
                HostNodeConnector removedHost = hostsDB.get(host
                        .getNetworkAddress());
                removeKnownHost(host.getNetworkAddress());
                if (removedHost != null) {
                    notifyHostLearnedOrRemoved(removedHost, false);
                    logger.debug(
                            "Host move occurred. Old Host:{}, New Host: {}",
                            removedHost, host);
                } else {
                    logger.error(
                            "Host to be removed not found in hostsDB. Host {}",
                            removedHost);
                }
            }

            /* check if there is an outstanding request for this host */
            InetAddress networkAddr = host.getNetworkAddress();

            // add and notify
            learnNewHost(host);
            ProcPendingARPReqs(networkAddr);
            notifyHostLearnedOrRemoved(host, true);
        }
    }

    public void hostListener(HostNodeConnector host) {

        if (hostExists(host)) {
            logger.debug("ARP received for Host: {}", host);
            HostNodeConnector existinghost = hostsDB.get(host
                    .getNetworkAddress());
            existinghost.initArpSendCountDown();
            return;
        }
        new NotifyHostThread(host).start();
    }

    // Notify whoever is interested that a new host was learned (dynamically or statically)
    private void notifyHostLearnedOrRemoved(HostNodeConnector host, boolean add) {
        // Update listeners if any
        if (newHostNotify != null) {
            synchronized (this.newHostNotify) {
                for (IfNewHostNotify ta : newHostNotify) {
                    try {
                        if (add) {
                            ta.notifyHTClient(host);
                        } else {
                            ta.notifyHTClientHostRemoved(host);
                        }
                    } catch (Exception e) {
                        logger.error("Exception on callback", e);
                    }
                }
            }
        } else {
            logger
                    .error("notifyHostLearnedOrRemoved(): New host notify is null");
        }

        // Topology update is for some reason outside of listeners registry logic
        Node node = host.getnodeconnectorNode();
        Host h = null;
        NodeConnector p = host.getnodeConnector();
        try {
            DataLinkAddress dla = new EthernetAddress(host
                    .getDataLayerAddressBytes());
            h = new org.opendaylight.controller.sal.core.Host(dla, host
                    .getNetworkAddress());
        } catch (ConstructionException ce) {
            p = null;
            h = null;
        }

        if (topologyManager != null && p != null && h != null) {
            if (add == true) {
                Tier tier = new Tier(1);
                switchManager.setNodeProp(node, tier);
                topologyManager.updateHostLink(p, h, UpdateType.ADDED, null);
                /*
                 * This is a temporary fix for Cisco Live's Hadoop Demonstration.
                 * The concept of Tiering must be revisited based on other application requirements
                 * and the design might warrant a separate module (as it involves tracking the topology/
                 * host changes & updating the Tiering numbers in an effective manner).
                 */
                updateSwitchTiers(node, 1);

                /*
                 * The following 2 lines are added for testing purposes.
                 * We can remove it once the North-Bound APIs are available for testing.

                ArrayList<ArrayList<String>> hierarchies = getHostNetworkHierarchy(host.getNetworkAddress());
                logHierarchies(hierarchies);
                 */
            } else {
                // No need to reset the tiering if no other hosts are currently connected
                // If this switch was discovered to be an access switch, it still is even if the host is down
                Tier tier = new Tier(0);
                switchManager.setNodeProp(node, tier);
                topologyManager.updateHostLink(p, h, UpdateType.REMOVED, null);
            }
        }
    }

    /**
     * When a new Host is learnt by the hosttracker module, it places the directly connected Node
     * in Tier-1 & using this function, updates the Tier value for all other Nodes in the network
     * hierarchy.
     *
     * This is a recursive function and it takes care of updating the Tier value for all the connected
     * and eligible Nodes.
     *
     * @param n	Node that represents one of the Vertex in the Topology Graph.
     * @param currentTier The Tier on which n belongs
     */
    private void updateSwitchTiers(Node n, int currentTier) {
        Map<Node, Set<Edge>> ndlinks = topologyManager.getNodeEdges();
        if (ndlinks == null) {
            logger.debug(
                    "updateSwitchTiers(): ndlinks null for Node: {}, Tier:{}",
                    n, currentTier);
            return;
        }
        Set<Edge> links = ndlinks.get(n);
        if (links == null) {
            logger.debug("updateSwitchTiers(): links null for ndlinks:{}",
                    ndlinks);
            return;
        }
        ArrayList<Node> needsVisiting = new ArrayList<Node>();
        for (Edge lt : links) {
            if (!lt.getHeadNodeConnector().getType().equals(
                    NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                // We don't want to work on Node that are not openflow
                // for now
                continue;
            }
            Node dstNode = lt.getHeadNodeConnector().getNode();
            if (switchNeedsTieringUpdate(dstNode, currentTier + 1)) {
                Tier t = new Tier(currentTier + 1);
                switchManager.setNodeProp(dstNode, t);
                //logger.info("Updating Switch Tier "+ (currentTier+1) +" for "+String.format("%x", dstSw.getId()));
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
     * Internal convenience routine to check the eligibility of a Switch for a Tier update.
     * Any Node with Tier=0 or a Tier value that is greater than the new Tier Value is eligible
     * for the update.
     *
     * @param n Node for which the Tier update eligibility is checked
     * @param tier new Tier Value
     * @return <code>true</code> if the Node is eligible for Tier Update
     *         <code>false</code> otherwise
     */

    private boolean switchNeedsTieringUpdate(Node n, int tier) {
        if (n == null) {
            logger.error("switchNeedsTieringUpdate(): Null node for tier: {}",
                    tier);
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
        if (t == null)
            return true;
        if (t.getValue() == 0)
            return true;
        else if (t.getValue() > tier)
            return true;
        //logger.info(getContainerName()+" -> "+ "Switch "+String.format("%x", sw.getId())+ " is in better Tier "+sw.getTier()+" ... skipping "+tier);
        return false;
    }

    /**
     * Internal convenience routine to clear all the Tier values to 0.
     * This cleanup is performed during cases such as Topology Change where the existing Tier values
     * might become incorrect
     */
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
            buf.append("Hierarchy#" + num + " : ");
            for (String switchName : hierarchy) {
                buf.append(switchName + "/");
            }
            logger.debug("{} -> {}", getContainerName(), buf.toString());
            num++;
        }
    }

    /**
     * getHostNetworkHierarchy is the Back-end routine for the North-Bound API that returns
     * the Network Hierarchy for a given Host. This API is typically used by applications like
     * Hadoop for Rack Awareness functionality.
     *
     * @param hostAddress IP-Address of the host/node.
     * @return Network Hierarchies represented by an Array of Array (of Switch-Ids as String).
     */
    public List<List<String>> getHostNetworkHierarchy(InetAddress hostAddress) {
        HostNodeConnector host = hostQuery(hostAddress);
        if (host == null)
            return null;

        List<List<String>> hierarchies = new ArrayList<List<String>>();
        ArrayList<String> currHierarchy = new ArrayList<String>();
        hierarchies.add(currHierarchy);

        Node node = host.getnodeconnectorNode();
        updateCurrentHierarchy(node, currHierarchy, hierarchies);
        return hierarchies;
    }

    /**
     * dpidToHostNameHack is a hack function for Cisco Live Hadoop Demo.
     * Mininet is used as the network for Hadoop Demos & in order to give a meaningful
     * rack-awareness switch names, the DPID is organized in ASCII Characters and
     * retrieved as string.
     *
     * @param dpid Switch DataPath Id
     * @return Ascii String represented by the DPID.
     */
    private String dpidToHostNameHack(long dpid) {
        String hex = Long.toHexString(dpid);

        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < hex.length(); i++) {
            result = (int) ((dpid >> (i * 8)) & 0xff);
            if (result == 0)
                continue;
            if (result < 0x30)
                result += 0x40;
            sb.append(String.format("%c", result));
        }
        return sb.reverse().toString();
    }

    /**
     * A convenient recursive routine to obtain the Hierarchy of Switches.
     *
     * @param node Current Node in the Recursive routine.
     * @param currHierarchy Array of Nodes that make this hierarchy on which the Current Switch belong
     * @param fullHierarchy Array of multiple Hierarchies that represent a given host.
     */
    @SuppressWarnings("unchecked")
    private void updateCurrentHierarchy(Node node,
            ArrayList<String> currHierarchy, List<List<String>> fullHierarchy) {
        //currHierarchy.add(String.format("%x", currSw.getId()));
        currHierarchy.add(dpidToHostNameHack((Long) node.getID()));
        ArrayList<String> currHierarchyClone = (ArrayList<String>) currHierarchy
                .clone(); //Shallow copy as required

        Map<Node, Set<Edge>> ndlinks = topologyManager.getNodeEdges();
        if (ndlinks == null) {
            logger
                    .debug(
                            "updateCurrentHierarchy(): topologyManager returned null ndlinks for node: {}",
                            node);
            return;
        }
        Node n = NodeCreator.createOFNode((Long) node.getID());
        Set<Edge> links = ndlinks.get(n);
        if (links == null) {
            logger.debug("updateCurrentHierarchy(): Null links for ndlinks");
            return;
        }
        for (Edge lt : links) {
            if (!lt.getHeadNodeConnector().getType().equals(
                    NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                // We don't want to work on Node that are not openflow
                // for now
                continue;
            }
            Node dstNode = lt.getHeadNodeConnector().getNode();

            Tier nodeTier = (Tier) switchManager.getNodeProp(node,
                    Tier.TierPropName);
            Tier dstNodeTier = (Tier) switchManager.getNodeProp(dstNode,
                    Tier.TierPropName);
            if (dstNodeTier.getValue() > nodeTier.getValue()) {
                ArrayList<String> buildHierarchy = currHierarchy;
                if (currHierarchy.size() > currHierarchyClone.size()) {
                    buildHierarchy = (ArrayList<String>) currHierarchyClone
                            .clone(); //Shallow copy as required
                    fullHierarchy.add(buildHierarchy);
                }
                updateCurrentHierarchy(dstNode, buildHierarchy, fullHierarchy);
            }
        }
    }

    @Override
    public void edgeUpdate(Edge e, UpdateType type, Set<Property> props) {
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
                logger.error("For now we cannot handle updates for "
                        + "non-openflow nodes");
                return;
            }

            if (dstType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                logger.debug("Skip updates for {}", e);
                return;
            }

            if (!dstType.equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                logger.error("For now we cannot handle updates for "
                        + "non-openflow nodes");
                return;
            }

            // At this point we know we got an openflow update, so
            // lets fill everything accordingly.
            srcNid = (Long) e.getTailNodeConnector().getNode()
                    .getID();
            srcPort = (Short) e.getTailNodeConnector().getID();
            dstNid = (Long) e.getHeadNodeConnector().getNode()
                    .getID();
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
        clearTiers();
        for (Entry<InetAddress, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            Node node = host.getnodeconnectorNode();
            if (node != null) {
                Tier t = new Tier(1);
                switchManager.setNodeProp(node, t);
                updateSwitchTiers(node, 1);
            }
        }
    }

    public void subnetNotify(Subnet sub, boolean add) {
        logger.debug("Received subnet notification: {}  add={}", sub, add);
        if (add) {
            for (int i = 0; i < failedARPReqList.size(); i++) {
                ARPPending arphost;
                arphost = failedARPReqList.get(i);
                logger.debug(
                        "Sending the ARP from FailedARPReqList fors IP: {}",
                        arphost.getHostIP().getHostAddress());
                hostFinder.find(arphost.getHostIP());
            }
        }
    }

    class OutStandingARPHandler extends TimerTask {
        public void run() {
            ARPPending arphost;
            /* This routine runs every 4 seconds */
            // logger.info ("ARP Handler called");
            for (int i = 0; i < ARPPendingList.size(); i++) {
                arphost = ARPPendingList.get(i);
                if (arphost.getSent_count() < switchManager.getHostRetryCount()) {
                    /* No reply has been received of first ARP Req, send the next one */
                    hostFinder.find(arphost.getHostIP());
                    arphost.sent_count++;
                    logger.debug("ARP Sent from ARPPending List, IP: {}",
                            arphost.getHostIP().getHostAddress());
                } else if (arphost.getSent_count() >= switchManager
                        .getHostRetryCount()) {
                    /* Two ARP requests have been sent without
                     * receiving a reply, remove this from the
                     * pending list
                     */
                    removePendingARPFromList(i);
                    logger
                            .debug(
                                    "ARP reply not received after two attempts, removing from Pending List IP: {}",
                                    arphost.getHostIP().getHostAddress());
                    /*
                     * Add this host to a different list which will be processed on link
                     * up events
                     */
                    logger.debug("Adding the host to FailedARPReqList IP: {}",
                            arphost.getHostIP().getHostAddress());
                    failedARPReqList.add(arphost);

                } else {
                    logger
                            .error(
                                    "Inavlid arp_sent count for entery at index: {}",
                                    i);
                }
            }
        }
    }

    private class ARPRefreshHandler extends TimerTask {
        public void run() {
            if ((clusterContainerService != null)
                    && !clusterContainerService.amICoordinator()) {
                return;
            }
            if ((switchManager != null)
                    && !switchManager.isHostRefreshEnabled()) {
                /*
                 * The host probe procedure was disabled by CLI
                 */
                return;
            }
            if (hostsDB == null) {
                /* hostsDB is not allocated yet */
                logger
                        .error("ARPRefreshHandler(): hostsDB is not allocated yet:");
                return;
            }
            for (Entry<InetAddress, HostNodeConnector> entry : hostsDB
                    .entrySet()) {
                HostNodeConnector host = entry.getValue();
                if (host.isStaticHost()) {
                    /* this host was learned via API3, don't age it out */
                    continue;
                }

                short arp_cntdown = host.getArpSendCountDown();
                arp_cntdown--;
                if (arp_cntdown > switchManager.getHostRetryCount()) {
                    host.setArpSendCountDown(arp_cntdown);
                } else if (arp_cntdown <= 0) {
                    /* No ARP Reply received in last 2 minutes, remove this host and inform applications*/
                    removeKnownHost(entry.getKey());
                    notifyHostLearnedOrRemoved(host, false);
                } else if (arp_cntdown <= switchManager.getHostRetryCount()) {
                    /* Use the services of arphandler to check if host is still there */
                    // logger.info("Probe for Host:{}", host);
                    //logger.info("ARP Probing ("+arp_cntdown+") for "+host.toString());
                    logger.trace("ARP Probing ({}) for {}({})", new Object[] {
                            arp_cntdown,
                            host.getNetworkAddress().getHostAddress(),
                            HexEncode.bytesToHexString(host
                                    .getDataLayerAddressBytes()) });
                    host.setArpSendCountDown(arp_cntdown);
                    hostFinder.probe(host);
                }
            }
        }
    }

    /**
     * Inform the controller IP to MAC binding of a host and its
     * connectivity to an openflow switch in terms of Node, port, and
     * VLAN.
     *
     * @param networkAddr   IP address of the host
     * @param dataLayer		Address MAC address of the host
     * @param nc            NodeConnector to which host is connected
     * @param port          Port of the switch to which host is connected
     * @param vlan          Vlan of which this host is member of
     *
     * @return Status		The status object as described in {@code Status}
     * 						indicating the result of this action.
     */

    public Status addStaticHostReq(InetAddress networkAddr,
            byte[] dataLayerAddress, NodeConnector nc, short vlan) {
        if (dataLayerAddress.length != 6) {
        	return new Status(StatusCode.BADREQUEST, "Invalid MAC address");
        }

        HostNodeConnector host = null;
        try {
            host = new HostNodeConnector(dataLayerAddress, networkAddr, nc,
                                         vlan);
            if (hostExists(host)) {
                // This host is already learned either via ARP or through a northbound request
                HostNodeConnector transHost = hostsDB.get(networkAddr);
                transHost.setStaticHost(true);
                return new Status(StatusCode.SUCCESS, null);
            }
            host.setStaticHost(true);
            /*
             * Before adding host, Check if the switch and the port have already come up
             */
            if (switchManager.isNodeConnectorEnabled(nc)) {
                learnNewHost(host);
                notifyHostLearnedOrRemoved(host, true);
            } else {
                inactiveStaticHosts.put(nc, host);
                logger
                        .debug(
                                "Switch or switchport is not up, adding host {} to inactive list",
                                networkAddr.getHostName());
            }
            return new Status(StatusCode.SUCCESS, null);
        } catch (ConstructionException e) {
            return new Status(StatusCode.INTERNALERROR, "Host could not be created");
        }

    }

    /**
     * Update the controller IP to MAC binding of a host and its
     * connectivity to an openflow switch in terms of
     * switch id, switch port, and VLAN.
     *
     * @param networkAddr   IP address of the host
     * @param dataLayer		Address MAC address of the host
     * @param nc            NodeConnector to which host is connected
     * @param port          Port of the switch to which host is connected
     * @param vlan          Vlan of which this host is member of
     *
     * @return boolean		true if the host was added successfully,
     * false otherwise
     */
    public boolean updateHostReq(InetAddress networkAddr,
                                 byte[] dataLayerAddress, NodeConnector nc,
                                 short vlan) {
        if (nc == null) {
            return false;
        }
        HostNodeConnector host = null;
        try {
            host = new HostNodeConnector(dataLayerAddress, networkAddr, nc,
                                         vlan);
            if (!hostExists(host)) {
                if ((inactiveStaticHosts.get(nc)) != null) {
                    inactiveStaticHosts.replace(nc, host);
                    return true;
                }
                return false;
            }
            hostsDB.replace(networkAddr, host);
            return true;
        } catch (ConstructionException e) {
        }
        return false;
    }

    /**
     * Remove from the controller IP to MAC binding of a host and its
     * connectivity to an openflow switch
     *
     * @param networkAddr   IP address of the host
     *
     * @return boolean		true if the host was removed successfully,
     * false otherwise
     */

    public Status removeStaticHostReq(InetAddress networkAddress) {
        // Check if host is in active hosts database
        HostNodeConnector host = getHostFromOnActiveDB(networkAddress);
        if (host != null) {
            // Validation check
            if (!host.isStaticHost()) {
            	return new Status(StatusCode.FORBIDDEN,
            			"Host " + networkAddress.getHostName() + 
            			" is not static");
            }
            // Remove and notify
            notifyHostLearnedOrRemoved(host, false);
            removeKnownHost(networkAddress);
            return new Status(StatusCode.SUCCESS, null);
        }

        // Check if host is in inactive hosts database
        Entry<NodeConnector, HostNodeConnector> entry = getHostFromInactiveDB(networkAddress);
        if (entry != null) {
            host = entry.getValue();
            // Validation check
            if (!host.isStaticHost()) {
            	return new Status(StatusCode.FORBIDDEN,
            			"Host " + networkAddress.getHostName() + 
            			" is not static");
            }
            this.removeHostFromInactiveDB(networkAddress);
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
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        if (node == null)
            return;

        switch (type) {
        case REMOVED:
            long sid = (Long) node.getID();
            logger.debug("Received removedSwitch for sw id {}", HexEncode
                    .longToHexString(sid));
            for (Entry<InetAddress, HostNodeConnector> entry : hostsDB
                    .entrySet()) {
                HostNodeConnector host = entry.getValue();
                if (host.getnodeconnectornodeId() == sid) {
                    logger.debug("Switch: {} is down, remove from Hosts_DB",
                            sid);
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
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null)
            return;

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
    public Status addStaticHost(String networkAddress, String dataLayerAddress,
                                NodeConnector nc, String vlan) {
        try {
            InetAddress ip = InetAddress.getByName(networkAddress);
            if (nc == null) {
            	return new Status(StatusCode.BADREQUEST, "Invalid NodeId");
            }
            return addStaticHostReq(ip,
                                    HexEncode
                                    .bytesFromHexString(dataLayerAddress),
                                    nc,
                    Short.valueOf(vlan));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return new Status(StatusCode.BADREQUEST, "Invalid Address");
        }
    }

    @Override
    public Status removeStaticHost(String networkAddress) {
        InetAddress address;
        try {
            address = InetAddress.getByName(networkAddress);
            return removeStaticHostReq(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return new Status(StatusCode.BADREQUEST, "Invalid Address");
        }
    }

    private void handleNodeConnectorStatusUp(NodeConnector nodeConnector) {
        ARPPending arphost;

        logger.debug("handleNodeConnectorStatusUp {}", nodeConnector);

        for (int i = 0; i < failedARPReqList.size(); i++) {
            arphost = failedARPReqList.get(i);
            logger.debug("Sending the ARP from FailedARPReqList fors IP: {}",
                    arphost.getHostIP().getHostAddress());
            hostFinder.find(arphost.getHostIP());
        }
        HostNodeConnector host = inactiveStaticHosts.get(nodeConnector);
        if (host != null) {
            inactiveStaticHosts.remove(nodeConnector);
            learnNewHost(host);
            notifyHostLearnedOrRemoved(host, true);
        }
    }

    private void handleNodeConnectorStatusDown(NodeConnector nodeConnector) {
        long sid = (Long) nodeConnector.getNode().getID();
        short port = (Short) nodeConnector.getID();

        logger.debug("handleNodeConnectorStatusDown {}", nodeConnector);

        for (Entry<InetAddress, HostNodeConnector> entry : hostsDB.entrySet()) {
            HostNodeConnector host = entry.getValue();
            if ((host.getnodeconnectornodeId() == sid)
                    && (host.getnodeconnectorportId() == port)) {
                logger.debug(
                        "Switch: {}, Port: {} is down, remove from Hosts_DB",
                        sid, port);
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
        if (containerName == null)
            return GlobalConstants.DEFAULT.toString();
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
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        destroyCache();
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        // TODO Auto-generated method stub

    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        // TODO Auto-generated method stub

    }

}
