package org.opendaylight.controller.networkconfig.neutron;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronSecurityGroupRule implements INeutronObject {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="id")
    private
    String secGroupRuleUUID;

    @XmlElement (name="security_group_id")
    private
    String secGroupUUID;

    @XmlElement (name="tenant_id")
    private
    String tenantUUID;

    @XmlElement (name="direction")
    private
    NeutronSecurityGroupRule_Direction direction;

    @XmlElement (name="ethertype")
    private
    NeutronSecurityGroupRule_Ethertype ethertype;

    @XmlElement (name="port_range_max")
    private
    Integer portRangeMax;

    @XmlElement (name="port_range_min")
    private
    Integer portRangeMin;

    @XmlElement (name="protocol")
    private
    NeutronSecurityGroupRule_Protocol protocol;

    @XmlElement (name="remote_group_id")
    private
    String remoteGroupUUID;

    @XmlElement (name="remote_ip_prefix")
    private
    String remoteIpPrefix;

    public void initDefaults() {
        if (secGroupRuleUUID == null)
            secGroupRuleUUID = UUID.randomUUID().toString();
        if (ethertype == null)
            ethertype = NeutronSecurityGroupRule_Ethertype.IPv4;
    }

    public String getID() {
        return secGroupRuleUUID;
    }

    public String getSecGroupRuleUUID() {
        return secGroupRuleUUID;
    }

    public void setSecGroupRuleUUID(String secGroupRuleUUID) {
        this.secGroupRuleUUID = secGroupRuleUUID;
    }

    public String getSecGroupUUID() {
        return secGroupUUID;
    }

    public void setSecGroupUUID(String secGroupUUID) {
        this.secGroupUUID = secGroupUUID;
    }

    public String getTenantUUID() {
        return tenantUUID;
    }

    public void setTenantUUID(String tenantUUID) {
        this.tenantUUID = tenantUUID;
    }

    public NeutronSecurityGroupRule_Direction getDirection() {
        return direction;
    }

    public void setDirection(NeutronSecurityGroupRule_Direction direction) {
        this.direction = direction;
    }

    public NeutronSecurityGroupRule_Ethertype getEthertype() {
        return ethertype;
    }

    public void setEthertype(NeutronSecurityGroupRule_Ethertype ethertype) {
        this.ethertype = ethertype;
    }

    public Integer getPortRangeMax() {
        return portRangeMax;
    }

    public void setPortRangeMax(Integer portRangeMax) {
        this.portRangeMax = portRangeMax;
    }

    public Integer getPortRangeMin() {
        return portRangeMin;
    }

    public void setPortRangeMin(Integer portRangeMin) {
        this.portRangeMin = portRangeMin;
    }

    public NeutronSecurityGroupRule_Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(NeutronSecurityGroupRule_Protocol protocol) {
        this.protocol = protocol;
    }

    public String getRemoteGroupUUID() {
        return remoteGroupUUID;
    }

    public void setRemoteGroupUUID(String remoteGroupUUID) {
        this.remoteGroupUUID = remoteGroupUUID;
    }

    public String getRemoteIpPrefix() {
        return remoteIpPrefix;
    }

    public void setRemoteIpPrefix(String remoteIpPrefix) {
        this.remoteIpPrefix = remoteIpPrefix;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return a NeutronSecurityGroupRule object with only the selected fields
     * populated
     */

    public NeutronSecurityGroupRule extractFields(List<String> fields) {
        NeutronSecurityGroupRule ans = new NeutronSecurityGroupRule();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setSecGroupRuleUUID(this.getSecGroupRuleUUID());
                    break;
                case "security_group_id":
                    ans.setSecGroupUUID(this.getSecGroupUUID());
                    break;
                case "tenant_id":
                    ans.setTenantUUID(this.getTenantUUID());
                    break;
                case "direction":
                    ans.setDirection(this.getDirection());
                    break;
                case "ethertype":
                    ans.setEthertype(this.getEthertype());
                    break;
                case "port_range_max":
                    ans.setPortRangeMax(this.getPortRangeMax());
                    break;
                case "port_range_min":
                    ans.setPortRangeMin(this.getPortRangeMin());
                    break;
                case "protocol":
                    ans.setProtocol(this.getProtocol());
                    break;
                case "remote_group_id":
                    ans.setRemoteGroupUUID(this.getRemoteGroupUUID());
                    break;
                case "remote_ip_prefix":
                    ans.setRemoteIpPrefix(this.getRemoteIpPrefix());
                    break;
                default:
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronSecurityGroupRule [secGroupRuleUUID=" + secGroupRuleUUID
                + ", secGroupUUID=" + secGroupUUID + ", tenantUUID=" + tenantUUID
                + ", direction=" + direction + ", ethertype=" + ethertype
                + ", portRangeMax=" + portRangeMax + ", portRangeMin=" + portRangeMin
                + ", protocol=" + protocol + ", remoteGroupUUID=" + remoteGroupUUID
                + ", remoteIpPrefix=" + remoteIpPrefix + "]";
    }
}
