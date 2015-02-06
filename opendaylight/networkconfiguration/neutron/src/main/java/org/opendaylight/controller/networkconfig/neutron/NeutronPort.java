/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)


public class NeutronPort extends ConfigurationObject implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="id")
    String portUUID;

    @XmlElement (name="network_id")
    String networkUUID;

    @XmlElement (name="name")
    String name;

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean adminStateUp;

    @XmlElement (name="status")
    String status;

    @XmlElement (name="mac_address")
    String macAddress;

    @XmlElement (name="fixed_ips")
    List<Neutron_IPs> fixedIPs;

    @XmlElement (name="device_id")
    String deviceID;

    @XmlElement (name="device_owner")
    String deviceOwner;

    @XmlElement (name="tenant_id")
    String tenantID;

    @XmlElement (name="security_groups")
    List<NeutronSecurityGroup> securityGroups;

    @XmlElement (namespace= "binding", name="host_id")
    String bindinghostID;

    @XmlElement (namespace= "binding", name="vnic_type")
    String bindingvnicType;

    @XmlElement (namespace= "binding", name="vif_type")
    String bindingvifType;


    /* this attribute stores the floating IP address assigned to
     * each fixed IP address
     */

    HashMap<String, NeutronFloatingIP> floatingIPMap;

    public NeutronPort() {
        floatingIPMap = new HashMap<String, NeutronFloatingIP>();
    }

    public String getID() { return portUUID; }

    public void setID(String id) { this.portUUID = id; }

    public String getPortUUID() {
        return portUUID;
    }

    public void setPortUUID(String portUUID) {
        this.portUUID = portUUID;
    }

    public String getNetworkUUID() {
        return networkUUID;
    }

    public void setNetworkUUID(String networkUUID) {
        this.networkUUID = networkUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdminStateUp() {
        if (adminStateUp == null) {
            return true;
        }
        return adminStateUp;
    }

    public Boolean getAdminStateUp() { return adminStateUp; }

    public void setAdminStateUp(Boolean newValue) {
            adminStateUp = newValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public List<Neutron_IPs> getFixedIPs() {
        return fixedIPs;
    }

    public void setFixedIPs(List<Neutron_IPs> fixedIPs) {
        this.fixedIPs = fixedIPs;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceOwner() {
        return deviceOwner;
    }

    public void setDeviceOwner(String deviceOwner) {
        this.deviceOwner = deviceOwner;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public List<NeutronSecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<NeutronSecurityGroup> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public String getBindinghostID() {
      return bindinghostID;
    }

    public void setBindinghostID(String bindinghostID) {
      this.bindinghostID = bindinghostID;
    }

  public String getBindingvnicType() {
    return bindingvnicType;
  }

  public void setBindingvnicType(String bindingvnicType) {
    this.bindingvnicType = bindingvnicType;
  }

  public String getBindingvifType() {
    return bindingvifType;
  }

  public void setBindingvifType(String bindingvifType) {
    this.bindingvifType = bindingvifType;
  }

    public NeutronFloatingIP getFloatingIP(String key) {
        if (!floatingIPMap.containsKey(key)) {
            return null;
        }
        return floatingIPMap.get(key);
    }

    public void removeFloatingIP(String key) {
        floatingIPMap.remove(key);
    }

    public void addFloatingIP(String key, NeutronFloatingIP floatingIP) {
        if (!floatingIPMap.containsKey(key)) {
            floatingIPMap.put(key, floatingIP);
        }
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackPorts object with only the selected fields
     * populated
     */

    public NeutronPort extractFields(List<String> fields) {
        NeutronPort ans = new NeutronPort();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setPortUUID(this.getPortUUID());
            }
            if (s.equals("network_id")) {
                ans.setNetworkUUID(this.getNetworkUUID());
            }
            if (s.equals("name")) {
                ans.setName(this.getName());
            }
            if (s.equals("admin_state_up")) {
                ans.setAdminStateUp(this.getAdminStateUp());
            }
            if (s.equals("status")) {
                ans.setStatus(this.getStatus());
            }
            if (s.equals("mac_address")) {
                ans.setMacAddress(this.getMacAddress());
            }
            if (s.equals("fixed_ips")) {
                List<Neutron_IPs> fixedIPs = new ArrayList<Neutron_IPs>();
                fixedIPs.addAll(this.getFixedIPs());
                ans.setFixedIPs(fixedIPs);
            }
            if (s.equals("device_id")) {
                ans.setDeviceID(this.getDeviceID());
            }
            if (s.equals("device_owner")) {
                ans.setDeviceOwner(this.getDeviceOwner());
            }
            if (s.equals("tenant_id")) {
                ans.setTenantID(this.getTenantID());
            }
            if (s.equals("security_groups")) {
                List<NeutronSecurityGroup> securityGroups = new ArrayList<NeutronSecurityGroup>();
                securityGroups.addAll(this.getSecurityGroups());
                ans.setSecurityGroups(securityGroups);
            }
        }
        return ans;
    }

    public void initDefaults() {
        adminStateUp = true;
        if (status == null) {
            status = "ACTIVE";
        }
        if (fixedIPs == null) {
            fixedIPs = new ArrayList<Neutron_IPs>();
        }
    }

    /**
     * This method checks to see if the port has a floating IPv4 address
     * associated with the supplied fixed IPv4 address
     *
     * @param fixedIP
     *            fixed IPv4 address in dotted decimal format
     * @return a boolean indicating if there is a floating IPv4 address bound
     * to the fixed IPv4 address
     */

    public boolean isBoundToFloatingIP(String fixedIP) {
        return floatingIPMap.containsKey(fixedIP);
    }

    @Override
    public String toString() {
        return "NeutronPort [portUUID=" + portUUID + ", networkUUID=" + networkUUID + ", name=" + name
                + ", adminStateUp=" + adminStateUp + ", status=" + status + ", macAddress=" + macAddress
                + ", fixedIPs=" + fixedIPs + ", deviceID=" + deviceID + ", deviceOwner=" + deviceOwner + ", tenantID="
                + tenantID + ", floatingIPMap=" + floatingIPMap + ", securityGroups=" + securityGroups
                + ", bindinghostID=" + bindinghostID + ", bindingvnicType=" + bindingvnicType
                + ", bindingvnicType=" + bindingvnicType + "]";
    }
}
