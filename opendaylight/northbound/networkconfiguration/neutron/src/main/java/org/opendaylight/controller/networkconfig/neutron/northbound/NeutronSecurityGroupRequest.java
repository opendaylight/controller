package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class NeutronSecurityGroupRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="security_group")
    NeutronSecurityGroup singletonSecGroup;

    @XmlElement(name="security_groups")
    List<NeutronSecurityGroup> bulkRequest;

    public NeutronSecurityGroupRequest() {
    }

    public NeutronSecurityGroupRequest(List<NeutronSecurityGroup> bulk) {
        bulkRequest = bulk;
    }

    public NeutronSecurityGroupRequest(NeutronSecurityGroup secGroup) {
        singletonSecGroup = secGroup;
    }

    public NeutronSecurityGroup getSingleton() {
        return singletonSecGroup;
    }

    public boolean isSingleton() {
        return singletonSecGroup != null;
    }

    public List<NeutronSecurityGroup> getBulk() {
        return bulkRequest;
    }
}
