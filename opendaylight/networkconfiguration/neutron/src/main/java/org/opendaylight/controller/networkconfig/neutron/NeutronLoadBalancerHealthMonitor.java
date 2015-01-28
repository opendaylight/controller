/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *
 * id                 uuid-str
 * tenant_id          uuid-str
 * healthmonitor_type String
 * delay              Integer
 * timeout            Integer
 * max_retries        Integer
 * http_method        String
 * url_path           String
 * expected_codes     String
 * admin_state_up     Boolean
 * status             String
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerHealthMonitor implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancer.class);

    @XmlElement(name="id")
    String loadBalancerHealthMonitorID;

    @XmlElement (name="tenant_id")
    String loadBalancerHealthMonitorTenantID;

    @XmlElement (name="healthmonitor_type")
    String loadBalancerHealthMonitorType;

    @XmlElement (name="delay")
    Integer loadBalancerHealthMonitorDelay;

    @XmlElement (name="timeout")
    Integer loadBalancerHealthMonitorTimeout;

    @XmlElement (name="max_retries")
    Integer loadBalancerHealthMonitorMaxRetries;

    @XmlElement (name="http_method")
    String loadBalancerHealthMonitorHttpMethod;

    @XmlElement (name="url_path")
    String loadBalancerHealthMonitorUrlPath;

    @XmlElement (name="expected_codes")
    String loadBalancerHealthMonitorExpectedCodes;

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean loadBalancerHealthMonitorAdminStateIsUp;

    @XmlElement (name="status")
    String loadBalancerHealthMonitorStatus;

    public String getLoadBalancerHealthMonitorID() {
        return loadBalancerHealthMonitorID;
    }

    public void setLoadBalancerHealthMonitorID(String loadBalancerHealthMonitorID) {
        this.loadBalancerHealthMonitorID = loadBalancerHealthMonitorID;
    }

    public String getLoadBalancerHealthMonitorTenantID() {
        return loadBalancerHealthMonitorTenantID;
    }

    public void setLoadBalancerHealthMonitorTenantID(String loadBalancerHealthMonitorTenantID) {
        this.loadBalancerHealthMonitorTenantID = loadBalancerHealthMonitorTenantID;
    }

    public String getLoadBalancerHealthMonitorType() {
        return loadBalancerHealthMonitorType;
    }

    public void setLoadBalancerHealthMonitorType(String loadBalancerHealthMonitorType) {
        this.loadBalancerHealthMonitorType = loadBalancerHealthMonitorType;
    }

    public Integer getLoadBalancerHealthMonitorDelay() {
        return loadBalancerHealthMonitorDelay;
    }

    public void setLoadBalancerHealthMonitorDelay(Integer loadBalancerHealthMonitorDelay) {
        this.loadBalancerHealthMonitorDelay = loadBalancerHealthMonitorDelay;
    }

    public Integer getLoadBalancerHealthMonitorTimeout() {
        return loadBalancerHealthMonitorTimeout;
    }

    public void setLoadBalancerHealthMonitorTimeout(Integer loadBalancerHealthMonitorTimeout) {
        this.loadBalancerHealthMonitorTimeout = loadBalancerHealthMonitorTimeout;
    }

    public Integer getLoadBalancerHealthMonitorMaxRetries() {
        return loadBalancerHealthMonitorMaxRetries;
    }

    public void setLoadBalancerHealthMonitorMaxRetries(Integer loadBalancerHealthMonitorMaxRetries) {
        this.loadBalancerHealthMonitorMaxRetries = loadBalancerHealthMonitorMaxRetries;
    }

    public String getLoadBalancerHealthMonitorHttpMethod() {
        return loadBalancerHealthMonitorHttpMethod;
    }

    public void setLoadBalancerHealthMonitorHttpMethod(String loadBalancerHealthMonitorHttpMethod) {
        this.loadBalancerHealthMonitorHttpMethod = loadBalancerHealthMonitorHttpMethod;
    }

    public String getLoadBalancerHealthMonitorUrlPath() {
        return loadBalancerHealthMonitorUrlPath;
    }

    public void setLoadBalancerHealthMonitorUrlPath(String loadBalancerHealthMonitorUrlPath) {
        this.loadBalancerHealthMonitorUrlPath = loadBalancerHealthMonitorUrlPath;
    }

    public String getLoadBalancerHealthMonitorExpectedCodes() {
        return loadBalancerHealthMonitorExpectedCodes;
    }

    public void setLoadBalancerHealthMonitorExpectedCodes(String loadBalancerHealthMonitorExpectedCodes) {
        this.loadBalancerHealthMonitorExpectedCodes = loadBalancerHealthMonitorExpectedCodes;
    }

    public Boolean getLoadBalancerHealthMonitorAdminStateIsUp() {
        return loadBalancerHealthMonitorAdminStateIsUp;
    }

    public void setLoadBalancerHealthMonitorAdminStateIsUp(Boolean loadBalancerHealthMonitorAdminStateIsUp) {
        this.loadBalancerHealthMonitorAdminStateIsUp = loadBalancerHealthMonitorAdminStateIsUp;
    }

    public String getLoadBalancerHealthMonitorStatus() {
        return loadBalancerHealthMonitorStatus;
    }

    public void setLoadBalancerHealthMonitorStatus(String loadBalancerHealthMonitorStatus) {
        this.loadBalancerHealthMonitorStatus = loadBalancerHealthMonitorStatus;
    }

    public NeutronLoadBalancerHealthMonitor extractFields(List<String> fields) {
        NeutronLoadBalancerHealthMonitor ans = new NeutronLoadBalancerHealthMonitor();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setLoadBalancerHealthMonitorID(this.getLoadBalancerHealthMonitorID());
            }
            if (s.equals("tenant_id")) {
                ans.setLoadBalancerHealthMonitorTenantID(this.getLoadBalancerHealthMonitorTenantID());
            }
            if (s.equals("healthmonitor_type")) {
                ans.setLoadBalancerHealthMonitorType(this.getLoadBalancerHealthMonitorType());
            }
            if (s.equals("delay")) {
                ans.setLoadBalancerHealthMonitorDelay(this.getLoadBalancerHealthMonitorDelay());
            }
            if (s.equals("timeout")) {
                ans.setLoadBalancerHealthMonitorTimeout(this.getLoadBalancerHealthMonitorTimeout());
            }
            if (s.equals("max_retries")) {
                ans.setLoadBalancerHealthMonitorMaxRetries(this.getLoadBalancerHealthMonitorMaxRetries());
            }
            if (s.equals("http_method")) {
                ans.setLoadBalancerHealthMonitorHttpMethod(this.getLoadBalancerHealthMonitorHttpMethod());
            }
            if(s.equals("url_path")) {
                ans.setLoadBalancerHealthMonitorUrlPath(this.getLoadBalancerHealthMonitorUrlPath());
            }
            if (s.equals("expected_codes")) {
                ans.setLoadBalancerHealthMonitorExpectedCodes(this.getLoadBalancerHealthMonitorExpectedCodes());
            }
            if (s.equals("admin_state_up")) {
                ans.setLoadBalancerHealthMonitorAdminStateIsUp(loadBalancerHealthMonitorAdminStateIsUp);
            }
            if (s.equals("status")) {
                ans.setLoadBalancerHealthMonitorStatus(this.getLoadBalancerHealthMonitorStatus());
            }
        }
        return ans;
    }

    @Override public String toString() {
        return "NeutronLoadBalancerHealthMonitor{" +
                "loadBalancerHealthMonitorID='" + loadBalancerHealthMonitorID + '\'' +
                ", loadBalancerHealthMonitorTenantID='" + loadBalancerHealthMonitorTenantID + '\'' +
                ", loadBalancerHealthMonitorType='" + loadBalancerHealthMonitorType + '\'' +
                ", loadBalancerHealthMonitorDelay=" + loadBalancerHealthMonitorDelay +
                ", loadBalancerHealthMonitorTimeout=" + loadBalancerHealthMonitorTimeout +
                ", loadBalancerHealthMonitorMaxRetries=" + loadBalancerHealthMonitorMaxRetries +
                ", loadBalancerHealthMonitorHttpMethod='" + loadBalancerHealthMonitorHttpMethod + '\'' +
                ", loadBalancerHealthMonitorUrlPath='" + loadBalancerHealthMonitorUrlPath + '\'' +
                ", loadBalancerHealthMonitorExpectedCodes='" + loadBalancerHealthMonitorExpectedCodes + '\'' +
                ", loadBalancerHealthMonitorAdminStateIsUp=" + loadBalancerHealthMonitorAdminStateIsUp +
                ", loadBalancerHealthMonitorStatus='" + loadBalancerHealthMonitorStatus + '\'' +
                '}';
    }
}
