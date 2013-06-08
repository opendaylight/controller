/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.policies;

import java.util.ArrayList;
import java.util.HashMap;

import org.opendaylight.controller.samples.loadbalancer.ConfigManager;
import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the round robin load balancing policy.
 *
 */
public class RoundRobinLBPolicy implements ILoadBalancingPolicy{
    
    /*
     * Logger instance
     */
    private static final Logger rrLogger = LoggerFactory.getLogger(RoundRobinLBPolicy.class);
    
    /*
     * Reference to the configuration manager. This reference is passed from load balancer 
     * class.
     */
    private ConfigManager cmgr;
    
    /*
     * Mapping between the client and the pool member that serves all traffic for that client.
     */
    private HashMap<Client, PoolMember> clientMemberMap;
    
    /*
     * Maintains the next pool member counter for the VIPs.
     * More than one VIP can be attached to one pool, so each VIP
     * will have its own counter for the next pool member from
     * the same pool.
     */
    private HashMap<VIP,Integer> nextItemFromPool;
    
    @SuppressWarnings("unused")
    private RoundRobinLBPolicy(){}
    
    public RoundRobinLBPolicy(ConfigManager cmgr){
        this.cmgr = cmgr;
        this.clientMemberMap = new HashMap<Client, PoolMember>();
        this.nextItemFromPool = new HashMap<VIP, Integer>();
    }
    
    @Override
    public String getPoolMemberForClient(Client source, VIP dest){
        
        rrLogger.info("Received traffic from client : {} for VIP : {} ",source, dest);
        
        syncWithLoadBalancerData();
        
        PoolMember pm= null;
        
        if(this.clientMemberMap.containsKey(source)){
            
            pm= this.clientMemberMap.get(source);
            rrLogger.info("Client {} had sent traffic before,new traffic will be routed to the same pool member {}",source,pm);
        }else{
            
            Pool pool = null;
            if(nextItemFromPool.containsKey(dest)){
                
                int memberNum = nextItemFromPool.get(dest).intValue();
                rrLogger.debug("Packet is from new client for VIP {}",dest);
                pool = this.cmgr.getPool(dest.getPoolName());
                pm = pool.getAllMembers().get(memberNum);
                this.clientMemberMap.put(source, pm );
                rrLogger.info("New client's packet will be directed to pool member {}",pm);
                memberNum++;
                
                if(memberNum > pool.getAllMembers().size()-1){
                    memberNum = 0;
                }
                rrLogger.debug("Next pool member for new client of VIP is set to {}",pool.getAllMembers().get(memberNum));
                
                this.nextItemFromPool.put(dest, new Integer(memberNum));
            }else{
                rrLogger.debug("Network traffic for VIP : {} has appeared first time from client {}",dest,source);
                pool = this.cmgr.getPool(dest.getPoolName());
                pm = pool.getAllMembers().get(0);
                this.clientMemberMap.put(source, pm);
                
                rrLogger.info("Network traffic from client {} will be directed to pool member {}",pm);
                this.nextItemFromPool.put(dest, new Integer(1));
                rrLogger.debug("Next pool member for new client of VIP is set to {}",pool.getAllMembers().get(1));
            }
        }
        return pm.getIp();
    }
    
    /*
     * This method does the clean up. Whenever a new client packet arrives with a given VIP,
     * this method checks the current configuration to see if any pool members have been deleted and
     * cleans up the metadata stored by this loadbalancing algorithm.
     */
    private void syncWithLoadBalancerData(){
        rrLogger.debug("[Client - PoolMember] table before cleanup : {}",this.clientMemberMap.toString());
        ArrayList<Client> removeClient = new ArrayList<Client>();
        
        if(this.clientMemberMap.size() != 0){
            for(Client client : this.clientMemberMap.keySet()){
                if(!this.cmgr.memberExists(this.clientMemberMap.get(client).getName(),
                                            this.clientMemberMap.get(client).getPoolName())){
                    
                    removeClient.add(client);
                }
            }
        }
        
        for(Client client : removeClient){
            this.clientMemberMap.remove(client);
            
            rrLogger.debug("Removed client : {} ",client);
        }
        rrLogger.debug("[Client - PoolMember] table after cleanup : {}",this.clientMemberMap.toString());
        
        rrLogger.debug("[VIP- NextMember] table before cleanup : {}",this.nextItemFromPool.toString());
        
        ArrayList<VIP> resetVIPPoolMemberCount= new ArrayList<VIP>();
        
        if(this.nextItemFromPool.size() != 0){
            
            for(VIP vip:this.nextItemFromPool.keySet()){
                if(this.nextItemFromPool.get(vip).intValue() > this.cmgr.getPool(vip.getPoolName()).getAllMembers().size()-1){
                    
                    resetVIPPoolMemberCount.add(vip);
                }
            }
        }
        
        for(VIP vip:resetVIPPoolMemberCount){
            rrLogger.debug("VIP next pool member counter reset to 0");
            this.nextItemFromPool.put(vip, new Integer(0));
        }
        
        rrLogger.debug("[VIP- NextMember] table after cleanup : {}",this.nextItemFromPool.toString());
    }
}
