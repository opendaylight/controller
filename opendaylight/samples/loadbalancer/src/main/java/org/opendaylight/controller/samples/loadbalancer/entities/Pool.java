/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.entities;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents the pool of hosts among which incoming traffic
 * will be load balanced. Each pool will load balance the traffic among its pool members 
 * based on the loadbalancing policy set for the pool. 
 * Currently, the pool supports two load balancing policies:
 * 1. Round Robin Policy{@link org.opendaylight.controller.samples.loadbalancer.policies.RoundRobinLBPolicy}
 * 2. Random Policy {@link org.opendaylight.controller.samples.loadbalancer.policies.RandomLBPolicy}
 * 
 * NOTE: After creation of the pool, user can't update (change) its load balancing policy.
 * NOTE: Each Pool should have a unique name.
 */

@XmlRootElement(name="pool")
@XmlAccessorType(XmlAccessType.NONE)
public class Pool {
    
    /*
     * Unique name of the pool
     */
    @XmlElement
    private String name;
    
    /*
     * Associated load balancing policy
     */
    @XmlElement(name="lbmethod")
    private String lbMethod;
    
    /*
     * Status of the pool (active/inactive)
     */
    @XmlElement
    private String status;
    
    /*
     * List of all the VIPs using this pool for load balancing their traffic - more than
     * one VIP can be mapped to each pool.
     */
    @XmlElement
    private ArrayList<VIP> vips = new ArrayList<VIP>();
    
    /*
     * List of all the pool members used for load balancing the traffic
     */
    @XmlElement
    private ArrayList<PoolMember> members = new ArrayList<PoolMember>();
    
    /*
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private Pool() {}
    
    /**
     * Getter/ Setter methods
     */
    
    public Pool(String name,
                    String lbMethod) {
        this.name = name;
        this.lbMethod = lbMethod;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the lbMethod
     */
    public String getLbMethod() {
        return lbMethod;
    }
    
    /**
     * @param lbMethod the lbMethod to set
     */
    public void setLbMethod(String lbMethod) {
        this.lbMethod = lbMethod;
    }
    
    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * @return the vip
     */
    public ArrayList<VIP> getAllVip() {
        return vips;
    }
    
    /**
     * @param vip the vip to set
     */
    public void setVips(ArrayList<VIP> vips) {
        this.vips = vips;
    }
    
    /**
     * @return the members
     */
    public ArrayList<PoolMember> getAllMembers() {
        return members;
    }
    
    /**
     * @param members the members to set
     */
    public void setMembers(ArrayList<PoolMember> members) {
        this.members = members;
    }
    
    /**
     * Add new VIP to the VIP list
     * @param vip       new VIP to add
     */
    public void addVIP(VIP vip){
        this.vips.add(vip);
    }
    
    /**
     * Remove VIP with given name from the VIP list of the pool
     * @param name      Name of the VIP
     * @return	true     If VIP was using this pool and removed
     *          false   IF VIP is not using this pool
     */
    public boolean removeVIP(String name){
        for(VIP vip: this.vips){
            if(vip.getName().equals(name)){
                this.members.remove(vip);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the given pool member is part of this pool
     * @param pm        Search for this pool member
     * @return	true     If pool member is attached to this pool
     *          false   else
     */
    public boolean poolMemberExists(PoolMember pm){
        return this.members.contains(pm);
    }
    
    /**
     * Returns the pool member with the given name
     * @param name      Search for this pool member
     * @return	PoolMember       If pool member is attached to this pool
     *          null            else
     */
    public PoolMember getMember(String name){
        
        for(PoolMember pm: this.members){
            if(pm.getName().equals(name)){
                return pm;
            }
        }
        return null;
    }
    
    /**
     * Add new pool member to the pool
     * @param pm        Add this new pool
     */
    public void addMember(PoolMember pm){
        this.members.add(pm);
    }
    
    /**
     * Remove pool member from the pool list
     * @param name Remove this pool member
     * @return	true	If pool member was attached to this pool and successfully removed
     *          false	If pool member is not attached to this pool
     */
    public boolean removeMember(String name){
        for(PoolMember pm: this.members){
            if(pm.getName().equals(name)){
                this.members.remove(pm);
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result+ ((lbMethod == null) ? 0 : lbMethod.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Pool)) {
            return false;
        }
        Pool other = (Pool) obj;
        if (lbMethod == null) {
            if (other.lbMethod != null) {
                return false;
            }
        }else if (!lbMethod.equals(other.lbMethod)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Pool [name=" + name + ", lbMethod=" + lbMethod + ", status="
                            + status + ", vips=" + vips + ", members=" + members + "]";
    }
}