/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.reader;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeTable;

/**
 * @author Aditya Prakash Vaja <aditya.vaja@bigswitch.com>
 * Represents the Table statistics for the node
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NodeTableStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement
    private NodeTable nodeTable;
    @XmlElement
    private String name;
    @XmlElement
    private int activeCount;
    @XmlElement
    private long lookupCount;
    @XmlElement
    private long matchedCount;
    @XmlElement
    private int maximumEntries;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + activeCount;
        result = prime * result + (int) (lookupCount ^ (lookupCount >>> 32));
        result = prime * result + (int) (matchedCount ^ (matchedCount >>> 32));
        result = prime * result + maximumEntries;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nodeTable == null) ? 0 : nodeTable.hashCode());
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
        if (!(obj instanceof NodeTableStatistics)) {
            return false;
        }
        NodeTableStatistics other = (NodeTableStatistics) obj;
        if (activeCount != other.activeCount) {
            return false;
        }
        if (lookupCount != other.lookupCount) {
            return false;
        }
        if (matchedCount != other.matchedCount) {
            return false;
        }
        if (maximumEntries != other.maximumEntries) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nodeTable == null) {
            if (other.nodeTable != null) {
                return false;
            }
        } else if (!nodeTable.equals(other.nodeTable)) {
            return false;
        }
        return true;
    }

    //To Satisfy JAXB
    public NodeTableStatistics() {

    }

    /**
     * @return the node table
     */
    public NodeTable getNodeTable() {
        return nodeTable;
    }

    /**
     * @param table of the node
     */
    public void setNodeTable(NodeTable table) {
        this.nodeTable = table;
    }

    /**
     * @return name of the table
     */
    public String getName() {
        return name;
    }

    /**
     * @param name - set the table name to name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the activeCount
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * @param activeCount the activeCount to set
     */
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    /**
     * @return the lookupCount
     */
    public long getLookupCount() {
        return lookupCount;
    }

    /**
     * @param lookupCount the lookupCount to set
     */
    public void setLookupCount(long lookupCount) {
        this.lookupCount = lookupCount;
    }

    /**
     * @return the matchedCount
     */
    public long getMatchedCount() {
        return matchedCount;
    }

    /**
     * @param matchedCount the matchedCount to set
     */
    public void setMatchedCount(long matchedCount) {
        this.matchedCount = matchedCount;
    }

    /**
     * @return the maximumEntries
     */
    public int getMaximumEntries() {
        return maximumEntries;
    }

    /**
     * @param maximumEntries the maximumEntries to set
     */
    public void setMaximumEntries(int maximumEntries) {
        this.maximumEntries = maximumEntries;
    }

    @Override
    public String toString() {
        return "NodeTableStats[tableId = " + nodeTable
                + ", activeCount = " + activeCount
                + ", lookupCount = " + lookupCount
                + ", matchedCount = " + matchedCount
                + ", maximumEntries = " + maximumEntries + "]";
    }
}
