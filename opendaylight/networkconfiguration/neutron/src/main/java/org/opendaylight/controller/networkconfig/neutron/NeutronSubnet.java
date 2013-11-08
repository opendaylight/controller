/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronSubnet {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="id")
    String subnetUUID;

    @XmlElement (name="network_id")
    String networkUUID;

    @XmlElement (name="name")
    String name;

    @XmlElement (defaultValue="4", name="ip_version")
    Integer ipVersion;

    @XmlElement (name="cidr")
    String cidr;

    @XmlElement (name="gateway_ip")
    String gatewayIP;

    @XmlElement (name="dns_nameservers")
    List<String> dnsNameservers;

    @XmlElement (name="allocation_pools")
    List<NeutronSubnet_IPAllocationPool> allocationPools;

    @XmlElement (name="host_routes")
    List<NeutronSubnet_HostRoute> hostRoutes;

    @XmlElement (defaultValue="true", name="enable_dhcp")
    Boolean enableDHCP;

    @XmlElement (name="tenant_id")
    String tenantID;

    /* stores the OpenStackPorts associated with an instance
     * used to determine if that instance can be deleted.
     */
    List<NeutronPort> myPorts;

    boolean gatewayIPAssigned;

    public NeutronSubnet() {
        myPorts = new ArrayList<NeutronPort>();
    }

    public String getID() { return subnetUUID; }

    public String getSubnetUUID() {
        return subnetUUID;
    }

    public void setSubnetUUID(String subnetUUID) {
        this.subnetUUID = subnetUUID;
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

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getGatewayIP() {
        return gatewayIP;
    }

    public void setGatewayIP(String gatewayIP) {
        this.gatewayIP = gatewayIP;
    }

    public List<String> getDnsNameservers() {
        return dnsNameservers;
    }

    public void setDnsNameservers(List<String> dnsNameservers) {
        this.dnsNameservers = dnsNameservers;
    }

    public List<NeutronSubnet_IPAllocationPool> getAllocationPools() {
        return allocationPools;
    }

    public void setAllocationPools(List<NeutronSubnet_IPAllocationPool> allocationPools) {
        this.allocationPools = allocationPools;
    }

    public List<NeutronSubnet_HostRoute> getHostRoutes() {
        return hostRoutes;
    }

    public void setHostRoutes(List<NeutronSubnet_HostRoute> hostRoutes) {
        this.hostRoutes = hostRoutes;
    }

    public boolean isEnableDHCP() {
        if (enableDHCP == null) {
            return true;
        }
        return enableDHCP;
    }

    public Boolean getEnableDHCP() { return enableDHCP; }

    public void setEnableDHCP(Boolean newValue) {
            enableDHCP = newValue;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackSubnets object with only the selected fields
     * populated
     */

    public NeutronSubnet extractFields(List<String> fields) {
        NeutronSubnet ans = new NeutronSubnet();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setSubnetUUID(this.getSubnetUUID());
            }
            if (s.equals("network_id")) {
                ans.setNetworkUUID(this.getNetworkUUID());
            }
            if (s.equals("name")) {
                ans.setName(this.getName());
            }
            if (s.equals("ip_version")) {
                ans.setIpVersion(this.getIpVersion());
            }
            if (s.equals("cidr")) {
                ans.setCidr(this.getCidr());
            }
            if (s.equals("gateway_ip")) {
                ans.setGatewayIP(this.getGatewayIP());
            }
            if (s.equals("dns_nameservers")) {
                List<String> nsList = new ArrayList<String>();
                nsList.addAll(this.getDnsNameservers());
                ans.setDnsNameservers(nsList);
            }
            if (s.equals("allocation_pools")) {
                List<NeutronSubnet_IPAllocationPool> aPools = new ArrayList<NeutronSubnet_IPAllocationPool>();
                aPools.addAll(this.getAllocationPools());
                ans.setAllocationPools(aPools);
            }
            if (s.equals("host_routes")) {
                List<NeutronSubnet_HostRoute> hRoutes = new ArrayList<NeutronSubnet_HostRoute>();
                hRoutes.addAll(this.getHostRoutes());
                ans.setHostRoutes(hRoutes);
            }
            if (s.equals("enable_dhcp")) {
                ans.setEnableDHCP(this.getEnableDHCP());
            }
            if (s.equals("tenant_id")) {
                ans.setTenantID(this.getTenantID());
            }
        }
        return ans;
    }

    /* test to see if the cidr address used to define this subnet
     * is a valid network address (an necessary condition when creating
     * a new subnet)
     */
    public boolean isValidCIDR() {
        try {
            SubnetUtils util = new SubnetUtils(cidr);
            SubnetInfo info = util.getInfo();
            if (!info.getNetworkAddress().equals(info.getAddress())) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /* test to see if the gateway IP specified overlaps with specified
     * allocation pools (an error condition when creating a new subnet
     * or assigning a gateway IP)
     */
    public boolean gatewayIP_Pool_overlap() {
        Iterator<NeutronSubnet_IPAllocationPool> i = allocationPools.iterator();
        while (i.hasNext()) {
            NeutronSubnet_IPAllocationPool pool = i.next();
            if (pool.contains(gatewayIP)) {
                return true;
            }
        }
        return false;
    }

    public boolean initDefaults() {
        if (enableDHCP == null) {
            enableDHCP = true;
        }
        if (ipVersion == null) {
            ipVersion = 4;
        }
        gatewayIPAssigned = false;
        dnsNameservers = new ArrayList<String>();
        allocationPools = new ArrayList<NeutronSubnet_IPAllocationPool>();
        hostRoutes = new ArrayList<NeutronSubnet_HostRoute>();
        try {
            SubnetUtils util = new SubnetUtils(cidr);
            SubnetInfo info = util.getInfo();
            if (gatewayIP == null) {
                gatewayIP = info.getLowAddress();
            }
            if (allocationPools.size() < 1) {
                NeutronSubnet_IPAllocationPool source =
                    new NeutronSubnet_IPAllocationPool(info.getLowAddress(),
                            info.getHighAddress());
                allocationPools = source.splitPool(gatewayIP);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<NeutronPort> getPortsInSubnet() {
        return myPorts;
    }

    public void addPort(NeutronPort port) {
        myPorts.add(port);
    }

    public void removePort(NeutronPort port) {
        myPorts.remove(port);
    }

    /* this method tests to see if the supplied IPv4 address
     * is valid for this subnet or not
     */
    public boolean isValidIP(String ipAddress) {
        try {
            SubnetUtils util = new SubnetUtils(cidr);
            SubnetInfo info = util.getInfo();
            return info.isInRange(ipAddress);
        } catch (Exception e) {
            return false;
        }
    }

    /* test to see if the supplied IPv4 address is part of one of the
     * available allocation pools or not
     */
    public boolean isIPInUse(String ipAddress) {
        if (ipAddress.equals(gatewayIP) && !gatewayIPAssigned ) {
            return false;
        }
        Iterator<NeutronSubnet_IPAllocationPool> i = allocationPools.iterator();
        while (i.hasNext()) {
            NeutronSubnet_IPAllocationPool pool = i.next();
            if (pool.contains(ipAddress)) {
                return false;
            }
        }
        return true;
    }

    /* method to get the lowest available address of the subnet.
     * go through all the allocation pools and keep the lowest of their
     * low addresses.
     */
    public String getLowAddr() {
        String ans = null;
        Iterator<NeutronSubnet_IPAllocationPool> i = allocationPools.iterator();
        while (i.hasNext()) {
            NeutronSubnet_IPAllocationPool pool = i.next();
            if (ans == null) {
                ans = pool.getPoolStart();
            }
            else
                if (NeutronSubnet_IPAllocationPool.convert(pool.getPoolStart()) <
                        NeutronSubnet_IPAllocationPool.convert(ans)) {
                    ans = pool.getPoolStart();
                }
        }
        return ans;
    }

    /*
     * allocate the parameter address.  Because this uses an iterator to
     * check the instance's list of allocation pools and we want to modify
     * pools while the iterator is being used, it is necessary to
     * build a new list of allocation pools and replace the list when
     * finished (otherwise a split will cause undefined iterator behavior.
     */
    public void allocateIP(String ipAddress) {
        Iterator<NeutronSubnet_IPAllocationPool> i = allocationPools.iterator();
        List<NeutronSubnet_IPAllocationPool> newList = new ArrayList<NeutronSubnet_IPAllocationPool>();    // we have to modify a separate list
        while (i.hasNext()) {
            NeutronSubnet_IPAllocationPool pool = i.next();
            /* if the pool contains a single address element and we are allocating it
             * then we don't need to copy the pool over.  Otherwise, we need to possibly
             * split the pool and add both pieces to the new list
             */
            if (!(pool.getPoolEnd().equalsIgnoreCase(ipAddress) &&
                    pool.getPoolStart().equalsIgnoreCase(ipAddress))) {
                if (pool.contains(ipAddress)) {
                    List<NeutronSubnet_IPAllocationPool> pools = pool.splitPool(ipAddress);
                    newList.addAll(pools);
                } else {
                    newList.add(pool);
                }
            }
        }
        allocationPools = newList;
    }

    /*
     * release an IP address back to the subnet.  Although an iterator
     * is used, the list is not modified until the iterator is complete, so
     * an extra list is not necessary.
     */
    public void releaseIP(String ipAddress) {
        NeutronSubnet_IPAllocationPool lPool = null;
        NeutronSubnet_IPAllocationPool hPool = null;
        Iterator<NeutronSubnet_IPAllocationPool> i = allocationPools.iterator();
        long sIP = NeutronSubnet_IPAllocationPool.convert(ipAddress);
        //look for lPool where ipAddr - 1 is high address
        //look for hPool where ipAddr + 1 is low address
        while (i.hasNext()) {
            NeutronSubnet_IPAllocationPool pool = i.next();
            long lIP = NeutronSubnet_IPAllocationPool.convert(pool.getPoolStart());
            long hIP = NeutronSubnet_IPAllocationPool.convert(pool.getPoolEnd());
            if (sIP+1 == lIP) {
                hPool = pool;
            }
            if (sIP-1 == hIP) {
                lPool = pool;
            }
        }
        //if (lPool == NULL and hPool == NULL) create new pool where low = ip = high
        if (lPool == null && hPool == null) {
            allocationPools.add(new NeutronSubnet_IPAllocationPool(ipAddress,ipAddress));
        }
        //if (lPool == NULL and hPool != NULL) change low address of hPool to ipAddr
        if (lPool == null && hPool != null) {
            hPool.setPoolStart(ipAddress);
        }
        //if (lPool != NULL and hPool == NULL) change high address of lPool to ipAddr
        if (lPool != null && hPool == null) {
            lPool.setPoolEnd(ipAddress);
        }
        //if (lPool != NULL and hPool != NULL) remove lPool and hPool and create new pool
        //        where low address = lPool.low address and high address = hPool.high Address
        if (lPool != null && hPool != null) {
            allocationPools.remove(lPool);
            allocationPools.remove(hPool);
            allocationPools.add(new NeutronSubnet_IPAllocationPool(
                    lPool.getPoolStart(), hPool.getPoolEnd()));
        }
    }

    public void setGatewayIPAllocated() {
        gatewayIPAssigned = true;
    }

    public void resetGatewayIPAllocated() {
        gatewayIPAssigned = false;
    }
}
