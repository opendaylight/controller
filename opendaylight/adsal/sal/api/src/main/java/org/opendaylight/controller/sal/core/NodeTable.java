/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NodeTable implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final Short SPECIALNODETABLEID = (short) 0;

    /**
     * Enum-like static class created with the purpose of identifing
     * multiple type of nodes in the SDN network. The type is
     * necessary to figure out to later on correctly use the
     * nodeTableID. Using a static class instead of an Enum so we can add
     * dynamically new types without changing anything in the
     * surround.
     */
    public static final class NodeTableIDType {
        private static final ConcurrentHashMap<String, Class<? extends Object>> compatibleType =
                new ConcurrentHashMap<String, Class<? extends Object>>();
        /**
         * These are in compliance with the compatibility types in 'Node'
         */
        public static String OPENFLOW = "OF";
        public static String PCEP = "PE";
        public static String ONEPK = "PK";
        public static String PRODUCTION = "PR";

        // Pre-populated types, just here for convenience and ease of
        // unit-testing, but certainly those could live also outside.
        // Currently we allow these 4. It can be changed later.
        static {
            compatibleType.put(OPENFLOW, Byte.class);
            compatibleType.put(PCEP, UUID.class);
            compatibleType.put(ONEPK, String.class);
            compatibleType.put(PRODUCTION, String.class);
        }

        /**
         * Return the type of the class expected for the
         * NodeTableID, it's used for validity check in the constructor
         *
         * @param type, the type of the NodeTable for which we
         * want to retrieve the compatible class to be used as ID.
         *
         * @return The Class which is supposed to instantiate the ID
         * for the NodeTableID
         */
        public static Class<?> getClassType(String type) {
            return compatibleType.get(type);
        }

        /**
         * Returns all the registered nodeTableIDTypes currently available
         *
         * @return The current registered NodeTableIDTypes
         */
        public static Set<String> values() {
            return compatibleType.keySet();
        }

        /**
         * Register a new ID for which NodeTable can be created
         *
         * @param type, the new type being registered
         * @param compatibleID, the type of class to be accepted as ID
         *
         * @return true if registered, false otherwise
         */
        public static boolean registerIDType(String type,
                Class<? extends Object> compatibleID) {
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

    // Elements that constitute the NodeTable
    private Object nodeTableID;
    private String nodeTableType;
    @XmlElement(name = "node")
    private Node nodeTableNode;

    // Helper field for JAXB
    private String nodeTableIDString;

    /**
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private NodeTable() {
        this.nodeTableIDString = null;
        this.nodeTableID = null;
        this.nodeTableType = null;
        this.nodeTableNode = null;
    }

    public NodeTable(String nodeTableType, Object id, Node node) throws ConstructionException {
        if (NodeTableIDType.getClassType(nodeTableType) != null &&
                NodeTableIDType.getClassType(nodeTableType).isInstance(id) &&
                node.getType().equals(nodeTableType)) {
            this.nodeTableType = nodeTableType;
            this.nodeTableID = id;
            this.nodeTableNode = node;
        } else {
            throw new ConstructionException("Type of incoming object:"
                    + id.getClass() + " not compatible with expected type:"
                    + NodeTableIDType.getClassType(nodeTableType)
                    + " or Node type incompatible:" + node.getType());
        }
    }

    /**
     * Copy constructor for NodeTable
     *
     * @param src NodeTable to copy from
     *
     */
    public NodeTable(NodeTable src) throws ConstructionException {
        if (src != null) {
            this.nodeTableType = src.getType();
            // Here we can reference the object because that is
            // supposed to be an immutable identifier as well like a
            // UUID/Integer and so on, hence no need to create a copy
            // of it
            this.nodeTableID = src.getID();
            this.nodeTableNode = new Node(src.getNode());
        } else {
            throw
            new ConstructionException("Null incoming object to copy from");
        }
    }

    /**
     * @return the nodeTableID
     */
    public Object getID() {
        return this.nodeTableID;
    }

    /**
     * @return the nodeTableType
     */
    public String getType() {
        return this.nodeTableType;
    }

    /**
     * Private setter for nodeConnectorType to be called by JAXB not by anyone
     * else, NodeConnector is immutable
     *
     * @param type the nodeTableType to set
     */
    @SuppressWarnings("unused")
    private void setType(String type) {
        this.nodeTableType = type;
        if (this.nodeTableIDString != null) {
            this.fillmeFromString(type, this.nodeTableIDString);
        }
    }

    /**
     * @return the nodeTableNode
     */
    public Node getNode() {
        return this.nodeTableNode;
    }

    /**
     * @param nodeTableNode the nodeTableNode to set
     */
    public void setNodeTableNode(Node nodeTableNode) {
        this.nodeTableNode = nodeTableNode;
    }

    /**
     * @return the nodeTableIDString
     */
    @XmlElement(name = "id")
    public String getNodeTableIDString() {
        return this.nodeTableIDString != null? this.nodeTableIDString : nodeTableID.toString();
    }

    /**
     * @param nodeTableIDString the nodeTableIDString to set
     */
    @SuppressWarnings("unused")
    private void setNodeTableIDString(String IDStr) {
        this.nodeTableIDString = IDStr;
        if (this.nodeTableType != null) {
            this.fillmeFromString(this.nodeTableType, IDStr);
        }
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

        this.nodeTableType = typeStr;
        this.nodeTableID = (byte) Byte.parseByte(IDStr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((nodeTableID == null) ? 0 : nodeTableID.hashCode());
        result = prime
                * result
                + ((nodeTableNode == null) ? 0 : nodeTableNode
                        .hashCode());
        result = prime
                * result
                + ((nodeTableType == null) ? 0 : nodeTableType
                        .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeTable other = (NodeTable) obj;
        if (nodeTableID == null) {
            if (other.nodeTableID != null)
                return false;
        } else if (!nodeTableID.equals(other.nodeTableID))
            return false;
        if (nodeTableNode == null) {
            if (other.nodeTableNode != null)
                return false;
        } else if (!nodeTableNode.equals(other.nodeTableNode))
            return false;
        if (nodeTableType == null) {
            if (other.nodeTableType != null)
                return false;
        } else if (!nodeTableType.equals(other.nodeTableType))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return this.getNodeTableIdAsString() + "@" + this.nodeTableNode;
    }

    public String getNodeTableIdAsString() {
        return this.nodeTableType + "|"
                + this.nodeTableID.toString();
    }

}
