/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.topology;

import com.google.common.base.Preconditions;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of NetworkGraphService{@link org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService}.
 * It uses Jung graph library internally to maintain a graph and optimum way to return shortest path using
 * Dijkstra algorithm.
 */
public class NetworkGraphDijkstra implements NetworkGraphService {

  private static final Logger _logger = LoggerFactory.getLogger(NetworkGraphDijkstra.class);

  DijkstraShortestPath<NodeId, Link> shortestPath = null;
  Graph<NodeId, Link> networkGraph = null;

  /**
   * Adds links to existing graph or creates new directed graph with given links if graph was not initialized.
   * @param links
   */
  @Override
  public synchronized void addLinks(List<Link> links) {
    if(links == null || links.isEmpty()) {
      _logger.info("In addLinks: No link added as links is null or empty.");
      return;
    }

    if(networkGraph == null) {
      networkGraph = new DirectedSparseGraph<>();
    }

    for(Link link : links) {
      NodeId sourceNodeId = link.getSource().getSourceNode();
      NodeId destinationNodeId = link.getDestination().getDestNode();
      networkGraph.addVertex(sourceNodeId);
      networkGraph.addVertex(destinationNodeId);
      networkGraph.addEdge(link, sourceNodeId, destinationNodeId);
    }
    if(shortestPath == null) {
      shortestPath = new DijkstraShortestPath<>(networkGraph);
    } else {
      shortestPath.reset();
    }
  }

  /**
   * removes links from existing graph.
   * @param links
   */
  @Override
  public synchronized void removeLinks(List<Link> links) {
    Preconditions.checkNotNull(networkGraph, "Graph is not initialized, add links first.");

    if(links == null || links.isEmpty()) {
      _logger.info("In removeLinks: No link removed as links is null or empty.");
      return;
    }

    for(Link link : links) {
      networkGraph.removeEdge(link);
    }

    if(shortestPath == null) {
      shortestPath = new DijkstraShortestPath<>(networkGraph);
    } else {
      shortestPath.reset();
    }
  }

  /**
   * returns a path between 2 nodes. Uses Dijkstra's algorithm to return shortest path.
   * @param sourceNodeId
   * @param destinationNodeId
   * @return
   */
  @Override
  public synchronized List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId) {
    Preconditions.checkNotNull(shortestPath, "Graph is not initialized, add links first.");

    if(sourceNodeId == null || destinationNodeId == null) {
      _logger.info("In getPath: returning null, as sourceNodeId or destinationNodeId is null.");
      return null;
    }

    return shortestPath.getPath(sourceNodeId, destinationNodeId);
  }

  /**
   * Clears the prebuilt graph, in case same service instance is required to process a new graph.
   */
  @Override
  public synchronized void clear() {
    networkGraph = null;
    shortestPath = null;
  }
}
