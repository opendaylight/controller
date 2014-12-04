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
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerPoolMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * TODO: Plumb into LBaaS Pool. Members are nested underneath Pool CRUD.
     */
    @XmlElement (name="id")
    String poolMemberID;

    @XmlElement (name="tenant_id")
    String poolMemberTenantID;

    @XmlElement (name="address")
    String poolMemberAddress;

    @XmlElement (name="protocol_port")
    Integer poolMemberProtoPort;

    @XmlElement (name="admin_state_up")
    Boolean poolMemberAdminStateIsUp;

    @XmlElement (name="weight")
    Integer poolMemberWeight;

    @XmlElement (name="subnet_id")
    String poolMemberSubnetID;

    @XmlElement (name="status")
    String poolMemberStatus;

    String poolID;

    public NeutronLoadBalancerPoolMember() {
    }

    @XmlTransient
    public String getPoolID() {
        return poolID;
    }

    public void setPoolID(String poolID) {
        this.poolID = poolID;
    }

    public String getPoolMemberID() {
        return poolMemberID;
    }

    public void setPoolMemberID(String poolMemberID) {
        this.poolMemberID = poolMemberID;
    }

    public String getPoolMemberTenantID() {
        return poolMemberTenantID;
    }

    public void setPoolMemberTenantID(String poolMemberTenantID) {
        this.poolMemberTenantID = poolMemberTenantID;
    }

    public String getPoolMemberAddress() {
        return poolMemberAddress;
    }

    public void setPoolMemberAddress(String poolMemberAddress) {
        this.poolMemberAddress = poolMemberAddress;
    }

    public Integer getPoolMemberProtoPort() {
        return poolMemberProtoPort;
    }

    public void setPoolMemberProtoPort(Integer poolMemberProtoPort) {
        this.poolMemberProtoPort = poolMemberProtoPort;
    }

    public Boolean getPoolMemberAdminStateIsUp() {
        return poolMemberAdminStateIsUp;
    }

    public void setPoolMemberAdminStateIsUp(Boolean poolMemberAdminStateIsUp) {
        this.poolMemberAdminStateIsUp = poolMemberAdminStateIsUp;
    }

    public Integer getPoolMemberWeight() {
        return poolMemberWeight;
    }

    public void setPoolMemberWeight(Integer poolMemberWeight) {
        this.poolMemberWeight = poolMemberWeight;
    }

    public String getPoolMemberSubnetID() {
        return poolMemberSubnetID;
    }

    public void setPoolMemberSubnetID(String poolMemberSubnetID) {
        this.poolMemberSubnetID = poolMemberSubnetID;
    }

    public String getPoolMemberStatus() {
        return poolMemberStatus;
    }

    public void setPoolMemberStatus(String poolMemberStatus) {
        this.poolMemberStatus = poolMemberStatus;
    }

    public NeutronLoadBalancerPoolMember extractFields(List<String> fields) {
        NeutronLoadBalancerPoolMember ans = new NeutronLoadBalancerPoolMember();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setPoolMemberID(this.getPoolMemberID());
            }
            if (s.equals("pool_id")) {
                ans.setPoolID(this.getPoolID());
            }
            if (s.equals("tenant_id")) {
                ans.setPoolMemberTenantID(this.getPoolMemberTenantID());
            }
            if (s.equals("address")) {
                ans.setPoolMemberAddress(this.getPoolMemberAddress());
            }
            if(s.equals("protocol_port")) {
                ans.setPoolMemberProtoPort(this.getPoolMemberProtoPort());
            }
            if (s.equals("admin_state_up")) {
                ans.setPoolMemberAdminStateIsUp(poolMemberAdminStateIsUp);
            }
            if(s.equals("weight")) {
                ans.setPoolMemberWeight(this.getPoolMemberWeight());
            }
            if(s.equals("subnet_id")) {
                ans.setPoolMemberSubnetID(this.getPoolMemberSubnetID());
            }
            if (s.equals("status")) {
                ans.setPoolMemberStatus(this.getPoolMemberStatus());
            }
        }
        return ans;
    }
    @Override public String toString() {
        return "NeutronLoadBalancerPoolMember{" +
                "poolMemberID='" + poolMemberID + '\'' +
                ", poolID='" + poolID + '\'' +
                ", poolMemberTenantID='" + poolMemberTenantID + '\'' +
                ", poolMemberAddress='" + poolMemberAddress + '\'' +
                ", poolMemberProtoPort=" + poolMemberProtoPort +
                ", poolMemberAdminStateIsUp=" + poolMemberAdminStateIsUp +
                ", poolMemberWeight=" + poolMemberWeight +
                ", poolMemberSubnetID='" + poolMemberSubnetID + '\'' +
                ", poolMemberStatus='" + poolMemberStatus + '\'' +
                '}';
    }
}
