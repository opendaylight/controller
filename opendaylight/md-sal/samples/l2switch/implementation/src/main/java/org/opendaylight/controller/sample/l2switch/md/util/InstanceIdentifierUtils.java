/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/* InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public class InstanceIdentifierUtils {

  /**
   * Creates an Instance Identifier (path) for node with specified id
   *
   * @param nodeId
   * @return
   */
  public static final InstanceIdentifier<Node> createNodePath(NodeId nodeId) {
    return InstanceIdentifier.builder(Nodes.class) //
        .child(Node.class, new NodeKey(nodeId)) //
        .build();
  }

  /**
   * Shorten's node child path to node path.
   *
   * @param nodeChild child of node, from which we want node path.
   * @return
   */
  public static final InstanceIdentifier<Node> getNodePath(InstanceIdentifier<?> nodeChild) {
    return nodeChild.firstIdentifierOf(Node.class);
  }


  /**
   * Creates a table path by appending table specific location to node path
   *
   * @param nodePath
   * @param tableKey
   * @return
   */
  public static final InstanceIdentifier<Table> createTablePath(InstanceIdentifier<Node> nodePath, TableKey tableKey) {
    return InstanceIdentifier.builder(nodePath)
        .augmentation(FlowCapableNode.class)
        .child(Table.class, tableKey)
        .build();
  }

  /**
   * Creates a path for particular flow, by appending flow-specific information
   * to table path.
   *
   * @param table
   * @param flowKey
   * @return
   */
  public static InstanceIdentifier<Flow> createFlowPath(InstanceIdentifier<Table> table, FlowKey flowKey) {
    return InstanceIdentifier.builder(table)
        .child(Flow.class, flowKey)
        .build();
  }

  /**
   * Extract table id from table path.
   *
   * @param tablePath
   * @return
   */
  public static Short getTableId(InstanceIdentifier<Table> tablePath) {
    return tablePath.firstKeyOf(Table.class, TableKey.class).getId();
  }

  /**
   * Extracts NodeConnectorKey from node connector path.
   */
  public static NodeConnectorKey getNodeConnectorKey(InstanceIdentifier<?> nodeConnectorPath) {
    return nodeConnectorPath.firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
  }

  /**
   * Extracts NodeKey from node path.
   */
  public static NodeKey getNodeKey(InstanceIdentifier<?> nodePath) {
    return nodePath.firstKeyOf(Node.class, NodeKey.class);
  }


  //
  public static final InstanceIdentifier<NodeConnector> createNodeConnectorIdentifier(String nodeIdValue,
                                                                                      String nodeConnectorIdValue) {
    return InstanceIdentifier.builder(createNodePath(new NodeId(nodeIdValue))) //
        .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorIdValue))) //
        .build();
  }

  /**
   * @param nodeConnectorRef
   * @return
   */
  public static InstanceIdentifier<Node> generateNodeInstanceIdentifier(NodeConnectorRef nodeConnectorRef) {
    return nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
  }

  /**
   * @param nodeConnectorRef
   * @param flowTableKey
   * @return
   */
  public static InstanceIdentifier<Table> generateFlowTableInstanceIdentifier(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {
    return InstanceIdentifier.builder(generateNodeInstanceIdentifier(nodeConnectorRef))
        .augmentation(FlowCapableNode.class)
        .child(Table.class, flowTableKey)
        .build();
  }

  /**
   * @param nodeConnectorRef
   * @param flowTableKey
   * @param flowKey
   * @return
   */
  public static InstanceIdentifier<Flow> generateFlowInstanceIdentifier(NodeConnectorRef nodeConnectorRef,
                                                                        TableKey flowTableKey,
                                                                        FlowKey flowKey) {
    return InstanceIdentifier.builder(generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey))
        .child(Flow.class, flowKey)
        .build();
  }

  public static InstanceIdentifier<Topology> generateTopologyInstanceIdentifier(String topologyId) {
    return InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
        .build();
  }
}

