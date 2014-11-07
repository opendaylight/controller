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
 * default_pool_id    String
 * tenant_id          uuid-str
 * name               String
 * description        String
 * shared             Bool
 * protocol           String
 * protocol_port      String
 * load_balancer_id   String
 * admin_state_up     Boolean
 * status             String
 *
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerListener implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name="id")
    String loadBalancerListenerID;

    @XmlElement (name="default_pool_id")
    String neutronLoadBalancerListenerDefaultPoolID;

    @XmlElement (name="tenant_id")
    String loadBalancerListenerTenantID;

    @XmlElement (name="name")
    String loadBalancerListenerName;

    @XmlElement (name="description")
    String loadBalancerListenerDescription;

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean loadBalancerListenerAdminStateIsUp;

    @XmlElement (name="status")
    String loadBalancerListenerStatus;

    @XmlElement (defaultValue="false", name="shared")
    Boolean loadBalancerListenerIsShared;

    @XmlElement (name="protocol")
    String neutronLoadBalancerListenerProtocol;

    @XmlElement (name="protocol_port")
    String neutronLoadBalancerListenerProtocolPort;

    @XmlElement (name="load_balancer_id")
    String neutronLoadBalancerListenerLoadBalancerID;


    public String getLoadBalancerListenerID() {
        return loadBalancerListenerID;
    }

    public void setLoadBalancerListenerID(String loadBalancerListenerID) {
        this.loadBalancerListenerID = loadBalancerListenerID;
    }

    public String getLoadBalancerListenerTenantID() {
        return loadBalancerListenerTenantID;
    }

    public void setLoadBalancerListenerTenantID(String loadBalancerListenerTenantID) {
        this.loadBalancerListenerTenantID = loadBalancerListenerTenantID;
    }

    public String getLoadBalancerListenerName() {
        return loadBalancerListenerName;
    }

    public void setLoadBalancerListenerName(String loadBalancerListenerName) {
        this.loadBalancerListenerName = loadBalancerListenerName;
    }

    public String getLoadBalancerListenerDescription() {
        return loadBalancerListenerDescription;
    }

    public void setLoadBalancerListenerDescription(String loadBalancerListenerDescription) {
        this.loadBalancerListenerDescription = loadBalancerListenerDescription;
    }

    public Boolean getLoadBalancerListenerAdminStateIsUp() {
        return loadBalancerListenerAdminStateIsUp;
    }

    public void setLoadBalancerListenerAdminStateIsUp(Boolean loadBalancerListenerAdminStateIsUp) {
        this.loadBalancerListenerAdminStateIsUp = loadBalancerListenerAdminStateIsUp;
    }

    public String getLoadBalancerListenerStatus() {
        return loadBalancerListenerStatus;
    }

    public void setLoadBalancerListenerStatus(String loadBalancerListenerStatus) {
        this.loadBalancerListenerStatus = loadBalancerListenerStatus;
    }

    public Boolean getLoadBalancerListenerIsShared() {
        return loadBalancerListenerIsShared;
    }

    public void setLoadBalancerListenerIsShared(Boolean loadBalancerListenerIsShared) {
        this.loadBalancerListenerIsShared = loadBalancerListenerIsShared;
    }

    public String getNeutronLoadBalancerListenerProtocol() {
        return neutronLoadBalancerListenerProtocol;
    }

    public void setNeutronLoadBalancerListenerProtocol(String neutronLoadBalancerListenerProtocol) {
        this.neutronLoadBalancerListenerProtocol = neutronLoadBalancerListenerProtocol;
    }

    public String getNeutronLoadBalancerListenerProtocolPort() {
        return neutronLoadBalancerListenerProtocolPort;
    }

    public void setNeutronLoadBalancerListenerProtocolPort(String neutronLoadBalancerListenerProtocolPort) {
        this.neutronLoadBalancerListenerProtocolPort = neutronLoadBalancerListenerProtocolPort;
    }

    public String getNeutronLoadBalancerListenerDefaultPoolID() {
        return neutronLoadBalancerListenerDefaultPoolID;
    }

    public void setNeutronLoadBalancerListenerDefaultPoolID(String neutronLoadBalancerListenerDefaultPoolID) {
        this.neutronLoadBalancerListenerDefaultPoolID = neutronLoadBalancerListenerDefaultPoolID;
    }

    public String getNeutronLoadBalancerListenerLoadBalancerID() {
        return neutronLoadBalancerListenerLoadBalancerID;
    }

    public void setNeutronLoadBalancerListenerLoadBalancerID(String neutronLoadBalancerListenerLoadBalancerID) {
        this.neutronLoadBalancerListenerLoadBalancerID = neutronLoadBalancerListenerLoadBalancerID;
    }

    public NeutronLoadBalancerListener extractFields(List<String> fields) {
        NeutronLoadBalancerListener ans = new NeutronLoadBalancerListener();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setLoadBalancerListenerID(this.getLoadBalancerListenerID());
            }
            if(s.equals("default_pool_id")) {
                ans.setNeutronLoadBalancerListenerDefaultPoolID(this.getNeutronLoadBalancerListenerDefaultPoolID());
            }
            if (s.equals("tenant_id")) {
                ans.setLoadBalancerListenerTenantID(this.getLoadBalancerListenerTenantID());
            }
            if (s.equals("name")) {
                ans.setLoadBalancerListenerName(this.getLoadBalancerListenerName());
            }
            if(s.equals("description")) {
                ans.setLoadBalancerListenerDescription(this.getLoadBalancerListenerDescription());
            }
            if (s.equals("shared")) {
                ans.setLoadBalancerListenerIsShared(loadBalancerListenerIsShared);
            }
            if (s.equals("protocol")) {
                ans.setNeutronLoadBalancerListenerProtocol(this.getNeutronLoadBalancerListenerProtocol());
            }
            if (s.equals("protocol_port")) {
                ans.setNeutronLoadBalancerListenerProtocolPort(this.getNeutronLoadBalancerListenerProtocolPort());
            }
            if (s.equals("load_balancer_id")) {
                ans.setNeutronLoadBalancerListenerLoadBalancerID(this.getNeutronLoadBalancerListenerLoadBalancerID());
            }
            if (s.equals("admin_state_up")) {
                ans.setLoadBalancerListenerAdminStateIsUp(loadBalancerListenerAdminStateIsUp);
            }
            if (s.equals("status")) {
                ans.setLoadBalancerListenerStatus(this.getLoadBalancerListenerStatus());
            }
        }
        return ans;
    }

    @Override public String toString() {
        return "NeutronLoadBalancerListener{" +
                "loadBalancerListenerID='" + loadBalancerListenerID + '\'' +
                ", neutronLoadBalancerListenerDefaultPoolID='" + neutronLoadBalancerListenerDefaultPoolID + '\'' +
                ", loadBalancerListenerTenantID='" + loadBalancerListenerTenantID + '\'' +
                ", loadBalancerListenerName='" + loadBalancerListenerName + '\'' +
                ", loadBalancerListenerDescription='" + loadBalancerListenerDescription + '\'' +
                ", loadBalancerListenerAdminStateIsUp=" + loadBalancerListenerAdminStateIsUp +
                ", loadBalancerListenerStatus='" + loadBalancerListenerStatus + '\'' +
                ", loadBalancerListenerIsShared=" + loadBalancerListenerIsShared +
                ", neutronLoadBalancerListenerProtocol='" + neutronLoadBalancerListenerProtocol + '\'' +
                ", neutronLoadBalancerListenerProtocolPort='" + neutronLoadBalancerListenerProtocolPort + '\'' +
                ", neutronLoadBalancerListenerLoadBalancerID='" + neutronLoadBalancerListenerLoadBalancerID + '\'' +
                '}';
    }
}