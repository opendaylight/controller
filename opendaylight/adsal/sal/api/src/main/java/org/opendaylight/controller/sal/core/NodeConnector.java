
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   NodeConnector.java
 *
 * @brief  Describe a generic network element attachment points,
 * attached to one Node, the NodeConnector is formed by the pair
 * (NodeConnectorType, NodeConnectorID) because each SDN technlogy can
 * identify an attachment point on the Node in different way.
 *
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.utils.ServiceHelper;

/**
 * Describe a generic network element attachment points,
 * attached to one Node, the NodeConnector is formed by the pair
 * (NodeConnectorType, NodeConnectorID) because each SDN technology can
 * identify an attachment point on the Node in different way.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NodeConnector implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Short SPECIALNODECONNECTORID = (short) 0;

    /**
     * Enumerate the different types of NodeConnectors supported by the class
     *
     */
    public static class NodeConnectorIDType {
        private static final
        ConcurrentHashMap<String, ImmutablePair<Class<? extends Object>, String>> compatibleType =
            new ConcurrentHashMap<String, ImmutablePair<Class<? extends Object>, String>>();
        /**
         * Represents the OFPP_CONTROLLER reserved port to forward a
         * packet to the controller, this is to send data packets
         * to the controller from the data plane triggering
         * a packet_in event.
         */
        public static String CONTROLLER = "CTRL";
        /**
         * Represents the OFPP_ALL reserved OF port
         * to forward to ALL the ports in the system ,
         * should be used for flooding like mechanism to
         * be used cautiously to avoid excessive flooding.
         */
        public static String ALL = "ALL";
        /**
         * Represents the OFPP_LOCAL reserved OF port
         * to access the local networking stack of the node
         * of which the packet is destined. Typically used for
         * inband OF communications channel.
         */
        public static String SWSTACK = "SW";
        /**
         * Describes OFPP_Normal reserved port destination that invokes
         * the traditional native L2/L3 HW normal forwarding functionality
         * if supported on the forwarding element.
         */
        public static String HWPATH = "HW";
        public static String OPENFLOW = "OF";
        public static String PCEP = "PE";
        public static String ONEPK = "PK";
        public static String OPENFLOW2PCEP = "O2E";
        public static String PCEP2OPENFLOW = "E2O";
        public static String OPENFLOW2ONEPK = "O2K";
        public static String ONEPK2OPENFLOW = "K2O";
        public static String PCEP2ONEPK = "E2K";
        public static String ONEPK2PCEP = "K2E";
        public static String PRODUCTION = "PR";

        // Initialize the map with some well known, even though all of
        // them could be siting outside of here, but it's convenient
        // for Unit Testing coverage
        static {
            compatibleType.put(CONTROLLER,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class, null));
            compatibleType.put(ALL,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class, null));
            compatibleType.put(SWSTACK,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class, null));
            compatibleType.put(HWPATH,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class, null));
            compatibleType.put(OPENFLOW,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class,
                                                 Node.NodeIDType.OPENFLOW));
            compatibleType.put(PCEP,
                               new ImmutablePair<Class<? extends Object>, String>(Integer.class,
                                                 Node.NodeIDType.PCEP));
            compatibleType.put(ONEPK,
                               new ImmutablePair<Class<? extends Object>, String>(String.class,
                                                 Node.NodeIDType.ONEPK));
            compatibleType.put(OPENFLOW2PCEP,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class,
                                                 Node.NodeIDType.OPENFLOW));
            compatibleType.put(OPENFLOW2ONEPK,
                               new ImmutablePair<Class<? extends Object>, String>(Short.class,
                                                 Node.NodeIDType.OPENFLOW));
            compatibleType.put(PCEP2OPENFLOW,
                               new ImmutablePair<Class<? extends Object>, String>(Integer.class,
                                                 Node.NodeIDType.PCEP));
            compatibleType.put(PCEP2ONEPK,
                               new ImmutablePair<Class<? extends Object>, String>(Integer.class,
                                                 Node.NodeIDType.PCEP));
            compatibleType.put(ONEPK2OPENFLOW,
                               new ImmutablePair<Class<? extends Object>, String>(String.class,
                                                 Node.NodeIDType.ONEPK));
            compatibleType.put(ONEPK2PCEP,
                               new ImmutablePair<Class<? extends Object>, String>(String.class,
                                                 Node.NodeIDType.ONEPK));
            compatibleType.put(PRODUCTION,
                               new ImmutablePair<Class<? extends Object>, String>(String.class,
                                                 Node.NodeIDType.PRODUCTION));
        }

        /**
         * Return the type of the class expected for the
         * NodeConnectorID, it's used for validity check in the constructor
         *
         * @param type, the type of the NodeConnector for which we
         * want to retrieve the compatible class to be used as ID.
         *
         * @return The Class which is supposed to instantiate the ID
         * for the NodeConnectorID
         */
        public static Class<?> getClassType(String type) {
            if (compatibleType.get(type) == null) {
                return null;
            }
            return compatibleType.get(type).getLeft();
        }

        /**
         * Return the NodeIDType compatible with this NodeConnector,
         * in fact you cannot attach for example a PCEP NodeConnector
         * to an OpenFlow Node.
         *
         * @param type, the type of the NodeConnector for which we
         * want to retrieve the compatible class to be used as ID.
         *
         * @return The ID of the compatible Node
         */
        public static String getCompatibleNode(String type) {
            if (compatibleType.get(type) == null) {
                return null;
            }
            return compatibleType.get(type).getRight();
        }

        /**
         * Register a new ID for which Node can be created
         *
         * @param type, the new type being registered
         * @param compatibleID, the type of class to be accepted as ID
         * @param compatibleNode, the type of Node with which this
         * NodeConnector is compatible
         *
         * @return true if registered, false otherwise
         */
        public static boolean registerIDType(String type,
                                             Class<? extends Object> compatibleID,
                                             String compatibleNode) {
            if (compatibleType.get(type) != null) {
                return false;
            }  else {
                compatibleType.put(type, new ImmutablePair<Class<? extends Object>, String>(compatibleID,
                                                           compatibleNode));
                return true;
            }
        }

        /**
         * UNRegister a new ID for which Node can be created
         *
         * @param type, the type being UN-registered
         *
         */
        public static void unRegisterIDType(String type) {
            compatibleType.remove(type);
        }
    }

    // Elements that constitute the NodeConnector
    private Object nodeConnectorID;
    private String nodeConnectorType;
    @XmlElement(name = "node")
    private Node nodeConnectorNode;

    // Helper field for JAXB
    private String nodeConnectorIDString;

    /**
     * Private constructor used for JAXB mapping
     */
    private NodeConnector() {
        this.nodeConnectorIDString = null;
        this.nodeConnectorID = null;
        this.nodeConnectorType = null;
        this.nodeConnectorNode = null;
    }

    /**
     * Create a NodeConnector from the component element. The
     * constructor make sure the NodeConnector type is congruent with
     * the Node used and also the NodeConnector ID is of type expected
     *
     * @param nodeConnectorType Type of the NodeConnector
     * @param id ID portion of the NodeConnector
     * @param node Node to which the NodeConnector is attached too
     *
     */
    public NodeConnector(String nodeConnectorType, Object id,
            Node node) throws ConstructionException {
        // In case of compatible type being null then assume that this
        // port can be attached on any node.
        String compatibleNode =
            NodeConnectorIDType.getCompatibleNode(nodeConnectorType);
        if (NodeConnectorIDType.getClassType(nodeConnectorType) != null &&
            NodeConnectorIDType.getClassType(nodeConnectorType).isInstance(id) &&
            (compatibleNode == null ||
             node.getType().equals(compatibleNode))) {
            this.nodeConnectorType = nodeConnectorType;
            this.nodeConnectorID = id;
            this.nodeConnectorNode = node;
        } else {
            throw new ConstructionException("Type of incoming object:"
                    + id.getClass() + " not compatible with expected type:"
                    + NodeConnectorIDType.getClassType(nodeConnectorType)
                    + " or Node type incompatible:" + node.getType());
        }
    }

    /**
     * Copy constructor for NodeConnector
     *
     * @param src NodeConnector to copy from
     *
     */
    public NodeConnector(NodeConnector src) throws ConstructionException {
        if (src != null) {
            this.nodeConnectorType = src.getType();
            // Here we can reference the object because that is
            // supposed to be an immutable identifier as well like a
            // UUID/Integer and so on, hence no need to create a copy
            // of it
            this.nodeConnectorID = src.getID();
            this.nodeConnectorNode = new Node(src.getNode());
        } else {
            throw
                new ConstructionException("Null incoming object to copy from");
        }
    }

    /**
     * getter method for NodeConnector
     *
     *
     * @return the NodeConnectorType of this object
     */
    @XmlElement(name = "type")
    public String getType() {
        return this.nodeConnectorType;
    }

    /**
     * fill the current object from the string parameters passed, will
     * be only used by JAXB
     *
     * @param typeStr string representing the type of the Node
     * @param IDStr String representation of the ID
     */
    private void fillmeFromString(String typeStr, String IDStr) {
        if (typeStr == null) {
            return;
        }

        if (IDStr == null) {
            return;
        }

        this.nodeConnectorType = typeStr;
        if (typeStr.equals(NodeConnectorIDType.OPENFLOW) ||
            typeStr.equals(NodeConnectorIDType.OPENFLOW2ONEPK) ||
            typeStr.equals(NodeConnectorIDType.OPENFLOW2PCEP)) {
            try {
                Short ID = Short.parseShort(IDStr);
                this.nodeConnectorID = ID;
            } catch (Exception ex) {
                return;
            }
        } else if (typeStr.equals(NodeConnectorIDType.ONEPK) ||
                   typeStr.equals(NodeConnectorIDType.ONEPK2OPENFLOW) ||
                   typeStr.equals(NodeConnectorIDType.ONEPK2PCEP) ||
                   typeStr.equals(NodeConnectorIDType.PRODUCTION)) {
            try {
                this.nodeConnectorID = IDStr;
            } catch (Exception ex) {
                return;
            }
        } else if (typeStr.equals(NodeConnectorIDType.PCEP) ||
                   typeStr.equals(NodeConnectorIDType.PCEP2ONEPK) ||
                   typeStr.equals(NodeConnectorIDType.PCEP2OPENFLOW)) {
            try {
                Integer ID = Integer.parseInt(IDStr);
                this.nodeConnectorID = ID;
            } catch (Exception ex) {
                return;
            }
        } else {
            // Lookup via OSGi service registry
        }
    }

    /**
     * Private setter for nodeConnectorType to be called by JAXB not by anyone
     * else, NodeConnector is immutable
     *
     * @param type of node to be set
     */
    @SuppressWarnings("unused")
    private void setType(String type) {
        this.nodeConnectorType = type;
        if (this.nodeConnectorIDString != null) {
            this.fillmeFromString(type, this.nodeConnectorIDString);
        }
    }

    /**
     * getter method for NodeConnector
     *
     *
     * @return the NodeConnector ID of this object
     */
    public Object getID() {
        return this.nodeConnectorID;
    }

    /**
     * getter method for NodeConnector ID in string format.
     *
     *
     * @return the NodeConnector ID of this object in String format
     */
    @XmlElement(name = "id")
    public String getNodeConnectorIDString() {
        return this.nodeConnectorID.toString();
    }

    /**
     * private setter to be used by JAXB
     *
     * @param nodeConnectorIDString String representation for NodeConnectorID
     */
    @SuppressWarnings("unused")
    private void setNodeConnectorIDString(String IDStr) {
        this.nodeConnectorIDString = IDStr;
        if (this.nodeConnectorType != null) {
            this.fillmeFromString(this.nodeConnectorType, IDStr);
        }
    }

    /**
     * getter method for NodeConnector
     *
     *
     * @return the Node of this object
     */
    public Node getNode() {
        return this.nodeConnectorNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((nodeConnectorID == null) ? 0 : nodeConnectorID.hashCode());
        result = prime
                * result
                + ((nodeConnectorNode == null) ? 0 : nodeConnectorNode
                        .hashCode());
        result = prime
                * result
                + ((nodeConnectorType == null) ? 0 : nodeConnectorType
                        .hashCode());
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
        NodeConnector other = (NodeConnector) obj;
        if (nodeConnectorID == null) {
            if (other.nodeConnectorID != null) {
                return false;
            }
        } else if (!nodeConnectorID.equals(other.nodeConnectorID)) {
            return false;
        }
        if (nodeConnectorNode == null) {
            if (other.nodeConnectorNode != null) {
                return false;
            }
        } else if (!nodeConnectorNode.equals(other.nodeConnectorNode)) {
            return false;
        }
        if (nodeConnectorType == null) {
            if (other.nodeConnectorType != null) {
                return false;
            }
        } else if (!nodeConnectorType.equals(other.nodeConnectorType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getNodeConnectorIdAsString() + "@" + this.nodeConnectorNode;
    }

    /**
     * A String representation of the NodeConnector without
     * the Node context
     *
     * @return A String representation of the NodeConnector without
     * the Node context
     */
    public String getNodeConnectorIdAsString() {
        if (this.nodeConnectorType
            .equals(NodeConnectorIDType.CONTROLLER) ||
            this.nodeConnectorType
            .equals(NodeConnectorIDType.ALL) ||
            this.nodeConnectorType
            .equals(NodeConnectorIDType.SWSTACK) ||
            this.nodeConnectorType
            .equals(NodeConnectorIDType.HWPATH)) {
            return this.nodeConnectorType;
        } else {
            return this.nodeConnectorType + "|"
                    + this.nodeConnectorID.toString();
        }
    }

    /**
     * return a NodeConnector from a string
     *
     * @param str String to be parsed in a NodeConnector
     *
     * @return the NodeConnector if parse is successful, null otherwise
     */
    public static NodeConnector fromString(String str) {
        if (str == null) {
            return null;
        }
        String parts[] = str.split("\\@");
        if (parts.length != 2) {
            return null;
        }
        // Now get the Node from the Node portion
        Node n = Node.fromString(parts[1]);
        if (n == null) {
            return null;
        }
        return fromStringNoNode(parts[0], n);
    }

    /**
     * return a set of NodeConnector from a collection of string
     *
     * @param stringCollection Collection of String object to be parsed as a NodeConnector
     *
     * @return the Set of unique NodeConnector objects if parse is successful, empty Set otherwise
     */
    public static Set<NodeConnector> fromString(Collection<String> stringCollection) {
        Set<NodeConnector> set = new HashSet<NodeConnector>();
        if (stringCollection != null) {

            for (String str : stringCollection) {
                NodeConnector nodeConnector = NodeConnector.fromString(str);
                if (nodeConnector != null) {
                    set.add(nodeConnector);
                }
            }
        }
        return set;
    }

    /**
     * return a NodeConnector from a string not containing explicitly
     * the Node portion which has to be supplied as parameter
     *
     * @param str String to be parsed in a NodeConnector
     * @param n Node to which the NodeConnector is attached
     *
     * @return the NodeConnector if parse is successful, null otherwise
     */
    public static NodeConnector fromStringNoNode(String str, Node n) {
        if (str == null) {
            return null;
        }
        String nodeConnectorParts[] = str.split("\\|");
        if (nodeConnectorParts.length != 2) {
            // Try to guess from a String formatted as a short because
            // for long time openflow has been prime citizen so lets
            // keep this legacy for now
            String numStr = str.toUpperCase();

            Short ofPortID = null;
            // Try as an decimal/hex number
            try {
                ofPortID = Short.decode(numStr);
            } catch (Exception ex) {
                ofPortID = null;
            }

            // Lets try the special ports we know about
            if (ofPortID == null) {
                try {
                    if (str.equalsIgnoreCase(NodeConnectorIDType.CONTROLLER
                            .toString())) {
                        return new NodeConnector(
                                NodeConnectorIDType.CONTROLLER,
                                SPECIALNODECONNECTORID, n);
                    }
                    if (str.equalsIgnoreCase(NodeConnectorIDType.HWPATH
                            .toString())) {
                        return new NodeConnector(NodeConnectorIDType.HWPATH,
                                SPECIALNODECONNECTORID, n);
                    }
                    if (str.equalsIgnoreCase(NodeConnectorIDType.SWSTACK
                            .toString())) {
                        return new NodeConnector(NodeConnectorIDType.SWSTACK,
                                SPECIALNODECONNECTORID, n);
                    }
                    if (str
                            .equalsIgnoreCase(NodeConnectorIDType.ALL
                                    .toString())) {
                        return new NodeConnector(NodeConnectorIDType.ALL,
                                SPECIALNODECONNECTORID, n);
                    }
                } catch (ConstructionException ex) {
                    return null;
                }
                return null;
            }

            // Lets return the cooked up NodeID
            try {
                return new NodeConnector(NodeConnectorIDType.OPENFLOW,
                        ofPortID, n);
            } catch (ConstructionException ex) {
                return null;
            }
        }

        String typeStr = nodeConnectorParts[0];
        String IDStr = nodeConnectorParts[1];
        return fromStringNoNode(typeStr, IDStr, n);
    }

    /**
     * return a NodeConnector from a pair (type, ID) in string format
     * not containing explicitly the Node portion which has to be
     * supplied as parameter
     *
     * @param typeStr type String to be parsed in a NodeConnector
     * @param IDStr ID String portion to be parsed in a NodeConnector
     * @param n Node to which the NodeConnector is attached
     *
     * @return the NodeConnector if parse is successful, null otherwise
     */
    public static NodeConnector fromStringNoNode(String typeStr, String IDStr,
                                                 Node n) {
        if (typeStr == null) {
            return null;
        }
        if (IDStr == null) {
            return null;
        }

        if (typeStr.equals(NodeConnectorIDType.OPENFLOW) ||
            typeStr.equals(NodeConnectorIDType.OPENFLOW2ONEPK) ||
            typeStr.equals(NodeConnectorIDType.OPENFLOW2PCEP)) {
            try {
                Short ID = Short.parseShort(IDStr);
                return new NodeConnector(typeStr, ID, n);
            } catch (Exception ex) {
                return null;
            }
        } else if (typeStr.equals(NodeConnectorIDType.ONEPK) ||
                   typeStr.equals(NodeConnectorIDType.ONEPK2OPENFLOW) ||
                   typeStr.equals(NodeConnectorIDType.ONEPK2PCEP) ||
                   typeStr.equals(NodeConnectorIDType.PRODUCTION)) {
            try {
                return new NodeConnector(typeStr, IDStr, n);
            } catch (Exception ex) {
                return null;
            }
        } else if (typeStr.equals(NodeConnectorIDType.PCEP) ||
                   typeStr.equals(NodeConnectorIDType.PCEP2ONEPK) ||
                   typeStr.equals(NodeConnectorIDType.PCEP2OPENFLOW)) {
            try {
                Integer ID = Integer.parseInt(IDStr);
                return new NodeConnector(typeStr, ID, n);
            } catch (Exception ex) {
                return null;
            }
        } else {
            //Use INodeConnectorFactory to create a NodeConnector of registered type.
            //The protocol plugin being used depends on typeStr.
            INodeConnectorFactory f = (INodeConnectorFactory) ServiceHelper
                    .getGlobalInstance(INodeConnectorFactory.class, new NodeConnector(), "(protocolName="+typeStr+")");
            if(f==null) {
                return null;
            }
            return f.fromStringNoNode(typeStr, IDStr, n);
        }
    }
}
