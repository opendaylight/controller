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
 * OpenStack Neutron v2.0 Firewall as a service
 * (FWaaS) bindings. See OpenStack Network API
 * v2.0 Reference for description of  the fields.
 * The implemented fields are as follows:
 *
 * tenant_id               uuid-str
 * name                    String
 * description             String
 * admin_state_up          Bool
 * status                  String
 * shared                  Bool
 * firewall_policy_id      uuid-str
 * protocol                String
 * ip_version              Integer
 * source_ip_address       String (IP addr or CIDR)
 * destination_ip_address  String (IP addr or CIDR)
 * source_port             Integer
 * destination_port        Integer
 * position                Integer
 * action                  String
 * enabled                 Bool
 * id                      uuid-str
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronFirewallRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "id")
    String firewallRuleUUID;

    @XmlElement(name = "tenant_id")
    String firewallRuleTenantID;

    @XmlElement(name = "name")
    String firewallRuleName;

    @XmlElement(name = "description")
    String firewallRuleDescription;

    @XmlElement(defaultValue = "true", name = "admin_state_up")
    Boolean firewallRuleAdminStateIsUp;

    @XmlElement(name = "status")
    String firewallRuleStatus;

    @XmlElement(defaultValue = "false", name = "shared")
    Boolean firewallRuleIsShared;

    @XmlElement(name = "firewall_policy_id")
    String firewallRulePolicyID;

    @XmlElement(name = "protocol")
    String firewallRuleProtocol;

    @XmlElement(name = "ip_version")
    Integer firewallRuleIpVer;

    @XmlElement(name = "source_ip_address")
    String firewallRuleSrcIpAddr;

    @XmlElement(name = "destination_ip_address")
    String firewallRuleDstIpAddr;

    @XmlElement(name = "source_port")
    Integer firewallRuleSrcPort;

    @XmlElement(name = "destination_port")
    Integer firewallRuleDstPort;

    @XmlElement(name = "position")
    Integer firewallRulePosition;

    @XmlElement(name = "action")
    String firewallRuleAction;

    @XmlElement(name = "enabled")
    Boolean firewallRuleIsEnabled;

    public Boolean getFirewallRuleIsEnabled() {
        return firewallRuleIsEnabled;
    }

    public void setFirewallRuleIsEnabled(Boolean firewallRuleIsEnabled) {
        this.firewallRuleIsEnabled = firewallRuleIsEnabled;
    }

    public String getFirewallRuleAction() {
        return firewallRuleAction;
    }

    public void setFirewallRuleAction(String firewallRuleAction) {
        this.firewallRuleAction = firewallRuleAction;
    }

    public Integer getFirewallRulePosition() {
        return firewallRulePosition;
    }

    public void setFirewallRulePosition(Integer firewallRulePosition) {
        this.firewallRulePosition = firewallRulePosition;
    }

    public Integer getFirewallRuleDstPort() {
        return firewallRuleDstPort;
    }

    public void setFirewallRuleDstPort(Integer firewallRuleDstPort) {
        this.firewallRuleDstPort = firewallRuleDstPort;
    }

    public Integer getFirewallRuleSrcPort() {
        return firewallRuleSrcPort;
    }

    public void setFirewallRuleSrcPort(Integer firewallRuleSrcPort) {
        this.firewallRuleSrcPort = firewallRuleSrcPort;
    }

    public String getFirewallRuleDstIpAddr() {
        return firewallRuleDstIpAddr;
    }

    public void setFirewallRuleDstIpAddr(String firewallRuleDstIpAddr) {
        this.firewallRuleDstIpAddr = firewallRuleDstIpAddr;
    }

    public String getFirewallRuleSrcIpAddr() {
        return firewallRuleSrcIpAddr;
    }

    public void setFirewallRuleSrcIpAddr(String firewallRuleSrcIpAddr) {
        this.firewallRuleSrcIpAddr = firewallRuleSrcIpAddr;
    }

    public Integer getFirewallRuleIpVer() {
        return firewallRuleIpVer;
    }

    public void setFirewallRuleIpVer(Integer firewallRuleIpVer) {
        this.firewallRuleIpVer = firewallRuleIpVer;
    }

    public String getFirewallRuleProtocol() {
        return firewallRuleProtocol;
    }

    public void setFirewallRuleProtocol(String firewallRuleProtocol) {
        this.firewallRuleProtocol = firewallRuleProtocol;
    }

    public String getFirewallRulePolicyID() {
        return firewallRulePolicyID;
    }

    public void setFirewallRulesPolicyID(String firewallRulePolicyID) {
        this.firewallRulePolicyID = firewallRulePolicyID;
    }

    public Boolean getFirewallRuleIsShared() {
        return firewallRuleIsShared;
    }

    public void setFirewallRuleIsShared(Boolean firewallRuleIsShared) {
        this.firewallRuleIsShared = firewallRuleIsShared;
    }

    public String getFirewallRuleStatus() {
        return firewallRuleStatus;
    }

    public void setFirewallRuleStatus(String firewallRuleStatus) {
        this.firewallRuleStatus = firewallRuleStatus;
    }

    public Boolean getFirewallRuleAdminStateIsUp() {
        return firewallRuleAdminStateIsUp;
    }

    public void setFirewallRuleAdminStateIsUp(Boolean firewallRuleAdminStateIsUp) {
        this.firewallRuleAdminStateIsUp = firewallRuleAdminStateIsUp;
    }

    public String getFirewallRuleDescription() {
        return firewallRuleDescription;
    }

    public void setFirewallRuleDescription(String firewallRuleDescription) {
        this.firewallRuleDescription = firewallRuleDescription;
    }

    public String getFirewallRuleName() {
        return firewallRuleName;
    }

    public void setFirewallRuleName(String firewallRuleName) {
        this.firewallRuleName = firewallRuleName;
    }

    public String getFirewallRuleTenantID() {
        return firewallRuleTenantID;
    }

    public void setFirewallRuleTenantID(String firewallRuleTenantID) {
        this.firewallRuleTenantID = firewallRuleTenantID;
    }

    public String getFirewallRuleUUID() {
        return firewallRuleUUID;
    }

    public void setFirewallRuleUUID(String firewallRuleUUID) {
        this.firewallRuleUUID = firewallRuleUUID;
    }

    public NeutronFirewallRule extractFields(List<String> fields) {
        NeutronFirewallRule ans = new NeutronFirewallRule();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setFirewallRuleUUID(this.getFirewallRuleUUID());
            }
            if (s.equals("tenant_id")) {
                ans.setFirewallRuleTenantID(this.getFirewallRuleTenantID());
            }
            if (s.equals("name")) {
                ans.setFirewallRuleName(this.getFirewallRuleName());
            }
            if (s.equals("description")) {
                ans.setFirewallRuleDescription(this.getFirewallRuleDescription());
            }
            if (s.equals("admin_state_up")) {
                ans.setFirewallRuleAdminStateIsUp(firewallRuleAdminStateIsUp);
            }
            if (s.equals("status")) {
                ans.setFirewallRuleStatus(this.getFirewallRuleStatus());
            }
            if (s.equals("shared")) {
                ans.setFirewallRuleIsShared(firewallRuleIsShared);
            }
            if (s.equals("firewall_policy_id")) {
                ans.setFirewallRulesPolicyID(this.getFirewallRulePolicyID());
            }
            if (s.equals("protocol")) {
                ans.setFirewallRuleProtocol(this.getFirewallRuleProtocol());
            }
            if (s.equals("source_ip_address")) {
                ans.setFirewallRuleSrcIpAddr(this.getFirewallRuleSrcIpAddr());
            }
            if (s.equals("destination_ip_address")) {
                ans.setFirewallRuleDstIpAddr(this.getFirewallRuleDstIpAddr());
            }
            if (s.equals("source_port")) {
                ans.setFirewallRuleSrcPort(this.getFirewallRuleSrcPort());
            }
            if (s.equals("destination_port")) {
                ans.setFirewallRuleDstPort(this.getFirewallRuleDstPort());
            }
            if (s.equals("position")) {
                ans.setFirewallRulePosition(this.getFirewallRulePosition());
            }
            if (s.equals("action")) {
                ans.setFirewallRuleAction(this.getFirewallRuleAction());
            }
            if (s.equals("enabled")) {
                ans.setFirewallRuleIsEnabled(firewallRuleIsEnabled);
            }

        }
        return ans;
    }

    @Override
    public String toString() {
        return "firewallPolicyRules{" +
            "firewallRuleUUID='" + firewallRuleUUID + '\'' +
            ", firewallRuleTenantID='" + firewallRuleTenantID + '\'' +
            ", firewallRuleName='" + firewallRuleName + '\'' +
            ", firewallRuleDescription='" + firewallRuleDescription + '\'' +
            ", firewallRuleAdminStateIsUp=" + firewallRuleAdminStateIsUp +
            ", firewallRuleStatus='" + firewallRuleStatus + '\'' +
            ", firewallRuleIsShared=" + firewallRuleIsShared +
            ", firewallRulePolicyID=" + firewallRulePolicyID +
            ", firewallRuleProtocol='" + firewallRuleProtocol + '\'' +
            ", firewallRuleIpVer=" + firewallRuleIpVer +
            ", firewallRuleSrcIpAddr='" + firewallRuleSrcIpAddr + '\'' +
            ", firewallRuleDstIpAddr='" + firewallRuleDstIpAddr + '\'' +
            ", firewallRuleSrcPort=" + firewallRuleSrcPort +
            ", firewallRuleDstPort=" + firewallRuleDstPort +
            ", firewallRulePosition=" + firewallRulePosition +
            ", firewallRuleAction='" + firewallRuleAction + '\'' +
            ", firewallRuleIsEnabled=" + firewallRuleIsEnabled +
            '}';
    }

    public void initDefaults() {
    }
}