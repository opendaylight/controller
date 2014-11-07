/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * OpenStack Neutron v2.0 Load Balancer as a service
 * (LBaaS) bindings. See OpenStack Network API
 * v2.0 Reference for description of  the fields:
 * Implemented fields are as follows:
 *
 * id                 uuid-str
 * tenant_id          uuid-str
 * name               String
 * description        String
 * status             String
 * vip_address        IP address
 * vip_subnet         uuid-str
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancer implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name="id")
    String loadBalancerID;

    @XmlElement (name="tenant_id")
    String loadBalancerTenantID;

    @XmlElement (name="name")
    String loadBalancerName;

    @XmlElement (name="description")
    String loadBalancerDescription;

    @XmlElement (name="status")
    String loadBalancerStatus;

    @XmlElement (name="vip_address")
    String loadBalancerVipAddress;

    @XmlElement (name="vip_subnet_id")
    String loadBalancerVipSubnetID;

    public String getLoadBalancerID() {
        return loadBalancerID;
    }

    public void setLoadBalancerID(String loadBalancerID) {
        this.loadBalancerID = loadBalancerID;
    }

    public String getLoadBalancerTenantID() {
        return loadBalancerTenantID;
    }

    public void setLoadBalancerTenantID(String loadBalancerTenantID) {
        this.loadBalancerTenantID = loadBalancerTenantID;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    public String getLoadBalancerDescription() {
        return loadBalancerDescription;
    }

    public void setLoadBalancerDescription(String loadBalancerDescription) {
        this.loadBalancerDescription = loadBalancerDescription;
    }

    public String getLoadBalancerStatus() {
        return loadBalancerStatus;
    }

    public void setLoadBalancerStatus(String loadBalancerStatus) {
        this.loadBalancerStatus = loadBalancerStatus;
    }

    public String getLoadBalancerVipAddress() {
        return loadBalancerVipAddress;
    }

    public void setLoadBalancerVipAddress(String loadBalancerVipAddress) {
        this.loadBalancerVipAddress = loadBalancerVipAddress;
    }

    public String getLoadBalancerVipSubnetID() {
        return loadBalancerVipSubnetID;
    }

    public void setLoadBalancerVipSubnetID(String loadBalancerVipSubnetID) {
        this.loadBalancerVipSubnetID = loadBalancerVipSubnetID;
    }

    public NeutronLoadBalancer extractFields(List<String> fields) {
        NeutronLoadBalancer ans = new NeutronLoadBalancer();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setLoadBalancerID(this.getLoadBalancerID());
            }
            if (s.equals("tenant_id")) {
                ans.setLoadBalancerTenantID(this.getLoadBalancerTenantID());
            }
            if (s.equals("name")) {
                ans.setLoadBalancerName(this.getLoadBalancerName());
            }
            if(s.equals("description")) {
                ans.setLoadBalancerDescription(this.getLoadBalancerDescription());
            }
            if (s.equals("vip_address")) {
                ans.setLoadBalancerVipAddress(this.getLoadBalancerVipAddress());
            }
            if (s.equals("vip_subnet_id")) {
                ans.setLoadBalancerVipSubnetID(this.getLoadBalancerVipSubnetID());
            }
            if (s.equals("status")) {
                ans.setLoadBalancerStatus(this.getLoadBalancerStatus());
            }
        }
        return ans;
    }

    @Override public String toString() {
        return "NeutronLoadBalancer{" +
                "loadBalancerID='" + loadBalancerID + '\'' +
                ", loadBalancerTenantID='" + loadBalancerTenantID + '\'' +
                ", loadBalancerName='" + loadBalancerName + '\'' +
                ", loadBalancerDescription='" + loadBalancerDescription + '\'' +
                ", loadBalancerStatus='" + loadBalancerStatus + '\'' +
                ", loadBalancerVipAddress='" + loadBalancerVipAddress + '\'' +
                ", loadBalancerVipSubnetID='" + loadBalancerVipSubnetID + '\'' +
                '}';
    }
}