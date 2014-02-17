/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import java.util.List;

@SuppressWarnings("all")
public class InventoryMapping {
  public static NodeConnector toAdNodeConnector(final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> identifier) {
    final List<PathArgument> path = identifier.getPath();
    final PathArgument lastPathArgument = IterableExtensions.<PathArgument>last(path);
    final NodeConnectorKey tpKey = ((IdentifiableItem<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector,NodeConnectorKey>) lastPathArgument).getKey();
    return InventoryMapping.nodeConnectorFromId(tpKey.getId().getValue());
  }
  
  public static Node toAdNode(final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> identifier) {
    final List<PathArgument> path = identifier.getPath();
    final PathArgument lastPathArgument = IterableExtensions.<PathArgument>last(path);
    final NodeKey tpKey = ((IdentifiableItem<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,NodeKey>) lastPathArgument).getKey();
    return InventoryMapping.nodeFromNodeId(tpKey.getId().getValue());
  }
  
  public static NodeRef toNodeRef(final Node node) {
    final NodeId nodeId = new NodeId(InventoryMapping.toNodeId(node));
    final NodeKey nodeKey = new NodeKey(nodeId);
    final InstanceIdentifierBuilder<? extends Object> builder = InstanceIdentifier.builder();
    final InstanceIdentifierBuilder<Nodes> nodes = builder.<Nodes>node(Nodes.class);
    final InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> child =
            nodes.<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, NodeKey>child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey);
    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> path = child.toInstance();
    return new NodeRef(path);
  }
  
  public static NodeKey toNodeKey(final Node node) {
    final NodeId nodeId = new NodeId(InventoryMapping.toNodeId(node));
    return new NodeKey(nodeId);
  }
  
  public static NodeConnectorKey toNodeConnectorKey(final NodeConnector nc) {
    final NodeConnectorId nodeConnectorId = new NodeConnectorId(InventoryMapping.toNodeConnectorId(nc));
    return new NodeConnectorKey(nodeConnectorId);
  }
  
  public static String toNodeId(final Node node) {
    final StringConcatenation builder = new StringConcatenation();
    builder.append("ad-sal:");
    builder.append(node.getType(), "");
    builder.append("::");
    builder.append(node.getNodeIDString(), "");
    return builder.toString();
  }
  
  public static String toNodeConnectorId(final NodeConnector nc) {
    final StringConcatenation builder = new StringConcatenation();
    builder.append(InventoryMapping.toNodeId(nc.getNode()), "");
    builder.append("::");
    builder.append(nc.getNodeConnectorIDString(), "");
    return builder.toString();
  }
  
  public static Node nodeFromNodeId(final String nodeId) {
    final String[] split = nodeId.split("::");
    return InventoryMapping.nodeFromString(split);
  }
  
  public static NodeConnector nodeConnectorFromId(final String invId) {
    final String[] split = invId.split("::");
    return InventoryMapping.nodeConnectorFromString(split);
  }
  
  private static NodeConnector nodeConnectorFromString(final String[] string) {
    final List<String> subList = ((List<String>)Conversions.doWrapArray(string)).subList(0, 1);
    final Node node = InventoryMapping.nodeFromString(((String[])Conversions.unwrapArray(subList, String.class)));
    final String index3 = string[2];
    return NodeConnector.fromStringNoNode(index3, node);
  }
  
  private static Node nodeFromString(final String[] strings) {
      String index0 = strings[0];
      final String type = index0.substring(6);
      String id = strings[1];
      return Node.fromString(type, id);
  }
}
