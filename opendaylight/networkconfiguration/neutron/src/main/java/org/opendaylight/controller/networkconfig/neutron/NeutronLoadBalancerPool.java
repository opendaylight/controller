/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import org.opendaylight.controller.configuration.ConfigurationObject;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
//import javax.xml.bind.annotation.XmlElementWrapper;
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
 * protocol           String
 * lb_algorithm       String
 * healthmonitor_id   String
 * admin_state_up     Bool
 * status             String
 * members            List <NeutronLoadBalancerPoolMember>
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerPool extends ConfigurationObject implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name="id")
    String loadBalancerPoolID;

    @XmlElement (name="tenant_id")
    String loadBalancerPoolTenantID;

    @XmlElement (name="name")
    String loadBalancerPoolName;

    @XmlElement (name="description")
    String loadBalancerPoolDescription;

    @XmlElement (name="protocol")
    String loadBalancerPoolProtocol;

    @XmlElement (name="lb_algorithm")
    String loadBalancerPoolLbAlgorithm;

    @XmlElement (name="healthmonitor_id")
    String neutronLoadBalancerPoolHealthMonitorID;

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean loadBalancerPoolAdminStateIsUp;

    @XmlElement (name="status")
    String loadBalancerPoolStatus;

    @XmlElement(name="members")
    List<NeutronLoadBalancerPoolMember> loadBalancerPoolMembers;

    public NeutronLoadBalancerPool() {
    }

    public String getLoadBalancerPoolID() {
        return loadBalancerPoolID;
    }

    public void setLoadBalancerPoolID(String loadBalancerPoolID) {
        this.loadBalancerPoolID = loadBalancerPoolID;
    }

    public String getLoadBalancerPoolTenantID() {
        return loadBalancerPoolTenantID;
    }

    public void setLoadBalancerPoolTenantID(String loadBalancerPoolTenantID) {
        this.loadBalancerPoolTenantID = loadBalancerPoolTenantID;
    }

    public String getLoadBalancerPoolName() {
        return loadBalancerPoolName;
    }

    public void setLoadBalancerPoolName(String loadBalancerPoolName) {
        this.loadBalancerPoolName = loadBalancerPoolName;
    }

    public String getLoadBalancerPoolDescription() {
        return loadBalancerPoolDescription;
    }

    public void setLoadBalancerPoolDescription(String loadBalancerPoolDescription) {
        this.loadBalancerPoolDescription = loadBalancerPoolDescription;
    }

    public String getLoadBalancerPoolProtocol() {
        return loadBalancerPoolProtocol;
    }

    public void setLoadBalancerPoolProtocol(String loadBalancerPoolProtocol) {
        this.loadBalancerPoolProtocol = loadBalancerPoolProtocol;
    }

    public String getLoadBalancerPoolLbAlgorithm() {
        return loadBalancerPoolLbAlgorithm;
    }

    public void setLoadBalancerPoolLbAlgorithm(String loadBalancerPoolLbAlgorithm) {
        this.loadBalancerPoolLbAlgorithm = loadBalancerPoolLbAlgorithm;
    }

    public String getNeutronLoadBalancerPoolHealthMonitorID() {
        return neutronLoadBalancerPoolHealthMonitorID;
    }

    public void setNeutronLoadBalancerPoolHealthMonitorID(String neutronLoadBalancerPoolHealthMonitorID) {
        this.neutronLoadBalancerPoolHealthMonitorID = neutronLoadBalancerPoolHealthMonitorID;
    }

    public Boolean getLoadBalancerPoolAdminIsStateIsUp() {
        return loadBalancerPoolAdminStateIsUp;
    }

    public void setLoadBalancerPoolAdminStateIsUp(Boolean loadBalancerPoolAdminStateIsUp) {
        this.loadBalancerPoolAdminStateIsUp = loadBalancerPoolAdminStateIsUp;
    }

    public String getLoadBalancerPoolStatus() {
        return loadBalancerPoolStatus;
    }

    public void setLoadBalancerPoolStatus(String loadBalancerPoolStatus) {
        this.loadBalancerPoolStatus = loadBalancerPoolStatus;
    }

    public List<NeutronLoadBalancerPoolMember> getLoadBalancerPoolMembers() {
        /*
         * Update the pool_id of the member to that this.loadBalancerPoolID
         */
        if (loadBalancerPoolMembers!=null) {
            for (NeutronLoadBalancerPoolMember member: loadBalancerPoolMembers)
                member.setPoolID(loadBalancerPoolID);
            return loadBalancerPoolMembers;
            }
        return loadBalancerPoolMembers;
    }

    public void setLoadBalancerPoolMembers(List<NeutronLoadBalancerPoolMember> loadBalancerPoolMembers) {
        this.loadBalancerPoolMembers = loadBalancerPoolMembers;
    }

    public void addLoadBalancerPoolMember(NeutronLoadBalancerPoolMember loadBalancerPoolMember) {
        this.loadBalancerPoolMembers.add(loadBalancerPoolMember);
    }

    public void removeLoadBalancerPoolMember(NeutronLoadBalancerPoolMember loadBalancerPoolMember) {
        this.loadBalancerPoolMembers.remove(loadBalancerPoolMember);
    }

    public NeutronLoadBalancerPool extractFields(List<String> fields) {
        NeutronLoadBalancerPool ans = new NeutronLoadBalancerPool();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setLoadBalancerPoolID(this.getLoadBalancerPoolID());
            }
            if (s.equals("tenant_id")) {
                ans.setLoadBalancerPoolTenantID(this.getLoadBalancerPoolTenantID());
            }
            if (s.equals("name")) {
                ans.setLoadBalancerPoolName(this.getLoadBalancerPoolName());
            }
            if(s.equals("description")) {
                ans.setLoadBalancerPoolDescription(this.getLoadBalancerPoolDescription());
            }
            if(s.equals("protocol")) {
                ans.setLoadBalancerPoolProtocol(this.getLoadBalancerPoolProtocol());
            }
            if(s.equals("lb_algorithm")) {
                ans.setLoadBalancerPoolLbAlgorithm(this.getLoadBalancerPoolLbAlgorithm());
            }
            if(s.equals("healthmonitor_id")) {
                ans.setNeutronLoadBalancerPoolHealthMonitorID(this.getNeutronLoadBalancerPoolHealthMonitorID());
            }
            if (s.equals("admin_state_up")) {
                ans.setLoadBalancerPoolAdminStateIsUp(loadBalancerPoolAdminStateIsUp);
            }
            if (s.equals("status")) {
                ans.setLoadBalancerPoolStatus(this.getLoadBalancerPoolStatus());
            }
            if (s.equals("members")) {
                ans.setLoadBalancerPoolMembers(getLoadBalancerPoolMembers());
            }
        }
        return ans;
    }
}
