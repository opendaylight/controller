
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
 * The Interface provides methods to manipulate user configured link.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class TopologyUserLinkConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TopologyUserLinkConfig.class);

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
    private String srcNodeConnector;
    @XmlElement
    private String dstNodeConnector;

    public TopologyUserLinkConfig() {
        super();
        status = STATUS.LINKDOWN.toString();
    }

    public TopologyUserLinkConfig(String name, String srcNodeConnector, String dstNodeConnector) {
        super();
        this.name = name;
        this.srcNodeConnector = srcNodeConnector;
        this.dstNodeConnector = dstNodeConnector;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public STATUS getStatus() {
        return STATUS.fromString(status);
    }

    public void setStatus(STATUS s) {
        this.status = s.toString();
    }

    public String getSrcNodeConnector() {
        return srcNodeConnector;
    }

    public void setSrcNodeConnector(String srcNodeConnector) {
        this.srcNodeConnector = srcNodeConnector;
    }

    public String getDstNodeConnector() {
        return dstNodeConnector;
    }

    public void setDstNodeConnector(String dstNodeConnector) {
        this.dstNodeConnector = dstNodeConnector;
    }

    public boolean isValidNodeConnector(String nodeConnectorStr) {
        NodeConnector nc = NodeConnector.fromString(nodeConnectorStr);
        if (nc == null) return false;
        return true;
    }

    public boolean isValid() {
        if (name == null || srcNodeConnector == null || dstNodeConnector == null) {
            return false;
        }

        if (!isValidNodeConnector(srcNodeConnector) ||
                !isValidNodeConnector(dstNodeConnector)) {
            logger.warn("Invalid NodeConnector");
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((dstNodeConnector == null) ? 0 : dstNodeConnector.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime
                * result
                + ((srcNodeConnector == null) ? 0 : srcNodeConnector.hashCode());
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
        TopologyUserLinkConfig other = (TopologyUserLinkConfig) obj;
        if (dstNodeConnector == null) {
            if (other.dstNodeConnector != null)
                return false;
        } else if (!dstNodeConnector.equals(other.dstNodeConnector))
            return false;
        if (srcNodeConnector == null) {
            if (other.srcNodeConnector != null)
                return false;
        } else if (!srcNodeConnector.equals(other.srcNodeConnector))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TopologyUserLinkConfig [status=" + status + ", name=" + name
                + ", srcNodeConnector=" + srcNodeConnector
                + ", dstNodeConnector=" + dstNodeConnector + "]";
    }
}
