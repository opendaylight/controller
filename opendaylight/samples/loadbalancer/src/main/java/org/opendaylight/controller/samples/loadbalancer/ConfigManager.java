/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a configuration manager for the Load Balancer service.
 * This class is responsible for managing(store/update/delete) the load balancer 
 * configuration that it receives through REST APIs or from any other applications
 * present in the controller.
 *
 */
public class ConfigManager implements IConfigManager{
    
    /*
     * Logger instance
     */
    private static final Logger cmLogger = LoggerFactory.getLogger(ConfigManager.class);
    
    /*
     * All the available VIPs 
     */
    private HashMap<String,VIP> vips = new HashMap<String,VIP>();
    
    /*
     * All the available Pools
     */
    private HashMap<String,Pool> pools = new HashMap<String,Pool>();
    
    public ConfigManager(){
    }
    
    @Override
    public boolean vipExists(String name) {
        return this.vips.containsKey(name);
    }
    
    @Override
    public boolean vipExists(VIP vip){
        if(vip.getName()==null){
            if(!vips.containsValue(vip)){
                return false;
            }
        }else{
            if(!vips.containsKey(vip.getName())){
                if(!vips.containsValue(vip)){
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean vipExists(String name,String ip,String protocol,short protocolPort,String poolName){
        
        VIP vip = new VIP(name,ip,protocol,protocolPort,poolName);
        
        //Check VIP with the same name
        
        if(!vips.containsKey(name)){
            //Check VIP with the same ip, protocol and protocolPort
            if(!vips.containsValue(vip)){
                
                //if you reach here, means this VIP don't exist
                return false;
            }
        }
        
        //Yeah, i have it.
    	return true;
    }
    
    @Override
    public Set<VIP> getAllVIPs(){
        return new HashSet<VIP>(this.vips.values());
    }
    
    public VIP getVIPWithPoolName(VIP vip){
        cmLogger.info("Search a VIP with name:{}",vip);
        for(VIP vipTemp: this.vips.values()){
            if(vipTemp.equals(vip)){
                
                cmLogger.info("Found VIP with pool detail : {}",vipTemp);
                return vipTemp;
            }
        }
        
        cmLogger.info("VIP with pool detail not found ");
        return null;
    }
    
    @Override
    public VIP createVIP(String name,String ip,String protocol,short protocolPort,String poolName){
        
        cmLogger.info("Create VIP with the following details :[ name : "+name
                                                                    +" ip : "+ip
                                                                    +" protocol : "+protocol
                                                                    +" protocol_port : "+protocolPort
                                                                    +" pool name : "+poolName);
        
        VIP vip = new VIP(name,ip,protocol,protocolPort,poolName);
        
        if(poolName !=null && !poolName.isEmpty()){
            if(pools.containsKey(poolName)){
                pools.get(poolName).addVIP(vip);
            }
        }
        
        vip.setStatus(LBConst.STATUS_ACTIVE);
        this.vips.put(name, vip);
        
        cmLogger.info("New VIP created : "+vip.toString());
        return vip;
    }
    
    @Override
    public String getVIPAttachedPool(String name) {
        return this.vips.get(name).getPoolName();
    }
    
    @Override
    public VIP updateVIP(String name, String poolName){
        
        cmLogger.info("Updating VIP : "+name+" pool name  to "+poolName);
        
        if(vips.containsKey(name)){
            VIP vip = vips.get(name);
            if(vip.getPoolName() == null){
                vip.setPoolName(poolName);
                cmLogger.error("VIP is now attached to the pool : {}",vip.toString());
                return vip;
            }
            cmLogger.error("VIP is already attached to one pool : {}",vip.toString());
        }
        cmLogger.error("VIP with name: "+name+" does not exist");
        return null;
    }
    
    @Override
    public VIP deleteVIP(String name){
        
        cmLogger.info("Deleting VIP : "+name);
        
        VIP vip = vips.get(name);
        
        String poolName = vip.getPoolName();
        
        if(poolName != null){
            if(pools.containsKey(poolName)){
                Pool pool = pools.get(poolName);
                pool.removeVIP(vip.getName());
            }
        }
        
        cmLogger.info("VIP removed : "+vip.toString());
        
        vips.remove(vip.getName());
        
        return vip;
    }
    
    @Override
    public boolean memberExists(String name, String poolName) {
        if(this.pools.containsKey(poolName)){
            if(this.pools.get(poolName).getMember(name) != null )
                return true;
        }
        return false;
    }
    
    @Override
    public boolean memberExists(String name, String memberIP,String poolName){
        if(!this.pools.containsKey(poolName))
            return false;
        
        return this.pools.get(poolName).poolMemberExists(new PoolMember(name, memberIP, poolName));
    }
    
    @Override
    public PoolMember addPoolMember(String name, String memberIP, String poolName){
        
        PoolMember pm = new PoolMember(name,memberIP,poolName);
        
        cmLogger.info("Adding pool member : "+pm.toString());
        
        pools.get(poolName).addMember(pm);
        
        return pm;
    }
    
    @Override
    public PoolMember removePoolMember(String name, String poolName){
        
        cmLogger.info("Removing pool member : {} from pool {}",name, poolName);
        
        Pool pool = pools.get(poolName);
        
        PoolMember pm = pool.getMember(name);
        
        pool.removeMember(name);
        
        cmLogger.info("Pool member {} removed from {} ",name,poolName);
        
        return pm;
    }
    
    @Override
    public Set<Pool> getAllPools(){
        return new HashSet<Pool>(this.pools.values());
    }
    
    @Override
    public boolean poolExists(String name) {
        return this.pools.containsKey(name);
    }
    
    @Override
    public boolean poolExists(String name, String lbMethod){
        
        return pools.containsValue(new Pool(name,lbMethod));
    }
    
    @Override
    public Pool createPool(String name, String lbMethod){
        
        Pool newPool = new Pool(name,lbMethod);
        
        cmLogger.info("New pool created : " + newPool.toString());
        
        pools.put(name, newPool);
        
        return newPool;
    }
    
    @Override
    public Pool deletePool(String poolName){
        
        Pool pool = pools.get(poolName);
        
        for(VIP vip:pool.getAllVip()){
            
            vip.setPoolName(null);
            
        }
        
        cmLogger.info("Pool removed : "+pool.toString());
        
        pools.remove(poolName);
        
        return pool;
    }
    
    @Override
    public Pool getPool( String poolName){
        if(pools.containsKey(poolName)){
            return pools.get(poolName);
        }
        return null;
    }
    
    @Override
    public Set<PoolMember> getAllPoolMembers(String poolName) {
        
        if(pools.containsKey(poolName)){
            return new HashSet<PoolMember>(pools.get(poolName).getAllMembers());
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ConfigManager [vips=" + vips + ", pools=" + pools + "]";
    }
}