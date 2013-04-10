/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer;

import java.util.Set;

import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;

/**
 * Interface implemented by the configuration manager.
 *
 */
public interface IConfigManager {
    
    /**
     * Return all existing VIPs
     * @return Set of VIP's
     * if there is no VIP, it will return empty set.
     */
    public Set<VIP> getAllVIPs();
    
    /**
     * Check if VIP with the 'name' exists
     * @param name	Name of the VIP 
     * @return	true 	if exists
     * 			false 	else
     */
    public boolean vipExists(String name);

    /**
     * Check if VIP exists with the details 'VIP' 
     * @param vip Search for this VIP 
     * @return	true 	if exists
     * 			false 	else
     */
    public boolean vipExists(VIP vip);

    /**
     * Check if VIP with the provided details exists
     * @param name	Name of the VIP
     * @param ip	IP of the VIP
     * @param protocol	IP Protocol of the VIP (TCP/UDP)
     * @param protocolPort	Transport port of the VIP (e.g 5550)
     * @param poolName	Name of the pool attached with the VIP
     * @return	true 	if exists
     * 			false	else
     */
    public boolean vipExists(String name,String ip,String protocol,short protocolPort,String poolName);
    
    /**
     * Add VIP to the configuration
     * @param name	Name of the VIP
     * @param ip	IP of the VIP
     * @param protocol	IP Protocol of the VIP (TCP/UDP)
     * @param protocolPort	Transport port of the VIP
     * @param poolName	Name of the pool that VIP will use for load balancing its traffic
     * @return	Newly created VIP
     */
    public VIP createVIP(String name,String ip,String protocol,short protocolPort,String poolName);
    
    /**
     * Return pool attached to VIP
     * @param name Name of the VIP
     * @return	Name of the pool attached to VIP
     * 			else null
     */
    public String getVIPAttachedPool(String name);
    /**
     * Update pool name of the VIP.
     * @param name	Name of the VIP
     * @param poolName	Attach this pool to VIP
     * @return	Updated VIP 	If successful
     * 			null			If this VIP is already attached to any existing pool.
     */			
    public VIP updateVIP(String name, String poolName);
    
    /**
     * Delete the VIP
     * @param name	Delete VIP with this name
     * @return	Details of the deleted VIP
     */
    public VIP deleteVIP(String name);

    /**
     * Check if pool member with the 'name' present in the pool with name 'poolName' 
     * @param name	Name of the pool member 
     * @param poolName	Name of the pool, you want to search for pool member
     * @return	true	If exist
     * 			false	else
     */
    public boolean memberExists(String name, String poolName);

    /**
     * Check if pool member with name 'name' and IP 'memberIP' exist in the pool 'poolName'
     * @param name	Name of the pool member
     * @param memberIP	IP of the pool member
     * @param poolName	Name of the pool member you want to search
     * @return	true	If Exist
     * 			false	else
     */
    public boolean memberExists(String name, String memberIP,String poolName);
    
    /**
     * Return all  pool members of the pool 'poolName'
     * @param poolName	Name of the pool
     * @return	Set of all the pool members		if pool with the name present in the configuration
     * 			null							else
     * 			
     */
    public Set<PoolMember> getAllPoolMembers(String poolName);
    
    /**
     * Add new pool member to the configuration
     * @param name	Name of the pool
     * @param memberIP	IP of the pool
     * @param poolName	Attach pool member to this pool
     * @return	Newly created pool member 
     */
    public PoolMember addPoolMember(String name, String memberIP, String poolName);
    
    /**
     * Remove pool member from the pool
     * @param name	Name of the pool member
     * @param poolName	Name of the pool
     * @return	Details of the removed pool member 
     */
    public PoolMember removePoolMember(String name, String poolName);

    /**
     * Return all the existing pools
     * @return	Set of Pools
     */
    public Set<Pool> getAllPools();
    
    /**
     * Return pool with input name
     * @param poolName	Name of the pool
     * @return	Details of the pool 	if pool exist
     * 			null					else
     */
    public Pool getPool(String poolName);
    
    /**
     * Check if pool exists with the input name 
     * @param name	Name of the pool
     * @return	true	If exists
     * 			false	else
     */
    public boolean poolExists(String name);

    /**
     * Check if pool exists with the input name and loadbalancing method.
     * @param name	Name of the pool 
     * @param lbMethod	Load balancing method name
     * @return	true	If exists
     * 			false	else
     */
    public boolean poolExists(String name, String lbMethod);
    
    /**
     * Create new pool with the provided details
     * @param name	Name of the pool
     * @param lbMethod	Load balancing method this pool will use
     * @return	Details of the newly created pool
     */
    public Pool createPool(String name, String lbMethod);
    
    /**
     * Delete pool with the provided name
     * @param poolName	Name of the pool
     * @return	Details of the deleted pool
     */
    public Pool deletePool(String poolName);

}