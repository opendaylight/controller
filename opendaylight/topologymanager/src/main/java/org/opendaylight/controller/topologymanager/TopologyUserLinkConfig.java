
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

import org.opendaylight.controller.sal.utils.GUIField;

/**
 * Interface class that provides methods to manipulate user configured link
 */
public class TopologyUserLinkConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String regexDatapathID = "^([0-9a-fA-F]{1,2}[:-]){7}[0-9a-fA-F]{1,2}$";
    private static final String regexDatapathIDLong = "^[0-9a-fA-F]{1,16}$";
    private static final String guiFields[] = { GUIField.STATUS.toString(),
            GUIField.NAME.toString(), GUIField.SRCNODE.toString(),
            GUIField.SRCPORT.toString(), GUIField.DSTNODE.toString(),
            GUIField.DSTPORT.toString() };

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

    private String status;
    private String name;
    private String srcSwitchId;
    private String srcPort;
    private String dstSwitchId;
    private String dstPort;

    public TopologyUserLinkConfig() {
        super();
        status = STATUS.LINKDOWN.toString();
    }

    public TopologyUserLinkConfig(String name, String srcSwitchId,
            String srcPort, String dstSwitchId, String dstPort) {
        super();
        this.name = name;
        this.srcSwitchId = srcSwitchId;
        this.dstSwitchId = dstSwitchId;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
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
                || srcPort == null || dstPort == null)
            return false;
        if (!isValidSwitchId(srcSwitchId) || !isValidSwitchId(dstSwitchId))
            return false;
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
                + ", srcSwitchId=" + srcSwitchId + ", srcPort=" + srcPort
                + ", dstSwitchId=" + dstSwitchId + ", dstPort=" + dstPort + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dstPort == null) ? 0 : dstPort.hashCode());
        result = prime * result
                + ((dstSwitchId == null) ? 0 : dstSwitchId.hashCode());
        result = prime * result + ((srcPort == null) ? 0 : srcPort.hashCode());
        result = prime * result
                + ((srcSwitchId == null) ? 0 : srcSwitchId.hashCode());
        return result;
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
