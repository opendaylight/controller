
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GUIField;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;

/**
 * The class represents a subnet configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SubnetConfig implements Serializable {
    //static fields are by default excluded by Gson parser
    private static final long serialVersionUID = 1L;
    private static final String prettyFields[] = { GUIField.NAME.toString(),
            GUIField.GATEWAYIP.toString(), GUIField.NODEPORTS.toString() };

    // Order matters: JSP file expects following fields in the
    // following order
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String subnet; // A.B.C.D/MM  Where A.B.C.D is the Default
                           // Gateway IP (L3) or ARP Querier IP (L2
    @XmlElement
    private List<String> nodePorts; // datapath ID/port list:
                                    // xx:xx:xx:xx:xx:xx:xx:xx/a,b,c-m,r-t,y

    public SubnetConfig() {
    }

    public SubnetConfig(String desc, String sub, List<String> sp) {
        name = desc;
        subnet = sub;
        nodePorts = sp;
    }

    public String getName() {
        return name;
    }

    public List<String> getNodePorts() {
        return nodePorts;
    }

    public String getSubnet() {
        return subnet;
    }

    public InetAddress getIPnum() {
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
        if (hasValidIP()) {
            String[] s = subnet.split("/");
            maskLen = (s.length == 2) ? Short.valueOf(s[1]) : 32;
        }
        return maskLen;
    }

    private Set<Short> getPortList(String ports) {
        /*
         * example:
         *     ports = "1,3,5-12"
         *     elemArray = ["1" "3" "5-12"]
         *     elem[2] = "5-12" --> limits = ["5" "12"]
         *     portList = [1 3 5 6 7 8 9 10 11 12]
         */
        Set<Short> portList = new HashSet<Short>();
        String[] elemArray = ports.split(",");
        for (String elem : elemArray) {
            if (elem.contains("-")) {
                String[] limits = elem.split("-");
                for (short j = Short.valueOf(limits[0]); j <= Short
                        .valueOf(limits[1]); j++) {
                    portList.add(Short.valueOf(j));
                }
            } else {
                portList.add(Short.valueOf(elem));
            }
        }
        return portList;
    }

    private boolean hasValidIP() {
        if (subnet != null) {
            if (NetUtils.isIPv4AddressValid(subnet)) {
                return true;
            } else if (NetUtils.isIPv6AddressValid(subnet)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidPorts() {
        for (String portSet : nodePorts) {
            if (!portSet.contains("/")) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidSwitchPort(String sp) {
        return sp.contains("/");
    }

    public boolean isValidConfig() {
        return hasValidIP() && hasValidPorts();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        /*
         * Configuration will be stored in collection only if it is valid
         * Hence we don't check here for uninitialized fields
         */
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubnetConfig that = (SubnetConfig) obj;
        if (this.subnet.equals(that.subnet)) {
            return true;
        }
        return false;
    }

    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : prettyFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    //Utility method useful for adding to a passed Set all the
    //NodeConnectors learnt from a string
    private void getNodeConnectorsFromString(String codedNodeConnectors,
            Set<NodeConnector> sp) {
        if (codedNodeConnectors == null) {
            return;
        }
        if (sp == null) {
            return;
        }
        // codedNodeConnectors = xx:xx:xx:xx:xx:xx:xx:xx/a,b,c-m,r-t,y
        String pieces[] = codedNodeConnectors.split("/");
        for (Short port : getPortList(pieces[1])) {
            Node n = Node.fromString(pieces[0]);
            if (n == null) {
                continue;
            }
            NodeConnector p = NodeConnectorCreator.createOFNodeConnector(port,
                    n);
            if (p == null) {
                continue;
            }
            sp.add(p);
        }
    }

    public Set<NodeConnector> getSubnetNodeConnectors() {
        Set<NodeConnector> sp = new HashSet<NodeConnector>();
        if (isGlobal())
            return sp;
        for (String str : nodePorts) {
            getNodeConnectorsFromString(str, sp);
        }
        return sp;
    }

    public Set<NodeConnector> getNodeConnectors(String codedNodeConnectors) {
        // codedNodeConnectors = xx:xx:xx:xx:xx:xx:xx:xx/a,b,c-m,r-t,y
        Set<NodeConnector> sp = new HashSet<NodeConnector>();
        getNodeConnectorsFromString(codedNodeConnectors, sp);
        return sp;
    }

    public boolean isGlobal() {
        // If no ports are specified to be part of the domain, then it's a global domain IP
        return (nodePorts == null || nodePorts.isEmpty());
    }

    public void addNodeConnectors(String sp) {
        nodePorts.add(sp);
    }

    public void removeNodeConnectors(String sp) {
        nodePorts.remove(sp);
    }

    public String toString() {
        return ("Subnet Config [Description=" + name + " Subnet=" + subnet
                + " NodeConnectors=" + nodePorts + "]");
    }
}
