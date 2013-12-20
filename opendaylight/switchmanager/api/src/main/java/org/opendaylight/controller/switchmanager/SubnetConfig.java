
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GUIField;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * The class represents a subnet configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SubnetConfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String prettyFields[] = { GUIField.NAME.toString(),
            GUIField.GATEWAYIP.toString(), GUIField.NODEPORTS.toString() };

    /**
     * Name of the subnet
     */
    @XmlElement
    private String name;
    /**
     * A.B.C.D/MM  Where A.B.C.D is the Default
     * Gateway IP (L3) or ARP Querier IP (L2)
     */
    @XmlElement
    private String subnet;
    /**
     * Set of node connectors in the format:
     * Port Type|Port Id@Node Type|Node Id
     */
    @XmlElement
    private List<String> nodeConnectors;

    public SubnetConfig() {
    }

    public SubnetConfig(String name, String subnet, List<String> nodeConnectors) {
        this.name = name;
        this.subnet = subnet;
        this.nodeConnectors = nodeConnectors;
    }

    public SubnetConfig(SubnetConfig subnetConfig) {
        name = subnetConfig.name;
        subnet = subnetConfig.subnet;
        nodeConnectors = (subnetConfig.nodeConnectors == null) ? null : new ArrayList<String>(subnetConfig.nodeConnectors);
    }

    public String getName() {
        return name;
    }

    public List<String> getNodePorts() {
        return (nodeConnectors == null) ? new ArrayList<String>(0) : new ArrayList<String>(nodeConnectors);
    }

    public String getSubnet() {
        return subnet;
    }

    public InetAddress getIPAddress() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(subnet.split("/")[0]);
        } catch (UnknownHostException e1) {
            return null;
        }
        return ip;
    }

    public Short getIPMaskLen() {
        Short maskLen = 0;
        String[] s = subnet.split("/");

        try{
        maskLen = (s.length == 2) ? Short.valueOf(s[1]) : 32;
        }
        catch(NumberFormatException e){
            maskLen = 32;
        }
        return maskLen;
    }

    private Status validateSubnetAddress() {
        if (!NetUtils.isIPAddressValid(subnet)) {
            return new Status(StatusCode.BADREQUEST, String.format("Invalid Subnet configuration: Invalid address: %s", subnet));
        }
        if((this.getIPMaskLen() == 0) || (this.getIPMaskLen() == 32)) {
            return new Status(StatusCode.BADREQUEST, String.format("Invalid Subnet configuration: Invalid mask: /%s", this.getIPMaskLen()));
        }

        int networkBytes = this.getIPMaskLen() / 8;
        int bits = this.getIPMaskLen() % 8;
        byte checkFor0sByte;
        byte checkFor1sByte;
        boolean allZeros = true;
        boolean allOnes = true;
        boolean networkBytesSame = true;
        boolean hostBytesSame = true;
        byte[] ip = this.getIPAddress()
                .getAddress();

        int byteCounter = 0;

        // Check the full bytes before network address ends
        for (; byteCounter < networkBytes && networkBytesSame; byteCounter++) {
            if (ip[byteCounter] == 0) {
                allOnes = false;
            }
            if (~ip[byteCounter] == 0) {
                allZeros = false;
            }
            if ((ip[byteCounter] != 0 && ~ip[byteCounter] != 0) || (allZeros && allOnes)) {
                networkBytesSame = false;
            }
        }
        // Check the byte where the network address ends
        if (bits > 0) {
            if (networkBytesSame) {
                checkFor0sByte = (byte) (ip[networkBytes] >> (8 - bits));
                checkFor1sByte = (byte) ((~ip[networkBytes]) >> (8 - bits));
                if (checkFor0sByte == 0 && allZeros || checkFor1sByte == 0 && allOnes) {
                    return new Status(StatusCode.BADREQUEST, "Invalid network source address: Subnet can't be all 0's or all 1's");
                }
            }

            // Checking the start of the host address
            allZeros = true;
            allOnes = true;
            checkFor0sByte = (byte) (ip[networkBytes] << (bits));
            checkFor1sByte = (byte) ((~ip[networkBytes] * 0xFF) << (bits));

            if (checkFor0sByte == 0) {
                allOnes = false;
            } else if (checkFor1sByte == 0) {
                allZeros = false;
            } else {
                hostBytesSame = false;
            }

        }
        // Check the full bytes for host address after network address ends
        if (hostBytesSame) {
            for (byteCounter = (bits > 0) ? networkBytes + 1 : networkBytes; byteCounter < ip.length; byteCounter++) {
                if (ip[byteCounter] == 0) {
                    allOnes = false;
                } else if (~ip[byteCounter] == 0) {
                    allZeros = false;
                } else {
                    return new Status(StatusCode.SUCCESS);
                }
                if (!allZeros && !allOnes) {
                    return new Status(StatusCode.SUCCESS);
                }
            }
            return new Status(StatusCode.BADREQUEST, "Invalid network source address: Host can't be all 0's or all 1's");
        }

        return new Status(StatusCode.SUCCESS);
    }

    public static Status validatePorts(List<String> nodeConnectors) {
        if (nodeConnectors != null) {
            for (String port : nodeConnectors) {
                if (null == NodeConnector.fromString(port)) {
                    return new Status(StatusCode.BADREQUEST,
                            "Invalid Subnet configuration: Not parsable node connector: " + port);
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status validateName() {
        if (name == null || name.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid name");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status validate() {
        Status status = validateName();
        if (status.isSuccess()) {
            status = validateSubnetAddress();
            if (status.isSuccess()) {
                status = validatePorts(this.nodeConnectors);
            }
        }
        return status;
    }

    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : prettyFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    public Set<NodeConnector> getNodeConnectors() {
        return NodeConnector.fromString(this.nodeConnectors);
    }

    public boolean isGlobal() {
        // If no ports are specified to be part of the domain, then it's a global domain IP
        return (nodeConnectors == null || nodeConnectors.isEmpty());
    }

    public void addNodeConnectors(List<String> nc) {
        if (nc != null) {
            if (nodeConnectors == null) {
                nodeConnectors = new ArrayList<String>(nc);
            } else {
                nodeConnectors.addAll(nc);
            }
        }
    }

    public void removeNodeConnectors(List<String> nc) {
        if (nc != null && nodeConnectors != null) {
            nodeConnectors.removeAll(nc);
        }
    }

    @Override
    public String toString() {
        return ("SubnetConfig [Name=" + name + ", Subnet=" + subnet
                + ", NodeConnectors=" + nodeConnectors + "]");
    }

    /**
     * Implement clonable interface
     */
    @Override
    public SubnetConfig clone() {
        return new SubnetConfig(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nodeConnectors == null) ? 0 : nodeConnectors.hashCode());
        result = prime * result + ((subnet == null) ? 0 : subnet.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SubnetConfig other = (SubnetConfig) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nodeConnectors == null) {
            if (other.nodeConnectors != null) {
                return false;
            }
        } else if (!nodeConnectors.equals(other.nodeConnectors)) {
            return false;
        }
        if (subnet == null) {
            if (other.subnet != null) {
                return false;
            }
        } else if (!subnet.equals(other.subnet)) {
            return false;
        }
        return true;
    }
}
