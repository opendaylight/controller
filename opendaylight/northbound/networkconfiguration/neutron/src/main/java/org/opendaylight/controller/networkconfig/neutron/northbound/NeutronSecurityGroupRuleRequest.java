package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class NeutronSecurityGroupRuleRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="security_group_rule")
    NeutronSecurityGroupRule singletonSecGroupRule;

    @XmlElement(name="security_group_rules")
    List<NeutronSecurityGroupRule> bulkRequest;

    public NeutronSecurityGroupRuleRequest() {
    }

    public NeutronSecurityGroupRuleRequest(List<NeutronSecurityGroupRule> bulk) {
        bulkRequest = bulk;
    }

    public NeutronSecurityGroupRuleRequest(NeutronSecurityGroupRule secGroup) {
        singletonSecGroupRule = secGroup;
    }

    public NeutronSecurityGroupRule getSingleton() {
        return singletonSecGroupRule;
    }

    public boolean isSingleton() {
        return singletonSecGroupRule != null;
    }

    public List<NeutronSecurityGroupRule> getBulk() {
        return bulkRequest;
    }
}
