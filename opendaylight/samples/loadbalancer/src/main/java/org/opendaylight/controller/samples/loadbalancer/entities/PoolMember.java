/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.entities;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents the host where load balancing service will
 * redirect VIP traffic for load balancing. All these hosts have to
 * register with a pool to be a part of traffic load balancing.
 * This entity is referred to as a 'PoolMember'. 
 * Load balancer service differentiates each pool member based on its 
 * two properties { ip address, attached pool }.
 * A host (IP) can be attached to two different pools through creation of two
 * different pool member objects.
 * 
 * NOTE: Each pool member should have a unique name.
 *
 */
@XmlRootElement(name="poolmember")
@XmlAccessorType(XmlAccessType.NONE)
public class PoolMember {
    
    /*
     * Unique name of the pool member
     */
    @XmlElement
    private String name;
    
    /*
     * IP address of the pool member
     */
    @XmlElement
    private String ip;
    
    /*
     * Name of the pool this member is attached to.
     */
    @XmlElement(name="poolname")
    private String poolName;
    
    /*
     * Status (active/inactive)
     */
    @XmlElement
    private String status;
    
    /**
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private PoolMember() {}
    
    public PoolMember(String name, String memberIP, String poolName){
        this.name = name;
        this.ip = memberIP;
        this.poolName = poolName;
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
     * @return the ip
     */
    public String getIp() {
        return ip;
    }
    
    /**
     * @param ip the ip to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    /**
     * @return the poolName
     */
    public String getPoolName() {
        return poolName;
    }
    
    /**
     * @param poolName the poolName to set
     */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
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
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result
                + ((poolName == null) ? 0 : poolName.hashCode());
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
        if (!(obj instanceof PoolMember)) {
            return false;
        }
        PoolMember other = (PoolMember) obj;
        if (ip == null) {
            if (other.ip != null) {
                return false;
            }
        }else if (!ip.equals(other.ip)) {
            return false;
        }
        if (poolName == null) {
            if (other.poolName != null) {
                return false;
            }
        }else if (!poolName.equals(other.poolName)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PoolMember [name=" + name + ", ip=" + ip + ", poolName="
                                    + poolName + ", status=" + status + "]";
    }
}