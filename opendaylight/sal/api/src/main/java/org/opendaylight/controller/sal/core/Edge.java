
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   Edge.java
 *
 * @brief  Describe an edge in network made of multiple SDN technologies
 *
 * Class that describe an Edge connecting two NodeConnector, the edge
 * is directed because there is the head and the tail concept which
 * implies a direction.
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * Class that describe an Edge connecting two NodeConnector, the edge
 * is directed because there is the tail and the head concept which
 * implies a direction.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Edge implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeConnector tailNodeConnector;
    @XmlElement
    private NodeConnector headNodeConnector;

    /**
     * Private constructor used for JAXB mapping
     */
    private Edge() {
        this.tailNodeConnector = null;
        this.headNodeConnector = null;
    }

    /**
     * Construct the Edge
     *
     * @param tailNodeConnector Tail Node output connector
     * @param headNodeConnector Head Node input connector
     *
     */
    public Edge(NodeConnector tailNodeConnector, NodeConnector headNodeConnector)
            throws ConstructionException {
        if (tailNodeConnector == null || headNodeConnector == null) {
            throw new ConstructionException(
                    "Null tail or head NodeConnector supplied");
        } else {
            this.tailNodeConnector = tailNodeConnector;
            this.headNodeConnector = headNodeConnector;
        }
    }

    /**
     * Copy Construct the Edge
     *
     * @param src Edge to copy from
     *
     */
    public Edge(Edge src) throws ConstructionException {
        if (src != null) {
            this.tailNodeConnector = new NodeConnector(src
                    .getTailNodeConnector());
            this.headNodeConnector = new NodeConnector(src
                    .getHeadNodeConnector());
        } else {
            throw new ConstructionException("src supplied was null");
        }
    }

    /**
     * getter of edge
     *
     *
     * @return tail NodeConnector of the edge
     */
    public NodeConnector getTailNodeConnector() {
        return tailNodeConnector;
    }

    /**
     * setter for edge
     *
     * @param tailNodeConnector NodeConnector to set the tail
     */
    public void setTailNodeConnector(NodeConnector tailNodeConnector) {
        this.tailNodeConnector = tailNodeConnector;
    }

    /**
     * getter of edge
     *
     *
     * @return head NodeConnector of the edge
     */
    public NodeConnector getHeadNodeConnector() {
        return headNodeConnector;
    }

    /**
     * setter for edge
     *
     * @param headNodeConnector NodeConnector to set the head
     */
    public void setHeadNodeConnector(NodeConnector headNodeConnector) {
        this.headNodeConnector = headNodeConnector;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "(" + this.tailNodeConnector + "->" + this.headNodeConnector
                + ")";
    }
}
