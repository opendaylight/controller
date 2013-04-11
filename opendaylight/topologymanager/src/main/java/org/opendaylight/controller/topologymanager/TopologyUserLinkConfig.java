
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GUIField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface class that provides methods to manipulate user configured link
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class TopologyUserLinkConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String regexDatapathID = "^([0-9a-fA-F]{1,2}[:-]){7}[0-9a-fA-F]{1,2}$";
    private static final String regexDatapathIDLong = "^[0-9a-fA-F]{1,16}$";
    private static final String guiFields[] = { GUIField.STATUS.toString(),
            GUIField.NAME.toString(), GUIField.SRCNODE.toString(),
            GUIField.SRCPORT.toString(), GUIField.DSTNODE.toString(),
            GUIField.DSTPORT.toString() };
    private static final Logger logger = LoggerFactory
    .getLogger(TopologyUserLinkConfig.class);

    public enum STATUS {
        SUCCESS("Success"), LINKDOWN("Link Down"), INCORRECT(
                "Incorrect Connection");
        private STATUS(String name) {
            this.name = name;
        }

        private String name;

        public String toString() {
            return name;
        }

        public static STATUS fromString(String str) {
            if (str == null)
                return LINKDOWN;
            if (str.equals(SUCCESS.toString()))
                return SUCCESS;
            if (str.equals(LINKDOWN.toString()))
                return LINKDOWN;
            if (str.equals(INCORRECT.toString()))
                return INCORRECT;
            return LINKDOWN;
        }
    }

    @XmlElement
    private String status;
    @XmlElement
    private String name;
    @XmlElement
    private String srcNodeIDType;
    @XmlElement
    private String srcSwitchId;
    @XmlElement
    private String srcNodeConnectorIDType;
    @XmlElement
    private String srcPort;
    @XmlElement
    private String dstNodeIDType;
    @XmlElement
    private String dstSwitchId;
    @XmlElement
    private String dstNodeConnectorIDType;
    @XmlElement
    private String dstPort;

    public TopologyUserLinkConfig() {
        super();
        status = STATUS.LINKDOWN.toString();
    }

	public TopologyUserLinkConfig(String name, String srcNodeIDType,
			String srcSwitchId, String srcNodeConnectorIDType, String srcPort,
			String dstNodeIDType, String dstSwitchId,
			String dstNodeConnectorIDType, String dstPort) {
        super();
        this.name = name;
        this.srcNodeIDType = srcNodeIDType;
        this.srcSwitchId = srcSwitchId;
        this.dstNodeIDType = dstNodeIDType;
        this.dstSwitchId = dstSwitchId;
        this.srcNodeConnectorIDType = srcNodeConnectorIDType;
        this.srcPort = srcPort;
        this.dstNodeConnectorIDType = dstNodeConnectorIDType;
        this.dstPort = dstPort;
    }

    public String getSrcNodeIDType() {
		return srcNodeIDType;
	}

	public void setSrcNodeIDType(String srcNodeIDType) {
		this.srcNodeIDType = srcNodeIDType;
	}

	public String getSrcNodeConnectorIDType() {
		return srcNodeConnectorIDType;
	}

	public void setSrcNodeConnectorIDType(String srcNodeConnectorIDType) {
		this.srcNodeConnectorIDType = srcNodeConnectorIDType;
	}

	public String getDstNodeIDType() {
		return dstNodeIDType;
	}

	public void setDstNodeIDType(String dstNodeIDType) {
		this.dstNodeIDType = dstNodeIDType;
	}

	public String getDstNodeConnectorIDType() {
		return dstNodeConnectorIDType;
	}

	public void setDstNodeConnectorIDType(String dstNodeConnectorIDType) {
		this.dstNodeConnectorIDType = dstNodeConnectorIDType;
	}

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSrcSwitchId() {
        return srcSwitchId;
    }

    public long getSrcSwitchIDLong() {
        return getSwitchIDLong(srcSwitchId);
    }

    public void setSrcSwitchId(String srcSwitchId) {
        this.srcSwitchId = srcSwitchId;
    }

    public String getDstSwitchId() {
        return dstSwitchId;
    }

    public long getDstSwitchIDLong() {
        return getSwitchIDLong(dstSwitchId);
    }

    public void setDstSwitchId(String dstSwitchId) {
        this.dstSwitchId = dstSwitchId;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(String srcPort) {
        this.srcPort = srcPort;
    }

    public String getDstPort() {
        return dstPort;
    }

    public void setDstPort(String dstPort) {
        this.dstPort = dstPort;
    }

    public STATUS getStatus() {
        return STATUS.fromString(status);
    }

    public void setStatus(STATUS s) {
        this.status = s.toString();
    }

    private boolean isValidSwitchId(String switchId) {
        return (switchId != null && (switchId.matches(regexDatapathID) || switchId
                .matches(regexDatapathIDLong)));
    }

    private boolean isValidSwitchId(String switchId, String typeStr) {
        if (typeStr.equals(NodeIDType.OPENFLOW)) {
            return isValidSwitchId(switchId);
        } else if (typeStr.equals(NodeIDType.ONEPK) || 
        		   typeStr.equals(NodeIDType.PCEP) || 
        		   typeStr.equals(NodeIDType.PRODUCTION)) {
            return true;
        } else {
			logger.warn("Invalid node id type {}", typeStr);
        	return false;
        }
    }

    private boolean isValidPortId(String portId, String nodeConnectorType) {
		if (NodeConnectorIDType.getClassType(nodeConnectorType) == null) {
			logger.warn("Invalid node connector id type {}", nodeConnectorType);
			return false; 
		}
		
		return true;
	}

    private long getSwitchIDLong(String switchId) {
        int radix = 16;
        String switchString = "0";

        if (isValidSwitchId(switchId)) {
            if (switchId.contains(":")) {
                // Handle the 00:00:AA:BB:CC:DD:EE:FF notation
                switchString = switchId.replace(":", "");
            } else if (switchId.contains("-")) {
                // Handle the 00-00-AA-BB-CC-DD-EE-FF notation
                switchString = switchId.replace("-", "");
            } else {
                // Handle the 0123456789ABCDEF notation
                switchString = switchId;
            }
        }
        return Long.parseLong(switchString, radix);
    }

    public boolean isValid() {
		if (name == null || srcSwitchId == null || dstSwitchId == null
				|| srcPort == null || dstPort == null || srcNodeIDType == null
				|| dstNodeIDType == null || srcNodeConnectorIDType == null
				|| dstNodeConnectorIDType == null) {
            return false;
		}
		
		if (!isValidSwitchId(srcSwitchId, srcNodeIDType) || 
			!isValidSwitchId(dstSwitchId, dstNodeIDType)) {
			logger.warn("Invalid switch id");
			return false;
		}
		
		if (!isValidPortId(srcPort, srcNodeConnectorIDType) || 
			!isValidPortId(dstPort, dstNodeConnectorIDType)) {
			logger.warn("Invalid port id");
			return false;
		}
			
		return true;
    }

    public boolean isSrcPortByName() {
        try {
            Short.parseShort(srcPort);
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public boolean isDstPortByName() {
        try {
            Short.parseShort(dstPort);
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : guiFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    @Override
    public String toString() {
		return "ITopologyUserLinkConfig [status=" + status + ", name=" + name
				+ ", srcNodeIDType=" + srcNodeIDType + ", srcSwitchId="
				+ srcSwitchId + ", srcNodeConnectorIDType="
				+ srcNodeConnectorIDType + ", srcPort=" + srcPort
				+ ", dstNodeIDType=" + dstNodeIDType + ", dstId="
				+ dstSwitchId + ", dstNodeConnectorIDType="
				+ dstNodeConnectorIDType + ", dstPort=" + dstPort + "]";
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public boolean equals(Long srcNid, String srcPortName, Long dstNid,
            String dstPortName) {
        if (srcNid.equals(getSrcSwitchIDLong())
                && dstNid.equals(getDstSwitchIDLong())
                && srcPortName.equals(getSrcPort())
                && dstPortName.equals(getDstPort())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TopologyUserLinkConfig other = (TopologyUserLinkConfig) obj;
        if (dstPort == null) {
            if (other.dstPort != null)
                return false;
        } else if (!dstPort.equals(other.dstPort))
            return false;
        if (dstSwitchId == null) {
            if (other.dstSwitchId != null)
                return false;
        } else if (!dstSwitchId.equals(other.dstSwitchId))
            return false;
        if (srcPort == null) {
            if (other.srcPort != null)
                return false;
        } else if (!srcPort.equals(other.srcPort))
            return false;
        if (srcSwitchId == null) {
            if (other.srcSwitchId != null)
                return false;
        } else if (!srcSwitchId.equals(other.srcSwitchId))
            return false;
        return true;
    }
}
