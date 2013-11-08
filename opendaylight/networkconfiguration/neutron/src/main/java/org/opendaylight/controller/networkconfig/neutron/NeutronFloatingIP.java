/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronFloatingIP {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="id")
    String floatingIPUUID;

    @XmlElement (name="floating_network_id")
    String floatingNetworkUUID;

    @XmlElement (name="port_id")
    String portUUID;

    @XmlElement (name="fixed_ip_address")
    String fixedIPAddress;

    @XmlElement (name="floating_ip_address")
    String floatingIPAddress;

    @XmlElement (name="tenant_id")
    String tenantUUID;

    public NeutronFloatingIP() {
    }

    public String getID() { return floatingIPUUID; }

    public String getFloatingIPUUID() {
        return floatingIPUUID;
    }

    public void setFloatingIPUUID(String floatingIPUUID) {
        this.floatingIPUUID = floatingIPUUID;
    }

    public String getFloatingNetworkUUID() {
        return floatingNetworkUUID;
    }

    public void setFloatingNetworkUUID(String floatingNetworkUUID) {
        this.floatingNetworkUUID = floatingNetworkUUID;
    }

    public String getPortUUID() {
        return portUUID;
    }

    public void setPortUUID(String portUUID) {
        this.portUUID = portUUID;
    }

    public String getFixedIPAddress() {
        return fixedIPAddress;
    }

    public void setFixedIPAddress(String fixedIPAddress) {
        this.fixedIPAddress = fixedIPAddress;
    }

    public String getFloatingIPAddress() {
        return floatingIPAddress;
    }

    public void setFloatingIPAddress(String floatingIPAddress) {
        this.floatingIPAddress = floatingIPAddress;
    }

    public String getTenantUUID() {
        return tenantUUID;
    }

    public void setTenantUUID(String tenantUUID) {
        this.tenantUUID = tenantUUID;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackFloatingIPs object with only the selected fields
     * populated
     */

    public NeutronFloatingIP extractFields(List<String> fields) {
        NeutronFloatingIP ans = new NeutronFloatingIP();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setFloatingIPUUID(this.getFloatingIPUUID());
            }
            if (s.equals("floating_network_id")) {
                ans.setFloatingNetworkUUID(this.getFloatingNetworkUUID());
            }
            if (s.equals("port_id")) {
                ans.setPortUUID(this.getPortUUID());
            }
            if (s.equals("fixed_ip_address")) {
                ans.setFixedIPAddress(this.getFixedIPAddress());
            }
            if (s.equals("floating_ip_address")) {
                ans.setFloatingIPAddress(this.getFloatingIPAddress());
            }
            if (s.equals("tenant_id")) {
                ans.setTenantUUID(this.getTenantUUID());
            }
        }
        return ans;
    }

    public void initDefaults() {
    }
}
