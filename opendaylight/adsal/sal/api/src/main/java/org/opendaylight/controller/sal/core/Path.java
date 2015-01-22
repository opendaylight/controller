
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
 * each of its Head Node there is an link to the next Tail Node in the sequence
 *
 */
package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Describe a path as a sequence of Edge such that from
 * each of its Head Node there is an link to the next Tail Node in the
 * sequence
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Path implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private List<Edge> edges;

    /**
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private Path() {
        this.edges = null;
    }

    /**
     * Construct an object representing a path, the constructor will
     * check if the passed list of edges is such that for every
     * consecutive edges the head node of the first edge coincide with
     * the tail node of the subsequent in order for connectivity to be there.
     *
     * @param edges Edges of the path
     *
     */
    public Path(List<Edge> edges) throws ConstructionException {
        // Lets check if the list of edges is such that the head node
        // of an edge is also the tail node of the subsequent one
        boolean sequential = true;
        if (edges.size() >= 2) {
            for (int i = 0; i < edges.size() - 1; i++) {
                Edge current = edges.get(i);
                Edge next = edges.get(i + 1);
                if (!current.getHeadNodeConnector().getNode().equals(next.getTailNodeConnector().getNode())) {
                    sequential = false;
                    break;
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
     * Create the reversed path
     * @return The reversed path
     */
    public Path reverse() {
        int j = edges.size(); // size always > 0
        Edge[]  aEdges = new Edge[j];
        for (Edge e : edges) {
            j--;
            aEdges[j] = e.reverse();
        }
        Path rp;
        try {
         rp = new Path(Arrays.asList(aEdges));
        } catch (ConstructionException ce) {
            rp = null;
        }
        return rp;
    }

    /**
     * Return the list of nodes of this path, the first node is the start node
     * @return the list of nodes
     */
    public List<Node> getNodes() {
        List<Node> nl = new ArrayList<Node>();
        nl.add(this.getStartNode());
        for (Edge e : edges) {
            nl.add(e.getHeadNodeConnector().getNode());
        }
        return nl;
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
     * @return Return the list of edges that constitute the Path
     */
    public List<Edge> getEdges() {
        return (edges == null) ? Collections.<Edge>emptyList() : new ArrayList<Edge>(edges);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((edges == null) ? 0 : edges.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        Path other = (Path) obj;
        if (edges == null) {
            if (other.edges != null) {
                return false;
            }
        } else if (!edges.equals(other.edges)) {
            return false;
        }
        return true;
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
