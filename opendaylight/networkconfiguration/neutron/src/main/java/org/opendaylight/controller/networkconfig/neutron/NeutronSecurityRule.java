/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
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
 * See OpenStack Network API v2.0 Reference for description of
 * annotated attributes. The current fields are as follows:
 * <p/>
 * id                uuid (String) UUID for the security group rule.
 * security_rule_id  uuid (String) The security group to associate rule.
 * direction         String Direction the VM traffic  (ingress/egress).
 * security_group_id The security group to associate rule with.
 * protocol          String IP Protocol (icmp, tcp, udp, etc).
 * port_range_min    Integer Port at start of range
 * port_range_max    Integer Port at end of range
 * ethertype         String ethertype in L2 packet (IPv4, IPv6, etc)
 * remote_ip_prefix  String (IP cidr) CIDR for address range.
 * remote_group_id   uuid-str Source security group to apply to rule.
 * tenant_id         uuid-str Owner of security rule. Admin only outside tenant.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronSecurityRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "id")
    String securityRuleUUID;

    @XmlElement(name = "direction")
    String securityRuleDirection;

    @XmlElement(name = "protocol")
    String securityRuleProtocol;

    @XmlElement(name = "port_range_min")
    Integer securityRulePortMin;

    @XmlElement(name = " port_range_max")
    Integer securityRulePortMax;

    @XmlElement(name = "ethertype")
    String securityRuleEthertype;

    @XmlElement(name = "remote_ip_prefix")
    String securityRuleRemoteIpPrefix;

    @XmlElement(name = "remote_group_id")
    String securityRemoteGroupID;

    @XmlElement(name = "security_group_id")
    String securityRuleGroupID;

    @XmlElement(name = "tenant_id")
    String securityRuleTenantID;

    public NeutronSecurityRule() {
        List<NeutronSecurityRule> securityRules;
    }

    public String getSecurityRuleUUID() {
        return securityRuleUUID;
    }

    public void setSecurityRuleUUID(String securityRuleUUID) {
        this.securityRuleUUID = securityRuleUUID;
    }

    public String getSecurityRuleDirection() {
        return securityRuleDirection;
    }

    public void setSecurityRuleDirection(String securityRuleDirection) {
        this.securityRuleDirection = securityRuleDirection;
    }

    public String getSecurityRuleProtocol() {
        return securityRuleProtocol;
    }

    public void setSecurityRuleProtocol(String securityRuleProtocol) {
        this.securityRuleProtocol = securityRuleProtocol;
    }

    public Integer getSecurityRulePortMin() {
        return securityRulePortMin;
    }

    public void setSecurityRulePortMin(Integer securityRulePortMin) {
        this.securityRulePortMin = securityRulePortMin;
    }

    public Integer getSecurityRulePortMax() {
        return securityRulePortMax;
    }

    public void setSecurityRulePortMax(Integer securityRulePortMax) {
        this.securityRulePortMax = securityRulePortMax;
    }

    public String getSecurityRuleEthertype() {
        return securityRuleEthertype;
    }

    public void setSecurityRuleEthertype(String securityRuleEthertype) {
        this.securityRuleEthertype = securityRuleEthertype;
    }

    public String getSecurityRuleRemoteIpPrefix() {
        return securityRuleRemoteIpPrefix;
    }

    public void setSecurityRuleRemoteIpPrefix(String securityRuleRemoteIpPrefix) {
        this.securityRuleRemoteIpPrefix = securityRuleRemoteIpPrefix;
    }

    public String getSecurityRemoteGroupID() {
        return securityRemoteGroupID;
    }

    public void setSecurityRemoteGroupID(String securityRemoteGroupID) {
        this.securityRemoteGroupID = securityRemoteGroupID;
    }

    public String getSecurityRuleGroupID() {
        return securityRuleGroupID;
    }

    public void setSecurityRuleGroupID(String securityRuleGroupID) {
        this.securityRuleGroupID = securityRuleGroupID;
    }

    public String getSecurityRuleTenantID() {
        return securityRuleTenantID;
    }

    public void setSecurityRuleTenantID(String securityRuleTenantID) {
        this.securityRuleTenantID = securityRuleTenantID;
    }

    public NeutronSecurityRule extractFields(List<String> fields) {
        NeutronSecurityRule ans = new NeutronSecurityRule();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setSecurityRuleUUID(this.getSecurityRuleUUID());
            }
            if (s.equals("direction")) {
                ans.setSecurityRuleDirection(this.getSecurityRuleDirection());
            }
            if (s.equals("protocol")) {
                ans.setSecurityRuleProtocol(this.getSecurityRuleProtocol());
            }
            if (s.equals("port_range_min")) {
                ans.setSecurityRulePortMin(this.getSecurityRulePortMin());
            }
            if (s.equals("port_range_max")) {
                ans.setSecurityRulePortMax(this.getSecurityRulePortMax());
            }
            if (s.equals("ethertype")) {
                ans.setSecurityRuleEthertype(this.getSecurityRuleEthertype());
            }
            if (s.equals("remote_ip_prefix")) {
                ans.setSecurityRuleRemoteIpPrefix(this.getSecurityRuleRemoteIpPrefix());
            }
            if (s.equals("remote_group_id")) {
                ans.setSecurityRemoteGroupID(this.getSecurityRemoteGroupID());
            }
            if (s.equals("security_group_id")) {
                ans.setSecurityRuleGroupID(this.getSecurityRuleGroupID());
            }
            if (s.equals("tenant_id")) {
                ans.setSecurityRuleTenantID(this.getSecurityRuleTenantID());
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronSecurityRule{" +
            "securityRuleUUID='" + securityRuleUUID + '\'' +
            ", securityRuleDirection='" + securityRuleDirection + '\'' +
            ", securityRuleProtocol='" + securityRuleProtocol + '\'' +
            ", securityRulePortMin=" + securityRulePortMin +
            ", securityRulePortMax=" + securityRulePortMax +
            ", securityRuleEthertype='" + securityRuleEthertype + '\'' +
            ", securityRuleRemoteIpPrefix='" + securityRuleRemoteIpPrefix + '\'' +
            ", securityRemoteGroupID=" + securityRemoteGroupID +
            ", securityRuleGroupID='" + securityRuleGroupID + '\'' +
            ", securityRuleTenantID='" + securityRuleTenantID + '\'' +
            '}';
    }

    public void initDefaults() {
        //TODO verify no defaults values are nessecary required.
    }
}