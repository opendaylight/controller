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

    @Override
    public String toString() {
        return "NodeTableStats[tableId = " + nodeTable
                + ", activeCount = " + activeCount
                + ", lookupCount = " + lookupCount
                + ", matchedCount = " + matchedCount + "]";
    }
}
