
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   Node.java
 *
 * @brief  Describe a generic network element in multiple SDNs technologies
 *
 * Describe a generic network element in multiple SDNs technologies. A
 * Node is identified by the pair (NodeType, NodeID), the nodetype are
 * needed in order to further specify the nodeID
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.utils.HexEncode;

/**
 * Describe a generic network element in multiple SDNs technologies. A
 * Node is identified by the pair (NodeType, NodeID), the nodetype are
 * needed in order to further specify the nodeID
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enum-like static class created with the purpose of identifing
     * multiple type of nodes in the SDN network. The type is
     * necessary to figure out to later on correctly use the
     * nodeID. Using a static class instead of an Enum so we can add
     * dynamically new types without changing anything in the
     * surround.
     */
    public static final class NodeIDType {
        private static final ConcurrentHashMap<String, Class> compatibleType =
            new ConcurrentHashMap<String, Class>();
        /**
         * Identifier for an OpenFlow node
         */
        public static String OPENFLOW = "OF";
        /**
         * Identifier for a PCEP node
         */
        public static String PCEP = "PE";
        /**
         * Identifier for a ONEPK node
         */
        public static String ONEPK = "PK";
        /**
         * Identifier for a node in a non-SDN network
         */
        public static String PRODUCTION = "PR";

        // Pre-populated types, just here for convenience and ease of
        // unit-testing, but certainly those could live also outside.
        static {
            compatibleType.put(OPENFLOW, Long.class);
            compatibleType.put(PCEP, UUID.class);
            compatibleType.put(ONEPK, String.class);
            compatibleType.put(PRODUCTION, String.class);
        }

        /**
         * Return the type of the class expected for the
         * NodeID, it's used for validity check in the constructor
         *
         * @param nodeType the type of the node we want to check
         * compatibility for
         *
         * @return The Class which is supposed to instantiate the ID
         * for the NodeID
         */
        public static Class<?> getClassType(String nodeType) {
            return compatibleType.get(nodeType);
        }

        /**
         * Returns all the registered nodeIDTypes currently available
         *
         * @return The current registered NodeIDTypes
         */
        public static Set<String> values() {
            return compatibleType.keySet();
        }

        /**
         * Register a new ID for which Node can be created
         *
         * @param type, the new type being registered
         * @param compatibleID, the type of class to be accepted as ID
         *
         * @return true if registered, false otherwise
         */
        public static boolean registerIDType(String type,
                                             Class compatibleID) {
            if (compatibleType.get(type) != null) {
                return false;
            }  else {
                compatibleType.put(type, compatibleID);
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

    // This is the identity of the Node a (Type,ID) pair!, the full
    // essence of this class.
    private Object nodeID;
    private String nodeType;

    // Shadow value for unmarshalling
    private String nodeIDString;

    /**
     * Private constructor used for JAXB mapping
     */
    private Node() {
        this.nodeID = null;
        this.nodeType = null;
        this.nodeIDString = null;
    }

    /**
     * Constructor for the Node objects, it validate the input so if
     * the ID passed is not of the type expected accordingly to the
     * type an exception is raised.
     *
     * @param nodeType Type of the node we are building
     * @param id ID used by the SDN technology to identify the node
     *
     */
    public Node(String nodeType, Object id) throws ConstructionException {
        if (NodeIDType.getClassType(nodeType) != null &&
            NodeIDType.getClassType(nodeType).isInstance(id)) {
            this.nodeType = nodeType;
            this.nodeID = id;
        } else {
            throw new ConstructionException("Type of incoming object:"
                                            + id.getClass() + " not compatible with expected type:"
                                            + NodeIDType.getClassType(nodeType));
        }
    }

    /**
     * Copy Constructor for the Node objects.
     *
     * @param src type of nodes to copy from
     *
     */
    public Node(Node src) throws ConstructionException {
        if (src != null) {
            this.nodeType = src.getType();
            // Here we can reference the object because that is
            // supposed to be an immutable identifier as well like a
            // UUID/Integer and so on, hence no need to create a copy
            // of it
            this.nodeID = src.getID();
        } else {
            throw
                new ConstructionException("Null incoming object to copy from");
        }
    }

    /**
     * getter for node type
     *
     *
     * @return The node Type for this Node object
     */
    @XmlAttribute(name = "type")
    public String getType() {
        return this.nodeType;
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

        this.nodeType = typeStr;
        if (typeStr.equals(NodeIDType.OPENFLOW)) {
            this.nodeID = Long.valueOf(HexEncode.stringToLong(IDStr));
        } else if (typeStr.equals(NodeIDType.ONEPK)) {
            this.nodeID = IDStr;
        } else if (typeStr.equals(NodeIDType.PCEP)) {
            this.nodeID = UUID.fromString(IDStr);
        } else if (typeStr.equals(NodeIDType.PRODUCTION)) {
            this.nodeID = IDStr;
        } else {
            // We need to lookup via OSGi service registry for an
            // handler for this
        }
    }

    /** 
     * Private setter for nodeType to be called by JAXB not by anyone
     * else, Node is immutable
     * 
     * @param type of node to be set
     */
    private void setType(String type) {
        this.nodeType = type;
        if (this.nodeIDString != null) {
            this.fillmeFromString(type, this.nodeIDString);
        }
    }

    /**
     * getter for node ID
     *
     *
     * @return The node ID for this Node object
     */
    public Object getID() {
        return this.nodeID;
    }

    /**
     * Getter for the node ID in string format
     *
     * @return The nodeID in string format
     */
    @XmlAttribute(name = "id")
    public String getNodeIDString() {
        if (this.nodeType.equals(NodeIDType.OPENFLOW)) {
            return HexEncode.longToHexString((Long) this.nodeID);
        } else {
            return this.nodeID.toString();
        }
    }
    
    /** 
     * private setter to be used by JAXB
     * 
     * @param nodeIDString String representation for NodeID
     */
    private void setNodeIDString(String nodeIDString) {
        this.nodeIDString = nodeIDString;
        if (this.nodeType != null) {
            this.fillmeFromString(this.nodeType, nodeIDString);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(163841, 56473)
            .append(nodeType)
            .append(nodeID)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Node rhs = (Node)obj;
        return new EqualsBuilder()
            .append(this.getType(), rhs.getType())
            .append(this.getID(), rhs.getID())
            .isEquals();
    }

    @Override
    public String toString() {
        if (this.nodeType.equals(NodeIDType.OPENFLOW)) {
            return this.nodeType.toString() + "|"
                + HexEncode.longToHexString((Long) this.nodeID);
        } else {
            return this.nodeType.toString() + "|" + this.nodeID.toString();
        }
    }

    /**
     * Static method to get back a Node from a string
     *
     * @param str string formatted in toString mode that can be
     * converted back to a Node format.
     *
     * @return a Node if succed or null if no
     */
    public static Node fromString(String str) {
        if (str == null) {
            return null;
        }

        String parts[] = str.split("\\|");
        if (parts.length != 2) {
            // Try to guess from a String formatted as a long because
            // for long time openflow has been prime citizen so lets
            // keep this legacy for now
            String numStr = str.toUpperCase();

            Long ofNodeID = null;
            if (numStr.startsWith("0X")) {
                // Try as an hex number
                try {
                    BigInteger b = new BigInteger(
                        numStr.replaceFirst("0X", ""), 16);
                    ofNodeID = Long.valueOf(b.longValue());
                } catch (Exception ex) {
                    ofNodeID = null;
                }
            } else {
                // Try as a decimal number
                try {
                    BigInteger b = new BigInteger(numStr);
                    ofNodeID = Long.valueOf(b.longValue());
                } catch (Exception ex) {
                    ofNodeID = null;
                }
            }

            // Startegy #3 parse as HexLong
            if (ofNodeID == null) {
                try {
                    ofNodeID = Long.valueOf(HexEncode.stringToLong(numStr));
                } catch (Exception ex) {
                    ofNodeID = null;
                }
            }

            // We ran out of ideas ... return null
            if (ofNodeID == null) {
                return null;
            }

            // Lets return the cooked up NodeID
            try {
                return new Node(NodeIDType.OPENFLOW, ofNodeID);
            } catch (ConstructionException ex) {
                return null;
            }
        }

        String typeStr = parts[0];
        String IDStr = parts[1];

        return fromString(typeStr, IDStr);
    }

    /**
     * Static method to get back a Node from a pair of strings, the
     * first one being the Type representation, the second one being
     * the ID string representation, expected to be heavily used in
     * northbound API.
     *
     * @param type, the type of the node we are parsing
     * @param id, the string representation of the node id
     *
     * @return a Node if succed or null if no
     */
    public static Node fromString(String typeStr, String IDStr) {
        if (typeStr == null) {
            return null;
        }

        if (IDStr == null) {
            return null;
        }

        if (typeStr.equals(NodeIDType.OPENFLOW)) {
            try {
                Long ID = Long.valueOf(HexEncode.stringToLong(IDStr));
                return new Node(typeStr, ID);
            } catch (Exception ex) {
                return null;
            }
        } else if (typeStr.equals(NodeIDType.ONEPK)) {
            try {
                return new Node(typeStr, IDStr);
            } catch (Exception ex) {
                return null;
            }
        } else if (typeStr.equals(NodeIDType.PCEP)) {
            try {
                UUID ID = UUID.fromString(IDStr);
                return new Node(typeStr, ID);
            } catch (Exception ex) {
                return null;
            }
        } else if (typeStr.equals(NodeIDType.PRODUCTION)) {
            try {
                return new Node(typeStr, IDStr);
            } catch (Exception ex) {
                return null;
            }
        } else {
            // We need to lookup via OSGi service registry for an
            // handler for this
        }
        return null;
    }
}
