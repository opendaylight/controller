/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.topology;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import java.util.List;

/**
 * Service that allows to build a network graph using Topology links
 * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and exposes operation that can be performed on such graph.
 */
public interface NetworkGraphService {

  /**
   * Adds links to existing graph or creates new graph with given links if graph was not initialized.
   * @param links
   */
  public void addLinks(List<Link> links);

  /**
   * removes links from existing graph.
   * @param links
   */
  public void removeLinks(List<Link> links);

  /**
   * returns a path between 2 nodes. Implementation should ideally return shortest path.
   * @param sourceNodeId
   * @param destinationNodeId
   * @return
   */
  public List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId);

  /**
   * Clears the prebuilt graph, in case same service instance is required to process a new graph.
   */
  public void clear();
}
