/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.samples.loadbalancer.ConfigManager;
import org.opendaylight.controller.samples.loadbalancer.IConfigManager;
import org.opendaylight.controller.samples.loadbalancer.LBConst;
import org.opendaylight.controller.samples.loadbalancer.LBUtil;
import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.opendaylight.controller.samples.loadbalancer.policies.RandomLBPolicy;
import org.opendaylight.controller.samples.loadbalancer.policies.RoundRobinLBPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the main class that represents the load balancer service. 
 * This is a sample load balancer application that balances traffic to backend servers 
 * based on the source address and source port on each incoming packet.  The service 
 * reactively installs OpenFlow rules to direct all packets with a specific source address
 * and source port to one of the appropriate backend servers.  The servers may be chosen 
 * using a round robin policy or a random policy. This service can be configured via a 
 * REST APIs which are similar to the OpenStack Quantum LBaaS (Load-balancer-as-a-Service)
 * v1.0 API proposal (http://wiki.openstack.org/Quantum/LBaaS)
 * 
 * To use this service, a virtual IP (or VIP) should be exposed to the clients of this service
 * and used as the destination address. A VIP is a entity that comprises of a virtual IP, port
 * and protocol (TCP or UDP).
 * Assumptions:
 *      1. One or more VIPs may be mapped to the same server pool. All VIPs that share the same
 *      pool must also share the same load balancing policy (random or round robin).
 *      
 *      2. Only one server pool can be be assigned to a VIP.
 *      
 *      3. All flow rules are installed with an idle timeout of 5 seconds.
 *      
 *      4. Packets to a VIP must leave the OpenFlow  cluster from the same switch from where
 *      it entered it.
 *      
 *      5. When you delete a VIP or a server pool or a server from a pool, the service does not
 *      delete the flow rules it has already installed. The flow rules should automatically
 *      time out after the idle timeout of 5 seconds. 
 *
 */
public class LoadBalancerService implements IListenDataPacket, IConfigManager{
    
    /*
     * Logger instance
     */
    private static Logger lbsLogger = LoggerFactory.getLogger(LoadBalancerService.class);
    
    /*
     * Single instance of the configuration manager. Application passes this reference to all
     * the new policies implemented for load balancing.
     */
    private static ConfigManager configManager = new ConfigManager();
    
    /*
     * Round robing policy instance. Need to implement factory patterns to get
     * policy instance.
     */
    private static RoundRobinLBPolicy rrLBMethod= new RoundRobinLBPolicy(configManager);
    
    /*
     * Random policy instance.
     */
    private static RandomLBPolicy ranLBMethod= new RandomLBPolicy(configManager);
    
    /*
     * Reference to the data packet service
     */
    private IDataPacketService dataPacketService = null;
    
    /*
     * Reference to the host tracker service
     */
    private IfIptoHost hostTracker;
    
    /*
     * Reference to the forwarding manager
     */
    private IForwardingRulesManager ruleManager;
    
    /*
     * Reference to the routing service
     */
    private IRouting routing;
    
    /*
     * Load balancer application installs all flows with priority 2.
     */
    private static short LB_IPSWITCH_PRIORITY = 2;

    /*
     * Name of the container where this application will register.
     */
    private String containerName = null;

    /*
     * Set/unset methods for the service instance that load balancer 
     * service requires
     */
    public String getContainerName() {
        if (containerName == null)
            return GlobalConstants.DEFAULT.toString();
        return containerName;
    }
    
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }
    
    public void setRouting(IRouting routing) {
        this.routing = routing;
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
    	lbsLogger.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
    	lbsLogger.debug("Setting ForwardingRulesManager");
        this.ruleManager = forwardingRulesManager;
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.ruleManager == forwardingRulesManager) {
            this.ruleManager = null;
        }
    }

    /**
     * This method receives first packet of flows for which there is no
     * matching flow rule installed on the switch. IP addresses used for VIPs 
     * are not supposed to be used by any real/virtual host in the network.
     * Hence, any forwarding/routing service will not install any flows rules matching
     * these VIPs. This ensures that all the flows destined for VIPs will not find a match
     * in the switch and will be forwarded to the load balancing service.
     * Service will decide where to route this traffic based on the load balancing 
     * policy of the VIP's attached pool and will install appropriate flow rules 
     * in a reactive manner. 
     */
    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt){
        
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        
        if (formattedPak instanceof Ethernet) {
            byte[] vipMacAddr = ((Ethernet) formattedPak).getDestinationMACAddress();
            Object ipPkt = formattedPak.getPayload();
            
            if (ipPkt instanceof IPv4) {
                
                lbsLogger.debug("Packet recieved from switch : {}",inPkt.getIncomingNodeConnector().getNode().toString());
                IPv4 ipv4Pkt = (IPv4)ipPkt;
                if(IPProtocols.getProtocolName(ipv4Pkt.getProtocol()).equals(IPProtocols.TCP.toString())
                        || IPProtocols.getProtocolName(ipv4Pkt.getProtocol()).equals(IPProtocols.TCP.toString())){
                    
                    lbsLogger.debug("Packet protocol : {}",IPProtocols.getProtocolName(ipv4Pkt.getProtocol()));
                    Client client = new LBUtil().getClientFromPacket(ipv4Pkt);
                    VIP vip = new LBUtil().getVIPFromPacket(ipv4Pkt);
                    
                    if(configManager.vipExists(vip)){
                        VIP vipWithPoolName = configManager.getVIPWithPoolName(vip);
                        String poolMemberIp = null;
                        if(configManager.getPool(vipWithPoolName.getPoolName()).getLbMethod().equalsIgnoreCase(LBConst.ROUND_ROBIN_LB_METHOD)){
                            
                            poolMemberIp = rrLBMethod.getPoolMemberForClient(client,vipWithPoolName);
                        }
                        
                        if(configManager.getPool(vipWithPoolName.getPoolName()).getLbMethod().equalsIgnoreCase(LBConst.RANDOM_LB_METHOD)){
                            poolMemberIp = ranLBMethod.getPoolMemberForClient(client,vipWithPoolName);
                        }
                        
                        try {
                            
                            Node clientNode = inPkt.getIncomingNodeConnector().getNode();
                            HostNodeConnector hnConnector = this.hostTracker.hostFind(InetAddress.getByName(poolMemberIp));
                            
                            Node destNode = hnConnector.getnodeconnectorNode();
                            
                            lbsLogger.debug("Client is connected to switch : {}",clientNode.toString());
                            lbsLogger.debug("Destination pool machine is connected to switch : {}",destNode.toString());
                            
                            //Get path between both the nodes
                            Path route = this.routing.getRoute(clientNode, destNode);
                            
                            lbsLogger.info("Path between source (client) and destination switch nodes : {}",route.toString());
                            
                            NodeConnector forwardPort = route.getEdges().get(0).getTailNodeConnector();
                            
                            if(installLoadBalancerFlow(client,
                                                            vip,
                                                            clientNode,
                                                            poolMemberIp,
                                                            hnConnector.getDataLayerAddressBytes(),
                                                            forwardPort,
                                                            LBConst.FORWARD_DIRECTION_LB_FLOW)){
                                lbsLogger.info("Traffic from client : {} will be routed " +
                                                            "to pool machine : {}",client,poolMemberIp);
                            }else{
                                lbsLogger.error("Not able to route traffic from client : {}",client );
                            }
                            
                            if(installLoadBalancerFlow(client,
                                                            vip,
                                                            clientNode,
                                                            poolMemberIp,
                                                            vipMacAddr,
                                                            inPkt.getIncomingNodeConnector(),
                                                            LBConst.REVERSE_DIRECTION_LB_FLOW)){
                                lbsLogger.info("Flow rule installed to change the source ip/mac from " +
                                                            "pool machine ip {} to VIP {} for traffic coming pool machine",poolMemberIp,vip);
                            }else{
                                lbsLogger.error("Not able to route traffic from client : {}",client );
                            }
                        }catch (UnknownHostException e) {
                            lbsLogger.error("Pool member not found  in the network : {}",e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return PacketResult.IGNORED;
    }
    
    /*
     * This method installs the flow rule for routing the traffic between two hosts.
     * @param source    Traffic is sent by this source
     * @param dest      Traffic is destined to this destination (VIP)
     * @param sourceSwitch      Switch from where controller received the packet
     * @param destMachineIp     IP address of the pool member where traffic needs to be routed
     * @param destMachineMac    MAC address of the pool member where traffic needs to be routed
     * @param outport   Use this port to send out traffic
     * @param flowDirection     FORWARD_DIRECTION_LB_FLOW or REVERSE_DIRECTION_LB_FLOW
     * @return	true     If flow installation was successful
     *          false 	else
     *          @throws UnknownHostException
     */
    private boolean installLoadBalancerFlow(Client source,
                                            VIP dest,
                                            Node sourceSwitch,
                                            String destMachineIp,
                                            byte[] destMachineMac, 
                                            NodeConnector outport,
                                            int flowDirection) throws UnknownHostException{
        
        Match match = new Match();
        List<Action> actions = new ArrayList<Action>();
        
        if(flowDirection == LBConst.FORWARD_DIRECTION_LB_FLOW){
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_SRC, InetAddress.getByName(source.getIp()));
            match.setField(MatchType.NW_DST, InetAddress.getByName(dest.getIp()));
            match.setField(MatchType.NW_PROTO, IPProtocols.getProtocolNumberByte(dest.getProtocol()));
            match.setField(MatchType.TP_SRC, source.getPort());
            match.setField(MatchType.TP_DST, dest.getPort());
            
            actions.add(new SetNwDst(InetAddress.getByName(destMachineIp)));
            actions.add(new SetDlDst(destMachineMac));
        }
        
        if(flowDirection == LBConst.REVERSE_DIRECTION_LB_FLOW){
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_SRC, InetAddress.getByName(destMachineIp));
            match.setField(MatchType.NW_DST, InetAddress.getByName(source.getIp()));
            match.setField(MatchType.NW_PROTO, IPProtocols.getProtocolNumberByte(source.getProtocol()));
            match.setField(MatchType.TP_SRC, dest.getPort());
            match.setField(MatchType.TP_DST,source.getPort());
            
            actions.add(new SetNwSrc(InetAddress.getByName(dest.getIp())));
            actions.add(new SetDlSrc(destMachineMac));
        }
        
        actions.add(new Output(outport));
        
        // Make sure the priority for IP switch entries is
        // set to a level just above default drop entries
        
        Flow flow = new Flow(match, actions);
        flow.setIdleTimeout((short) 5);
        flow.setHardTimeout((short) 0);
        flow.setPriority(LB_IPSWITCH_PRIORITY);
        
        String policyName = source.getIp()+":"+source.getProtocol()+":"+source.getPort();
        String flowName =null;
        
        if(flowDirection == LBConst.FORWARD_DIRECTION_LB_FLOW){
            flowName = "["+policyName+":"+source.getIp() + ":"+dest.getIp()+"]";
        }
        
        if(flowDirection == LBConst.REVERSE_DIRECTION_LB_FLOW){
            
            flowName = "["+policyName+":"+dest.getIp() + ":"+source.getIp()+"]";
        }
        
        FlowEntry fEntry = new FlowEntry(policyName, flowName, flow, sourceSwitch);
        
        lbsLogger.info("Install flow entry {} on node {}",fEntry.toString(),sourceSwitch.toString());
        
        if(!this.ruleManager.checkFlowEntryConflict(fEntry)){
            if(this.ruleManager.installFlowEntry(fEntry).isSuccess()){
                return true;
            }else{
                lbsLogger.error("Error in installing flow entry to node : {}",sourceSwitch);
            }
        }else{
            lbsLogger.error("Conflicting flow entry exists : {}",fEntry.toString());
        }
        return false;
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
            
            lbsLogger.info("Running container name:" + this.containerName);
        }else {
            
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        lbsLogger.info(configManager.toString());
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
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    /*
     * All the methods below are just proxy methods to direct the REST API requests to configuration
     * manager. We need this redirection as currently, opendaylight supports only one 
     * implementation of the service. 
     */
    @Override
    public Set<VIP> getAllVIPs() {
        return configManager.getAllVIPs();
    }
    
    @Override
    public boolean vipExists(String name, String ip, String protocol,
                                short protocolPort, String poolName) {
        return configManager.vipExists(name, ip, protocol, protocolPort, poolName);
    }
    
    @Override
    public boolean vipExists(VIP vip) {
        return configManager.vipExists(vip);
    }
    
    @Override
    public VIP createVIP(String name, String ip, String protocol,
                            short protocolPort, String poolName) {
        return configManager.createVIP(name, ip, protocol, protocolPort, poolName);
    }
    
    @Override
    public VIP updateVIP(String name, String poolName) {
        return configManager.updateVIP(name, poolName);
    }
    
    @Override
    public VIP deleteVIP(String name) {
        return configManager.deleteVIP(name);
    }
    
    @Override
    public boolean memberExists(String name, String memberIP, String poolName) {
        return configManager.memberExists(name, memberIP, poolName);
    }
    
    @Override
    public Set<PoolMember> getAllPoolMembers(String poolName) {
        
        return configManager.getAllPoolMembers(poolName);
    }
    
    @Override
    public PoolMember addPoolMember(String name, 
                                    String memberIP,
                                    String poolName) {
        return configManager.addPoolMember(name, memberIP, poolName);
    }
    
    @Override
    public PoolMember removePoolMember(String name, String poolName) {
        
        return configManager.removePoolMember(name, poolName);
    }
    
    @Override
    public Set<Pool> getAllPools() {
        
        return configManager.getAllPools();
    }
    
    @Override
    public Pool getPool(String poolName) {
        return configManager.getPool(poolName);
    }
    
    @Override
    public boolean poolExists(String name, String lbMethod) {
        return configManager.poolExists(name, lbMethod);
    }
    
    @Override
    public Pool createPool(String name, String lbMethod) {
        return configManager.createPool(name, lbMethod);
    }
    
    @Override
    public Pool deletePool(String poolName) {
        return configManager.deletePool(poolName);
    }
    
    @Override
    public boolean vipExists(String name) {
        return configManager.vipExists(name);
    }
    
    @Override
    public boolean memberExists(String name, String poolName) {
        return configManager.memberExists(name, poolName);
    }
    
    @Override
    public boolean poolExists(String name) {
        return configManager.poolExists(name);
    }
    
    @Override
    public String getVIPAttachedPool(String name) {
        return configManager.getVIPAttachedPool(name);
    }
}
