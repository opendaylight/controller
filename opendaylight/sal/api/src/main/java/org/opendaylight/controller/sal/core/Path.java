
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   Path.java
 *
 * @brief  Describe a path as a sequence of Edge such that from
 * each of its Tail Node there is an link to the next Head Node in the sequence
 *
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Describe a path as a sequence of Edge such that from
 * each of its Tail Node there is an link to the next Head Node in the
 * sequence
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Path implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private List<Edge> edges;

    /**
     * Private constructor used for JAXB mapping
     */
    private Path() {
        this.edges = null;
    }

    /**
     * Construct an object representing a path, the constructor will
     * check if the passed list of edges is such that for every
     * consecutive edges the tailnode of the first edge coincide with
     * the head node of the subsequent in order for connectivity to be there.
     *
     * @param edges Edges of the path
     *
     */
    public Path(List<Edge> edges) throws ConstructionException {
        // Lets check if the list of edges is such that the tail node
        // of an edge is also the head node of the subsequent one
        boolean sequential = true;
        if (edges.size() >= 2) {
            for (int i = 0; i < edges.size() - 1; i++) {
                Edge current = edges.get(i);
                Edge next = edges.get(i + 1);
                if (!current.getHeadNodeConnector().getNode()
                        .equals(
                                next.getTailNodeConnector()
                                        .getNode())) {
                    sequential = false;
                }
            }
        } else if (edges.size() == 0) {
            throw new ConstructionException("Path is empty");
        }

        if (!sequential) {
            throw new ConstructionException("Path is not sequential");
        }

        this.edges = edges;
    }

    /**
     * Copy Construct for a path
     *
     * @param src Path to copy from
     *
     */
    public Path(Path src) throws ConstructionException {
        if (src != null) {
            this.edges = new LinkedList<Edge>(src.getEdges());
        } else {
            throw new ConstructionException("src supplied was null");
        }
    }

    /**
     * get the First Node of the path
     *
     *
     * @return The start Node of the Path
     */
    public Node getStartNode() {
        return this.edges.get(0).getTailNodeConnector().getNode();
    }

    /**
     * get the Last Node of the path
     *
     *
     * @return The last Node of the Path
     */
    public Node getEndNode() {
        return this.edges.get(this.edges.size() - 1).getHeadNodeConnector()
                .getNode();
    }

    /**
     * getter method for the Path
     *
     *
     * @return Return the list of edges that constitue the Path
     */
    public List<Edge> getEdges() {
        return this.edges;
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
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.edges.size(); i++) {
            if (i != 0) {
                // add the comma to the previous element
                sb.append(",");
            }
            sb.append(this.edges.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
