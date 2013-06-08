/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.entities;

/**
 * This class represents the source host that sends any traffic to any existing virtual IP (VIP).
 * This source host is referred to as a 'Client'. Clients will be differentiated from each other
 * based on the following three properties:
 * 1. IP address of the client.
 * 2. Protocol of the traffic it is using for sending traffic
 * 3. Source port from which it is sending traffic.
 * e.g TCP traffic from two different ports from the same host to a given VIP will be considered
 * as two different clients by this service. Similarly, traffic using two different protocols
 * (TCP, UDP) from the same host will be considered as two different clients.
 * 
 */
public class Client {
    
    /*
     * IP address of the client (source address)
     */
    private String ip;
    
    /*
     * Network protocol of the traffic sent by client
     */
    private String protocol;
    
    /*
     * Port used to send network traffic (source port)
     */
    private short port;
    
    public Client(String ip, String protocol, short port){
        this.ip = ip;
        this.protocol = protocol;
        this.port = port;
    }
    
    /**
     * @return the client IP
     */
    public String getIp() {
        return ip;
    }
    
    /**
     * @param ip the IP to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    /**
     * @return the client network protocol
     */
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    /**
     * @return the client port
     */
    public short getPort() {
        return port;
    }
    
    /**
     * @param port the port to set
     */
    public void setPort(short port) {
        this.port = port;
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
        result = prime * result+ ((protocol == null) ? 0 : protocol.hashCode());
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
        if (!(obj instanceof Client)) {
            return false;
        }
        Client other = (Client) obj;
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
        }else if (!protocol.equals(other.protocol)) {
            return false;
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Client [ip=" + ip + ", protocol=" + protocol + ", port=" + port+ "]";
    }
}
