/**
 * 
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author adityavaja
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NodeTable implements Serializable {

	private static final long serialVersionUID = 1L;
    public static final Short SPECIALNODETABLEID = (short) 0;
    
    /**
     * Enum-like static class created with the purpose of identifing
     * multiple type of nodes in the SDN network. The type is
     * necessary to figure out to later on correctly use the
     * nodeID. Using a static class instead of an Enum so we can add
     * dynamically new types without changing anything in the
     * surround.
     */
    public static final class NodeTableIDType {
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
        public static String OPENFLOW = "OF";
        public static String PCEP = "PE";
        public static String ONEPK = "PK";
        public static String PRODUCTION = "PR";

        // Pre-populated types, just here for convenience and ease of
        // unit-testing, but certainly those could live also outside.
        static {
			 compatibleType.put(OPENFLOW,
					 new ImmutablePair(Short.class, Node.NodeIDType.OPENFLOW));
			 compatibleType.put(PCEP,
					 new ImmutablePair(Integer.class, Node.NodeIDType.PCEP));
			 compatibleType.put(ONEPK,
					 new ImmutablePair(String.class, Node.NodeIDType.ONEPK));
			 compatibleType.put(PRODUCTION,
					 new ImmutablePair(String.class, Node.NodeIDType.PRODUCTION));
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
            if (compatibleType.get(type) == null) {
                return null;
            }
            return compatibleType.get(type).getLeft();
        }
        
        /**
         * Return the NodeIDType compatible with this NodeTable,
         * in fact you cannot attach for example a PCEP NodeTable
         * to an OpenFlow Node.
         *
         * @param type, the type of the NodeTable for which we
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
         * @param compatibleNode, the type of Node with which this
         * NodeTable is compatible
         *
         * @return true if registered, false otherwise
         */
        public static boolean registerIDType(String type,
                                             Class<? extends Object> compatibleID,
                                             String compatibleNode) {
            if (compatibleType.get(type) != null) {
                return false;
            }  else {
                compatibleType.put(type,
                		new ImmutablePair(compatibleID, compatibleNode));
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
    private NodeTable() {
        this.nodeTableIDString = null;
        this.nodeTableID = null;
        this.nodeTableType = null;
        this.nodeTableNode = null;
    }    
    
	public NodeTable(String nodeTableType, Object id, Node node) throws ConstructionException {
		
        if (node.getType().equals(nodeTableType) && id != null) {
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
	 * @param type the nodeTableType to set
	 * 
     * Private setter for nodeConnectorType to be called by JAXB not by anyone
     * else, NodeConnector is immutable
	 */
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
	@XmlAttribute(name = "id")
	public String getNodeTableIDString() {
		return this.nodeTableIDString.toString();
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
        return new HashCodeBuilder(64489, 4961)
            .append(nodeTableType)
            .append(nodeTableID)
            .append(nodeTableNode)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        NodeTable rhs = (NodeTable)obj;
        return new EqualsBuilder()
            .append(this.getType(), rhs.getType())
            .append(this.getID(), rhs.getID())
            .append(this.getNode(), rhs.getNode())
            .isEquals();
    }

    @Override
    public String toString() {
        return this.getNodeTableIdAsString() + "@" + this.nodeTableNode;
    }

	public String getNodeTableIdAsString() {
		return this.nodeTableType.toString() + "|"
                        + this.nodeTableID.toString();
	}

}
