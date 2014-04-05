/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.inventory;

import org.opendaylight.controller.sample.l2switch.md.util.InstanceIdentifierUtils;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.*;

/**
 * InventoryService provides functions related to Nodes & NodeConnectors.
 */
public class InventoryService {
  private DataBrokerService dataService;
  // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between controller & switch
  private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;

  /**
   * Construct an InventoryService object with the specified inputs.
   * @param dataService  The DataBrokerService associated with the InventoryService.
   */
  public InventoryService(DataBrokerService dataService) {
    this.dataService = dataService;
    controllerSwitchConnectors = new HashMap<String, NodeConnectorRef>();
  }

  public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
    return controllerSwitchConnectors;
  }

  // ToDo: Improve performance for thousands of switch ports
  /**
   * Get the External NodeConnectors of the network, which are the NodeConnectors connected to hosts.
   * @return  The list of external node connectors.
   */
  public List<NodeConnectorRef> getExternalNodeConnectors() {
    // External NodeConnectors = All - Internal
    ArrayList<NodeConnectorRef> externalNodeConnectors = new ArrayList<NodeConnectorRef>();
    Set<String> internalNodeConnectors = new HashSet<>();

    // Read Topology -- find list of switch-to-switch internal node connectors
    NetworkTopology networkTopology =
        (NetworkTopology)dataService.readOperationalData(
            InstanceIdentifier.<NetworkTopology>builder(NetworkTopology.class).toInstance());

    for (Topology topology : networkTopology.getTopology()) {
      Topology completeTopology =
          (Topology)dataService.readOperationalData(
              InstanceIdentifierUtils.generateTopologyInstanceIdentifier(
                  topology.getTopologyId().getValue()));

      for (Link link : completeTopology.getLink()) {
        internalNodeConnectors.add(link.getDestination().getDestTp().getValue());
        internalNodeConnectors.add(link.getSource().getSourceTp().getValue());
      }
    }

    // Read Inventory -- contains list of all nodeConnectors
    InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    Nodes nodes = (Nodes)dataService.readOperationalData(nodesInsIdBuilder.toInstance());
    if (nodes != null) {
      for (Node node : nodes.getNode()) {
        Node completeNode = (Node)dataService.readOperationalData(InstanceIdentifierUtils.createNodePath(node.getId()));
        for (NodeConnector nodeConnector : completeNode.getNodeConnector()) {
          // NodeConnector isn't switch-to-switch, so it must be controller-to-switch (internal) or external
          if (!internalNodeConnectors.contains(nodeConnector.getId().getValue())) {
            NodeConnectorRef ncRef = new NodeConnectorRef(
                    InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                            .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nodeConnector.getKey()).toInstance());

            // External node connectors have "-" in their name for mininet, i.e. "s1-eth1"
            if (nodeConnector.getAugmentation(FlowCapableNodeConnector.class).getName().contains("-")) {
              externalNodeConnectors.add(ncRef);
            }
            // Controller-to-switch internal node connectors
            else {
              controllerSwitchConnectors.put(node.getId().getValue(), ncRef);
            }
          }
        }
      }
    }

    return externalNodeConnectors;
  }
}