package org.opendaylight.controller.networkconfig.neutron;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronSecurityGroup implements INeutronObject {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    public static String DEFAULT_NAME = "default";

    @XmlElement (name="id")
    String secGroupUUID;

    @XmlElement (name="name")
    String name;

    @XmlElement (name="description")
    String description;

    @XmlElement (name="security_group_rules")
    List<NeutronSecurityGroupRule> rules;

    @XmlElement (name="tenant_id")
    String tenantUUID;

    /* Holds the Neutron Ports associated with an instance
     * used to determine if that instance can be deleted.
     */
    List<NeutronPort> ports;

    public void initDefaults() {
        if (secGroupUUID == null)
            secGroupUUID = UUID.randomUUID().toString();

        if (name == null) {
            name = "";
        }

        if (description == null) {
            description = "";
        }

        rules = new ArrayList<>();
        rules.add(createDefaultRule(NeutronSecurityGroupRule_Ethertype.IPv4));
        rules.add(createDefaultRule(NeutronSecurityGroupRule_Ethertype.IPv6));

        ports = new ArrayList<>();
    }

    private static NeutronSecurityGroupRule createDefaultRule(NeutronSecurityGroupRule_Ethertype type) {
        NeutronSecurityGroupRule ans = new NeutronSecurityGroupRule();
        ans.initDefaults();
        ans.setEthertype(type);
        ans.setDirection(NeutronSecurityGroupRule_Direction.EGRESS);
        return ans;
    }

    public String getID() {
        return secGroupUUID;
    }

    public String getSecGroupUUID() {
        return secGroupUUID;
    }

    public void setSecGroupUUID(String secGroupUUID) {
        this.secGroupUUID = secGroupUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefault() {
        return this.name.equals(DEFAULT_NAME);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NeutronSecurityGroupRule> getRules() {
        return rules;
    }

    public void setRules(List<NeutronSecurityGroupRule> rules) {
        this.rules = rules;
    }

    public String getTenantUUID() {
        return tenantUUID;
    }

    public void setTenantUUID(String tenantUUID) {
        this.tenantUUID = tenantUUID;
    }

    public void addRule(NeutronSecurityGroupRule rule) {
        rules.add(rule);
    }

    public void removeRule(NeutronSecurityGroupRule rule) {
        rules.remove(rule);
    }

    public void addPort(NeutronPort port) {
        ports.add(port);
    }

    public void removePort(NeutronPort port) {
        ports.remove(port);
    }

    public List<NeutronPort> getPorts() {
        return ports;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return a NeutronSecurityGroup object with only the selected fields
     * populated
     */

    public NeutronSecurityGroup extractFields(List<String> fields) {
        NeutronSecurityGroup ans = new NeutronSecurityGroup();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setSecGroupUUID(this.getSecGroupUUID());
                    break;
                case "name":
                    ans.setName(this.getName());
                    break;
                case "description":
                    ans.setDescription(this.getDescription());
                    break;
                case "security_group_rules":
                    ans.setRules(this.getRules());
                    break;
                case "tenant_id":
                    ans.setTenantUUID(this.getTenantUUID());
                    break;
                default:
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronSecurityGroup [secGroupUUID=" + secGroupUUID + ", name=" + name
                + ", description=" + description + ", tenantUUID=" + tenantUUID
                + ", rules=" + rules + "]";
    }
}
