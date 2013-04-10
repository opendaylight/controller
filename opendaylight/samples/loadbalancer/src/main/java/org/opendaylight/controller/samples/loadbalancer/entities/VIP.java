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
 * This class represents the Virtual IP  (VIP) address exposed by the load balancer application.
 * Load balancer service differentiates one VIP from the other, using the following three properties:
 * 1. IP address of the VIP exposed by the application
 * 2. Protocol of the network traffic (TCP/UDP)
 * 3. Port to which incoming traffic is destined
 * 
 * User is allowed to create mutliple VIPs with the same IP, but all such VIPs (with the same IP) 
 * should differ at least in the protocol or port or both.
 * 
 * NOTE: Each VIP should have a unique name.
 */
@XmlRootElement(name="vip")
@XmlAccessorType(XmlAccessType.NONE)

public class VIP {
    
    /*
     * Unique name of the VIP
     */
    @XmlElement
    private String name;
    
    /*
     * Virtual IP address of the VIP 
     */
    @XmlElement
    private String ip;
    
    /*
     *	Network traffic protocol 
     */
    @XmlElement
    private String protocol;
    
    /*
     * Port where network traffic is destined (destination port)
     */
    @XmlElement
    private short port;
    
    /*
     * Name of the pool attached to the VIP for load balancing its traffic
     */
    @XmlElement(name="poolname")
    private String poolName;
    
    /*
     * Status (Active/inactive)
     */
    @XmlElement
    private String status;

    /**
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private VIP() {}
    
    public VIP(String name,
                String ip,
                String protocol,
                short port,
                String poolName){
        this.name = name;
        this.ip=ip;
        this.protocol=protocol;
        this.port = port;
        this.poolName = poolName;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public short getPort() {
        return port;
    }
    
    public void setPort(short port) {
        this.port = port;
    }
    
    public String getPoolName() {
        return poolName;
    }
    
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
        result = prime * result + port;
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
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
        if (!(obj instanceof VIP)) {
            return false;
        }
        
        VIP other = (VIP) obj;
        if (ip == null) {
            if (other.ip != null) {
                return false;
            }
        }else if (!ip.equals(other.ip)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        }else if (!protocol.equalsIgnoreCase(other.protocol)) {
            return false;
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "VIP [name=" + name + ", ip=" + ip + ", protocol=" + protocol
                            + ", port=" + port + ", poolName=" + poolName + ", status="
                            + status + "]";
    }
}