/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService;
import org.opendaylight.controller.sample.l2switch.md.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of FlowWriterService{@link org.opendaylight.controller.sample.l2switch.md.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using provided DataBrokerService
 * {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService}
 */
public class FlowWriterServiceImpl implements FlowWriterService {
  private static final Logger _logger = LoggerFactory.getLogger(FlowWriterServiceImpl.class);
  private final DataBrokerService dataBrokerService;
  private final NetworkGraphService networkGraphService;
  private AtomicLong flowIdInc = new AtomicLong();
  private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);


  public FlowWriterServiceImpl(DataBrokerService dataBrokerService, NetworkGraphService networkGraphService) {
    Preconditions.checkNotNull(dataBrokerService, "dataBrokerService should not be null.");
    Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
    this.dataBrokerService = dataBrokerService;
    this.networkGraphService = networkGraphService;
  }

  /**
   * Writes a flow that forwards packets to destPort if destination mac in packet is destMac and
   * source Mac in packet is sourceMac. If sourceMac is null then flow would not set any source mac,
   * resulting in all packets with destMac being forwarded to destPort.
   *
   * @param sourceMac
   * @param destMac
   * @param destNodeConnectorRef
   */
  @Override
  public void addMacToMacFlow(MacAddress sourceMac, MacAddress destMac, NodeConnectorRef destNodeConnectorRef) {

    Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
    Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");


    // do not add flow if both macs are same.
    if(sourceMac != null && destMac.equals(sourceMac)) {
      _logger.info("In addMacToMacFlow: No flows added. Source and Destination mac are same.");
      return;
    }

    // get flow table key
    TableKey flowTableKey = new TableKey((short) 0); //TODO: Hard coded Table Id 0, need to get it from Configuration data.

    //build a flow path based on node connector to program flow
    InstanceIdentifier<Flow> flowPath = buildFlowPath(destNodeConnectorRef, flowTableKey);

    // build a flow that target given mac id
    Flow flowBody = createMacToMacFlow(flowTableKey.getId(), 0, sourceMac, destMac, destNodeConnectorRef);

    // commit the flow in config data
    writeFlowToConfigData(flowPath, flowBody);
  }

  /**
   * Writes mac-to-mac flow on all ports that are in the path between given source and destination ports.
   * It uses path provided by NetworkGraphService
   * {@link org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService} to find a links
   * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
   * between given ports. And then writes appropriate flow on each port that is covered in that path.
   *
   * @param sourceMac
   * @param sourceNodeConnectorRef
   * @param destMac
   * @param destNodeConnectorRef
   */
  @Override
  public void addMacToMacFlowsUsingShortestPath(MacAddress sourceMac,
                                                NodeConnectorRef sourceNodeConnectorRef,
                                                MacAddress destMac,
                                                NodeConnectorRef destNodeConnectorRef) {
    Preconditions.checkNotNull(sourceMac, "Source mac address should not be null.");
    Preconditions.checkNotNull(sourceNodeConnectorRef, "Source port should not be null.");
    Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
    Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");

    if(sourceNodeConnectorRef.equals(destNodeConnectorRef)) {
      _logger.info("In addMacToMacFlowsUsingShortestPath: No flows added. Source and Destination ports are same.");
      return;

    }
    NodeId sourceNodeId = new NodeId(sourceNodeConnectorRef.getValue().firstKeyOf(Node.class, NodeKey.class).getId().getValue());
    NodeId destNodeId = new NodeId(destNodeConnectorRef.getValue().firstKeyOf(Node.class, NodeKey.class).getId().getValue());

    // add destMac-To-sourceMac flow on source port
    addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef);

    // add sourceMac-To-destMac flow on destination port
    addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef);

    if(!sourceNodeId.equals(destNodeId)) {
      List<Link> linksInBeween = networkGraphService.getPath(sourceNodeId, destNodeId);

      if(linksInBeween != null) {
        // assumes the list order is maintained and starts with link that has source as source node
        for(Link link : linksInBeween) {
          // add sourceMac-To-destMac flow on source port
          addMacToMacFlow(sourceMac, destMac, getSourceNodeConnectorRef(link));

          // add destMac-To-sourceMac flow on destination port
          addMacToMacFlow(destMac, sourceMac, getDestNodeConnectorRef(link));
        }
      }
    }
  }

  private NodeConnectorRef getSourceNodeConnectorRef(Link link) {
    InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
        = InstanceIdentifierUtils.createNodeConnectorIdentifier(
        link.getSource().getSourceNode().getValue(),
        link.getSource().getSourceTp().getValue());
    return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
  }

  private NodeConnectorRef getDestNodeConnectorRef(Link link) {
    InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
        = InstanceIdentifierUtils.createNodeConnectorIdentifier(
        link.getDestination().getDestNode().getValue(),
        link.getDestination().getDestTp().getValue());

    return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
  }

  /**
   * @param nodeConnectorRef
   * @return
   */
  private InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

    // generate unique flow key
    FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
    FlowKey flowKey = new FlowKey(flowId);

    return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
  }

  /**
   * @param tableId
   * @param priority
   * @param sourceMac
   * @param destMac
   * @param destPort
   * @return {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder}
   *         builds flow that forwards all packets with destMac to given port
   */
  private Flow createMacToMacFlow(Short tableId, int priority,
                                  MacAddress sourceMac, MacAddress destMac, NodeConnectorRef destPort) {

    // start building flow
    FlowBuilder macToMacFlow = new FlowBuilder() //
        .setTableId(tableId) //
        .setFlowName("mac2mac");

    // use its own hash code for id.
    macToMacFlow.setId(new FlowId(Long.toString(macToMacFlow.hashCode())));

    // create a match that has mac to mac ethernet match
    EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder() //
        .setEthernetDestination(new EthernetDestinationBuilder() //
            .setAddress(destMac) //
            .build());
    // set source in the match only if present
    if(sourceMac != null) {
      ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder()
          .setAddress(sourceMac)
          .build());
    }
    EthernetMatch ethernetMatch = ethernetMatchBuilder.build();
    Match match = new MatchBuilder()
        .setEthernetMatch(ethernetMatch)
        .build();


    Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

    Action outputToControllerAction = new ActionBuilder() //
        .setAction(new OutputActionCaseBuilder() //
            .setOutputAction(new OutputActionBuilder() //
                .setMaxLength(new Integer(0xffff)) //
                .setOutputNodeConnector(destPortUri) //
                .build()) //
            .build()) //
        .build();

    // Create an Apply Action
    ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
        .build();

    // Wrap our Apply Action in an Instruction
    Instruction applyActionsInstruction = new InstructionBuilder() //
        .setInstruction(new ApplyActionsCaseBuilder()//
            .setApplyActions(applyActions) //
            .build()) //
        .build();

    // Put our Instruction in a list of Instructions
    macToMacFlow
        .setMatch(match) //
        .setInstructions(new InstructionsBuilder() //
            .setInstruction(ImmutableList.of(applyActionsInstruction)) //
            .build()) //
        .setPriority(priority) //
        .setBufferId(0L) //
        .setHardTimeout(0) //
        .setIdleTimeout(0) //
        .setCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement()))
        .setFlags(new FlowModFlags(false, false, false, false, false));

    return macToMacFlow.build();
  }

  /**
   * Starts and commits data change transaction which
   * modifies provided flow path with supplied body.
   *
   * @param flowPath
   * @param flowBody
   * @return transaction commit
   */
  private Future<RpcResult<TransactionStatus>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath,
                                                                     Flow flowBody) {
    DataModificationTransaction addFlowTransaction = dataBrokerService.beginTransaction();
    addFlowTransaction.putConfigurationData(flowPath, flowBody);
    return addFlowTransaction.commit();
  }
}
