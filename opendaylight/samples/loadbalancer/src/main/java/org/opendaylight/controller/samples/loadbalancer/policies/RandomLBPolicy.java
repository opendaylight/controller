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
import java.util.Random;

import org.opendaylight.controller.samples.loadbalancer.ConfigManager;
import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the random load balancing policy.
 *
 */
public class RandomLBPolicy implements ILoadBalancingPolicy {
    
    /*
     * Instance logger
     */
    private static final Logger rLogger = LoggerFactory.getLogger(RandomLBPolicy.class);
    
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
     * Random generator
     */
    Random randomGenerator = null;
    
    @SuppressWarnings("unused")
    private RandomLBPolicy(){}
    
    public RandomLBPolicy(ConfigManager cmgr){
        this.cmgr = cmgr;
        this.clientMemberMap = new HashMap<Client, PoolMember>();
        randomGenerator = new Random();
    }
    @Override
    public String getPoolMemberForClient(Client source, VIP dest){
        
        rLogger.info("Received traffic from client : {} for VIP : {} ",source, dest);
        
        syncWithLoadBalancerData();
        
        PoolMember pm= null;
        
        if(this.clientMemberMap.containsKey(source)){
            pm= this.clientMemberMap.get(source);
            rLogger.info("Client {} had sent traffic before,new traffic will be routed to the same pool member {}",source,pm);
        }else{
            Pool pool = null;
            pool = this.cmgr.getPool(dest.getPoolName());
            int memberNum = this.randomGenerator.nextInt(pool.getAllMembers().size()-1);
            pm = pool.getAllMembers().get(memberNum);
            this.clientMemberMap.put(source, pm );
            rLogger.info("Network traffic from client {} will be directed to pool member {}",pm);
        }
        return pm.getIp();
    }
    
    /*
     * This method does the clean up. Whenever a new client packet arrives with a given VIP,
     * this method checks the current configuration to see if any pool members have been deleted and
     * cleans up the metadata stored by this loadbalancing algorithm.
     */
    private void syncWithLoadBalancerData(){
        rLogger.debug("[Client - PoolMember] table before cleanup : {}",this.clientMemberMap.toString());
        
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
            
            rLogger.debug("Removed client : {} ",client);
        }
        rLogger.debug("[Client - PoolMember] table after cleanup : {}",this.clientMemberMap.toString());
    }
    
}
