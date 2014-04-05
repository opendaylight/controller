/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.flow;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class FlowWriterServiceImplTest {
  private DataBrokerService dataBrokerService;
  private NodeConnectorRef srcNodeConnectorRef;
  private NodeConnectorRef destNodeConnectorRef;
  private MacAddress destMacAddress;
  private MacAddress srcMacAddress;
  private DataModificationTransaction dataModificationTransaction;
  private NetworkGraphService networkGraphService;

  @Before
  public void init() {
    dataBrokerService = mock(DataBrokerService.class);
    networkGraphService = mock(NetworkGraphService.class);
    //build source node connector ref
    InstanceIdentifier<Nodes> srcNodesInstanceIdentifier
        = InstanceIdentifier.builder(Nodes.class)
        .build();
    InstanceIdentifier<Node> srcNodeInstanceIdentifier
        = InstanceIdentifier.builder(srcNodesInstanceIdentifier)
        .child(Node.class, new NodeKey(new NodeId("openflow:1")))
        .build();
    InstanceIdentifier<NodeConnector> srcNodeConnectorInstanceIdentifier
        = InstanceIdentifier.builder(srcNodeInstanceIdentifier)
        .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("openflow:1:2")))
        .build();
    srcNodeConnectorRef = new NodeConnectorRef(srcNodeConnectorInstanceIdentifier);

    //build dest node connector ref
    InstanceIdentifier<Nodes> nodesInstanceIdentifier
        = InstanceIdentifier.builder(Nodes.class)
        .build();
    InstanceIdentifier<Node> nodeInstanceIdentifier
        = InstanceIdentifier.builder(nodesInstanceIdentifier)
        .child(Node.class, new NodeKey(new NodeId("openflow:2")))
        .build();
    InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
        = InstanceIdentifier.builder(nodeInstanceIdentifier)
        .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("openflow:2:2")))
        .build();
    destNodeConnectorRef = new NodeConnectorRef(nodeConnectorInstanceIdentifier);
    destMacAddress = new MacAddress("00:0a:95:9d:68:16");
    srcMacAddress = new MacAddress("00:0a:95:8c:97:24");
    dataModificationTransaction = mock(DataModificationTransaction.class);
    when(dataBrokerService.beginTransaction()).thenReturn(dataModificationTransaction);
  }

  @Test
  public void testFlowWriterServiceImpl_NPEWhenDataBrokerServiceIsNull() throws Exception {
    try {
      new FlowWriterServiceImpl(null, networkGraphService);
      fail("Expected null pointer exception.");
    } catch(NullPointerException npe) {
      assertEquals("dataBrokerService should not be null.", npe.getMessage());
    }
  }

  @Test
  public void testAddMacToMacFlow_NPEWhenNullSourceMacDestMacAndNodeConnectorRef() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    try {
      flowWriterService.addMacToMacFlow(null, null, null);
      fail("Expected null pointer exception.");
    } catch(NullPointerException npe) {
      assertEquals("Destination mac address should not be null.", npe.getMessage());
    }
  }

  @Test
  public void testAddMacToMacFlow_NPEWhenSourceMacNullMac() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    try {
      flowWriterService.addMacToMacFlow(null, null, destNodeConnectorRef);
      fail("Expected null pointer exception.");
    } catch(NullPointerException npe) {
      assertEquals("Destination mac address should not be null.", npe.getMessage());
    }
  }

  @Test
  public void testAddMacToMacFlow_NPEWhenNullSourceMacNodeConnectorRef() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    try {
      flowWriterService.addMacToMacFlow(null, destMacAddress, null);
      fail("Expected null pointer exception.");
    } catch(NullPointerException npe) {
      assertEquals("Destination port should not be null.", npe.getMessage());
    }
  }

  @Test
  public void testAddMacToMacFlow_WhenNullSourceMac() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    flowWriterService.addMacToMacFlow(null, destMacAddress, destNodeConnectorRef);
    verify(dataBrokerService, times(1)).beginTransaction();
    verify(dataModificationTransaction, times(1)).commit();
  }

  @Test
  public void testAddMacToMacFlow_WhenSrcAndDestMacAreSame() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    flowWriterService.addMacToMacFlow(new MacAddress(destMacAddress.getValue()), destMacAddress, destNodeConnectorRef);
    verify(dataBrokerService, never()).beginTransaction();
    verify(dataModificationTransaction, never()).commit();

  }

  @Test
  public void testAddMacToMacFlow_SunnyDay() throws Exception {
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataBrokerService, networkGraphService);
    flowWriterService.addMacToMacFlow(srcMacAddress, destMacAddress, destNodeConnectorRef);
    verify(dataBrokerService, times(1)).beginTransaction();
    verify(dataModificationTransaction, times(1)).commit();
  }

}
