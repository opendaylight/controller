/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Utility class that provides topological sort
 */
public class TopologicalSort {

    /**
     * Topological sort of dependent nodes in acyclic graphs.
     *
     * @return Sorted {@link List} of {@link Node}s. Order: Nodes with no
     *         dependencies starting.
     * @throws IllegalStateException
     *             when cycle is present in the graph
     */
    public static List<Node> sort(Set<Node> nodes) {
        List<Node> sortedNodes = Lists.newArrayList();

        Set<Node> dependentNodes = getDependentNodes(nodes);

        while (!dependentNodes.isEmpty()) {
            Node n = dependentNodes.iterator().next();
            dependentNodes.remove(n);

            sortedNodes.add(n);

            for (Edge e : n.getInEdges()) {
                Node m = e.getFrom();
                m.getOutEdges().remove(e);

                if (m.getOutEdges().isEmpty()) {
                    dependentNodes.add(m);
                }
            }
        }

        detectCycles(nodes);

        return sortedNodes;
    }

    private static Set<Node> getDependentNodes(Set<Node> nodes) {
        Set<Node> S = Sets.newHashSet();
        for (Node n : nodes) {
            if (n.getOutEdges().size() == 0) {
                S.add(n);
            }
        }
        return S;
    }

    private static void detectCycles(Set<Node> nodes) {
        // Detect cycles
        boolean cycle = false;
        Node cycledNode = null;

        for (Node n : nodes) {
            if (!n.getOutEdges().isEmpty()) {
                cycle = true;
                cycledNode = n;
                break;
            }
        }
        Preconditions.checkState(cycle == false,
                "Cycle detected in graph around node: " + cycledNode);
    }

    /**
     * Interface for nodes in graph that can be sorted topologically
     */
    public static interface Node {
        Set<Edge> getInEdges();

        Set<Edge> getOutEdges();
    }

    /**
     * Interface for edges in graph that can be sorted topologically
     */
    public static interface Edge {
        Node getFrom();

        Node getTo();
    }

    /**
     * Basic Node implementation.
     */
    public static class NodeImpl implements Node {
        private final Set<Edge> inEdges;
        private final Set<Edge> outEdges;

        @Override
        public Set<Edge> getInEdges() {
            return inEdges;
        }

        @Override
        public Set<Edge> getOutEdges() {
            return outEdges;
        }

        public void addEdge(Node to) {
            Edge e = new EdgeImpl(this, to);
            outEdges.add(e);
            to.getInEdges().add(e);
        }

        public NodeImpl() {
            inEdges = Sets.newHashSet();
            outEdges = Sets.newHashSet();
        }
    }

    /**
     * Basic Edge implementation
     */
    public static class EdgeImpl implements Edge {
        private final Node from;
        private final Node to;

        @Override
        public Node getFrom() {
            return from;
        }

        @Override
        public Node getTo() {
            return to;
        }

        public EdgeImpl(Node from, Node to) {
            this.from = from;
            this.to = to;

        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
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
            EdgeImpl other = (EdgeImpl) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (to == null) {
                if (other.to != null)
                    return false;
            } else if (!to.equals(other.to))
                return false;
            return true;
        }
    }

}
