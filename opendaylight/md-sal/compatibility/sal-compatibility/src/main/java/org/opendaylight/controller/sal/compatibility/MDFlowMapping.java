/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Objects;
import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNextHop;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.action._case.FloodAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.action._case.FloodActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.all.action._case.FloodAllAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.all.action._case.FloodAllActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.loopback.action._case.LoopbackAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.loopback.action._case.LoopbackActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.sw.path.action._case.SwPathAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.sw.path.action._case.SwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;

@SuppressWarnings("all")
public class MDFlowMapping {
  private MDFlowMapping() {
    UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException();
    throw _unsupportedOperationException;
  }
  
  public static FlowAdded flowAdded(final Flow sourceFlow) {
    boolean _equals = Objects.equal(sourceFlow, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException();
      throw _illegalArgumentException;
    }
    FlowAddedBuilder _flowAddedBuilder = new FlowAddedBuilder();
    final FlowAddedBuilder it = _flowAddedBuilder;
    short _hardTimeout = sourceFlow.getHardTimeout();
    it.setHardTimeout(Integer.valueOf(((int) _hardTimeout)));
    short _idleTimeout = sourceFlow.getIdleTimeout();
    it.setIdleTimeout(Integer.valueOf(((int) _idleTimeout)));
    long _id = sourceFlow.getId();
    BigInteger _valueOf = BigInteger.valueOf(_id);
    it.setCookie(_valueOf);
    short _priority = sourceFlow.getPriority();
    it.setPriority(Integer.valueOf(((int) _priority)));
    final List<Action> sourceActions = sourceFlow.getActions();
    ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> _arrayList = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();
    final ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> targetActions = _arrayList;
    int action = 0;
    for (final Action sourceAction : sourceActions) {
      {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _action = MDFlowMapping.toAction(sourceAction, action);
        targetActions.add(_action);
        int _plus = (action + 1);
        action = _plus;
      }
    }
    Instructions _applyInstruction = MDFlowMapping.toApplyInstruction(targetActions);
    it.setInstructions(_applyInstruction);
    Match _match = sourceFlow.getMatch();
    org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match _match_1 = FromSalConversionsUtils.toMatch(_match);
    it.setMatch(_match_1);
    Integer _integer = new Integer(0);
    short _shortValue = _integer.shortValue();
    it.setTableId(Short.valueOf(_shortValue));
    return it.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow toMDFlow(final Flow sourceFlow, final String flowId) {
    boolean _equals = Objects.equal(sourceFlow, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException();
      throw _illegalArgumentException;
    }
    FlowBuilder _flowBuilder = new FlowBuilder();
    final FlowBuilder it = _flowBuilder;
    short _hardTimeout = sourceFlow.getHardTimeout();
    it.setHardTimeout(Integer.valueOf(((int) _hardTimeout)));
    short _idleTimeout = sourceFlow.getIdleTimeout();
    it.setIdleTimeout(Integer.valueOf(((int) _idleTimeout)));
    long _id = sourceFlow.getId();
    BigInteger _valueOf = BigInteger.valueOf(_id);
    it.setCookie(_valueOf);
    short _priority = sourceFlow.getPriority();
    it.setPriority(Integer.valueOf(((int) _priority)));
    FlowId _flowId = new FlowId(flowId);
    it.setId(_flowId);
    final List<Action> sourceActions = sourceFlow.getActions();
    ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> _arrayList = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();
    final ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> targetActions = _arrayList;
    int action = 0;
    for (final Action sourceAction : sourceActions) {
      {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _action = MDFlowMapping.toAction(sourceAction, action);
        targetActions.add(_action);
        int _plus = (action + 1);
        action = _plus;
      }
    }
    Instructions _applyInstruction = MDFlowMapping.toApplyInstruction(targetActions);
    it.setInstructions(_applyInstruction);
    Match _match = sourceFlow.getMatch();
    org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match _match_1 = FromSalConversionsUtils.toMatch(_match);
    it.setMatch(_match_1);
    Integer _integer = new Integer(0);
    short _shortValue = _integer.shortValue();
    it.setTableId(Short.valueOf(_shortValue));
    return it.build();
  }
  
  public static Instructions toApplyInstruction(final ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions) {
    InstructionsBuilder _instructionsBuilder = new InstructionsBuilder();
    final InstructionsBuilder it = _instructionsBuilder;
    InstructionBuilder _instructionBuilder = new InstructionBuilder();
    final InstructionBuilder applyActions = _instructionBuilder;
    ApplyActionsCaseBuilder _applyActionsCaseBuilder = new ApplyActionsCaseBuilder();
    ApplyActionsBuilder _applyActionsBuilder = new ApplyActionsBuilder();
    ApplyActionsBuilder _setAction = _applyActionsBuilder.setAction(actions);
    ApplyActions _build = _setAction.build();
    ApplyActionsCaseBuilder _setApplyActions = _applyActionsCaseBuilder.setApplyActions(_build);
    ApplyActionsCase _build_1 = _setApplyActions.build();
    applyActions.setInstruction(_build_1);
    Integer _integer = new Integer(0);
    applyActions.setOrder(_integer);
    Instruction _build_2 = applyActions.build();
    List<Instruction> _singletonList = Collections.<Instruction>singletonList(_build_2);
    it.setInstruction(_singletonList);
    return it.build();
  }
  
  public static RemoveFlowInput removeFlowInput(final Node sourceNode, final Flow sourceFlow) {
    final FlowAdded source = MDFlowMapping.flowAdded(sourceFlow);
    RemoveFlowInputBuilder _removeFlowInputBuilder = new RemoveFlowInputBuilder(((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) source));
    final RemoveFlowInputBuilder it = _removeFlowInputBuilder;
    NodeRef _nodeRef = NodeMapping.toNodeRef(sourceNode);
    it.setNode(_nodeRef);
    return it.build();
  }
  
  public static AddFlowInput addFlowInput(final Node sourceNode, final Flow sourceFlow) {
    final FlowAdded source = MDFlowMapping.flowAdded(sourceFlow);
    AddFlowInputBuilder _addFlowInputBuilder = new AddFlowInputBuilder(((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) source));
    final AddFlowInputBuilder it = _addFlowInputBuilder;
    NodeRef _nodeRef = NodeMapping.toNodeRef(sourceNode);
    it.setNode(_nodeRef);
    return it.build();
  }
  
  public static UpdateFlowInput updateFlowInput(final Node sourceNode, final Flow oldFlow, final Flow newFlow) {
    UpdateFlowInputBuilder _updateFlowInputBuilder = new UpdateFlowInputBuilder();
    final UpdateFlowInputBuilder it = _updateFlowInputBuilder;
    final FlowAdded sourceOld = MDFlowMapping.flowAdded(newFlow);
    OriginalFlowBuilder _originalFlowBuilder = new OriginalFlowBuilder(((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) sourceOld));
    final OriginalFlowBuilder original = _originalFlowBuilder;
    final FlowAdded sourceNew = MDFlowMapping.flowAdded(newFlow);
    UpdatedFlowBuilder _updatedFlowBuilder = new UpdatedFlowBuilder(((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) sourceNew));
    final UpdatedFlowBuilder updated = _updatedFlowBuilder;
    OriginalFlow _build = original.build();
    it.setOriginalFlow(_build);
    UpdatedFlow _build_1 = updated.build();
    it.setUpdatedFlow(_build_1);
    NodeRef _nodeRef = NodeMapping.toNodeRef(sourceNode);
    it.setNode(_nodeRef);
    return it.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final Controller sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    ControllerActionCaseBuilder _controllerActionCaseBuilder = new ControllerActionCaseBuilder();
    ControllerActionBuilder _controllerActionBuilder = new ControllerActionBuilder();
    ControllerAction _build = _controllerActionBuilder.build();
    ControllerActionCaseBuilder _setControllerAction = _controllerActionCaseBuilder.setControllerAction(_build);
    ControllerActionCase _build_1 = _setControllerAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final Drop sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    DropActionCaseBuilder _dropActionCaseBuilder = new DropActionCaseBuilder();
    DropActionBuilder _dropActionBuilder = new DropActionBuilder();
    DropAction _build = _dropActionBuilder.build();
    DropActionCaseBuilder _setDropAction = _dropActionCaseBuilder.setDropAction(_build);
    DropActionCase _build_1 = _setDropAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final Flood sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    FloodActionCaseBuilder _floodActionCaseBuilder = new FloodActionCaseBuilder();
    FloodActionBuilder _floodActionBuilder = new FloodActionBuilder();
    FloodAction _build = _floodActionBuilder.build();
    FloodActionCaseBuilder _setFloodAction = _floodActionCaseBuilder.setFloodAction(_build);
    FloodActionCase _build_1 = _setFloodAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final FloodAll sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    FloodAllActionCaseBuilder _floodAllActionCaseBuilder = new FloodAllActionCaseBuilder();
    FloodAllActionBuilder _floodAllActionBuilder = new FloodAllActionBuilder();
    FloodAllAction _build = _floodAllActionBuilder.build();
    FloodAllActionCaseBuilder _setFloodAllAction = _floodAllActionCaseBuilder.setFloodAllAction(_build);
    FloodAllActionCase _build_1 = _setFloodAllAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final HwPath sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    HwPathActionCaseBuilder _hwPathActionCaseBuilder = new HwPathActionCaseBuilder();
    HwPathActionBuilder _hwPathActionBuilder = new HwPathActionBuilder();
    HwPathAction _build = _hwPathActionBuilder.build();
    HwPathActionCaseBuilder _setHwPathAction = _hwPathActionCaseBuilder.setHwPathAction(_build);
    HwPathActionCase _build_1 = _setHwPathAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final Loopback sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    LoopbackActionCaseBuilder _loopbackActionCaseBuilder = new LoopbackActionCaseBuilder();
    LoopbackActionBuilder _loopbackActionBuilder = new LoopbackActionBuilder();
    LoopbackAction _build = _loopbackActionBuilder.build();
    LoopbackActionCaseBuilder _setLoopbackAction = _loopbackActionCaseBuilder.setLoopbackAction(_build);
    LoopbackActionCase _build_1 = _setLoopbackAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final Output sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    OutputActionBuilder _outputActionBuilder = new OutputActionBuilder();
    final OutputActionBuilder it = _outputActionBuilder;
    NodeConnector _port = sourceAction.getPort();
    Uri _uri = MDFlowMapping.toUri(_port);
    it.setOutputNodeConnector(_uri);
    OutputActionCaseBuilder _outputActionCaseBuilder = new OutputActionCaseBuilder();
    OutputAction _build = it.build();
    OutputActionCaseBuilder _setOutputAction = _outputActionCaseBuilder.setOutputAction(_build);
    OutputActionCase _build_1 = _setOutputAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final PopVlan sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    PopVlanActionCaseBuilder _popVlanActionCaseBuilder = new PopVlanActionCaseBuilder();
    PopVlanActionCase _build = _popVlanActionCaseBuilder.build();
    actionBuilder.setAction(_build);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final PushVlan sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    PushVlanActionBuilder _pushVlanActionBuilder = new PushVlanActionBuilder();
    final PushVlanActionBuilder it = _pushVlanActionBuilder;
    int _cfi = sourceAction.getCfi();
    VlanCfi _vlanCfi = new VlanCfi(Integer.valueOf(_cfi));
    it.setCfi(_vlanCfi);
    int _vlanId = sourceAction.getVlanId();
    VlanId _vlanId_1 = new VlanId(Integer.valueOf(_vlanId));
    it.setVlanId(_vlanId_1);
    int _pcp = sourceAction.getPcp();
    it.setPcp(Integer.valueOf(_pcp));
    int _tag = sourceAction.getTag();
    it.setTag(Integer.valueOf(_tag));
    PushVlanActionCaseBuilder _pushVlanActionCaseBuilder = new PushVlanActionCaseBuilder();
    PushVlanAction _build = it.build();
    PushVlanActionCaseBuilder _setPushVlanAction = _pushVlanActionCaseBuilder.setPushVlanAction(_build);
    PushVlanActionCase _build_1 = _setPushVlanAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetDlDst sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetDlDstActionBuilder _setDlDstActionBuilder = new SetDlDstActionBuilder();
    final SetDlDstActionBuilder it = _setDlDstActionBuilder;
    byte[] _dlAddress = sourceAction.getDlAddress();
    MacAddress _macAddress = MDFlowMapping.toMacAddress(_dlAddress);
    it.setAddress(_macAddress);
    SetDlDstActionCaseBuilder _setDlDstActionCaseBuilder = new SetDlDstActionCaseBuilder();
    SetDlDstAction _build = it.build();
    SetDlDstActionCaseBuilder _setSetDlDstAction = _setDlDstActionCaseBuilder.setSetDlDstAction(_build);
    SetDlDstActionCase _build_1 = _setSetDlDstAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetDlSrc sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetDlSrcActionBuilder _setDlSrcActionBuilder = new SetDlSrcActionBuilder();
    final SetDlSrcActionBuilder it = _setDlSrcActionBuilder;
    byte[] _dlAddress = sourceAction.getDlAddress();
    MacAddress _macAddress = MDFlowMapping.toMacAddress(_dlAddress);
    it.setAddress(_macAddress);
    SetDlSrcActionCaseBuilder _setDlSrcActionCaseBuilder = new SetDlSrcActionCaseBuilder();
    SetDlSrcAction _build = it.build();
    SetDlSrcActionCaseBuilder _setSetDlSrcAction = _setDlSrcActionCaseBuilder.setSetDlSrcAction(_build);
    SetDlSrcActionCase _build_1 = _setSetDlSrcAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetDlType sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetDlTypeActionBuilder _setDlTypeActionBuilder = new SetDlTypeActionBuilder();
    final SetDlTypeActionBuilder it = _setDlTypeActionBuilder;
    int _dlType = sourceAction.getDlType();
    EtherType _etherType = new EtherType(Long.valueOf(((long) _dlType)));
    it.setDlType(_etherType);
    SetDlTypeActionCaseBuilder _setDlTypeActionCaseBuilder = new SetDlTypeActionCaseBuilder();
    SetDlTypeAction _build = it.build();
    SetDlTypeActionCaseBuilder _setSetDlTypeAction = _setDlTypeActionCaseBuilder.setSetDlTypeAction(_build);
    SetDlTypeActionCase _build_1 = _setSetDlTypeAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetNextHop sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetNextHopActionBuilder _setNextHopActionBuilder = new SetNextHopActionBuilder();
    final SetNextHopActionBuilder it = _setNextHopActionBuilder;
    final InetAddress inetAddress = sourceAction.getAddress();
    Address _inetAddress = MDFlowMapping.toInetAddress(inetAddress);
    it.setAddress(_inetAddress);
    SetNextHopActionCaseBuilder _setNextHopActionCaseBuilder = new SetNextHopActionCaseBuilder();
    SetNextHopAction _build = it.build();
    SetNextHopActionCaseBuilder _setSetNextHopAction = _setNextHopActionCaseBuilder.setSetNextHopAction(_build);
    SetNextHopActionCase _build_1 = _setSetNextHopAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetNwDst sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetNwDstActionBuilder _setNwDstActionBuilder = new SetNwDstActionBuilder();
    final SetNwDstActionBuilder it = _setNwDstActionBuilder;
    final InetAddress inetAddress = sourceAction.getAddress();
    Address _inetAddress = MDFlowMapping.toInetAddress(inetAddress);
    it.setAddress(_inetAddress);
    SetNwDstActionCaseBuilder _setNwDstActionCaseBuilder = new SetNwDstActionCaseBuilder();
    SetNwDstAction _build = it.build();
    SetNwDstActionCaseBuilder _setSetNwDstAction = _setNwDstActionCaseBuilder.setSetNwDstAction(_build);
    SetNwDstActionCase _build_1 = _setSetNwDstAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetNwSrc sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetNwSrcActionBuilder _setNwSrcActionBuilder = new SetNwSrcActionBuilder();
    final SetNwSrcActionBuilder it = _setNwSrcActionBuilder;
    final InetAddress inetAddress = sourceAction.getAddress();
    Address _inetAddress = MDFlowMapping.toInetAddress(inetAddress);
    it.setAddress(_inetAddress);
    SetNwSrcActionCaseBuilder _setNwSrcActionCaseBuilder = new SetNwSrcActionCaseBuilder();
    SetNwSrcAction _build = it.build();
    SetNwSrcActionCaseBuilder _setSetNwSrcAction = _setNwSrcActionCaseBuilder.setSetNwSrcAction(_build);
    SetNwSrcActionCase _build_1 = _setSetNwSrcAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetNwTos sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetNwTosActionBuilder _setNwTosActionBuilder = new SetNwTosActionBuilder();
    final SetNwTosActionBuilder it = _setNwTosActionBuilder;
    int _nwTos = sourceAction.getNwTos();
    it.setTos(Integer.valueOf(_nwTos));
    SetNwTosActionCaseBuilder _setNwTosActionCaseBuilder = new SetNwTosActionCaseBuilder();
    SetNwTosAction _build = it.build();
    SetNwTosActionCaseBuilder _setSetNwTosAction = _setNwTosActionCaseBuilder.setSetNwTosAction(_build);
    SetNwTosActionCase _build_1 = _setSetNwTosAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetTpDst sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetTpDstActionBuilder _setTpDstActionBuilder = new SetTpDstActionBuilder();
    final SetTpDstActionBuilder it = _setTpDstActionBuilder;
    int _port = sourceAction.getPort();
    PortNumber _portNumber = new PortNumber(Integer.valueOf(_port));
    it.setPort(_portNumber);
    SetTpDstActionCaseBuilder _setTpDstActionCaseBuilder = new SetTpDstActionCaseBuilder();
    SetTpDstAction _build = it.build();
    SetTpDstActionCaseBuilder _setSetTpDstAction = _setTpDstActionCaseBuilder.setSetTpDstAction(_build);
    SetTpDstActionCase _build_1 = _setSetTpDstAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetTpSrc sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetTpSrcActionBuilder _setTpSrcActionBuilder = new SetTpSrcActionBuilder();
    final SetTpSrcActionBuilder it = _setTpSrcActionBuilder;
    int _port = sourceAction.getPort();
    PortNumber _portNumber = new PortNumber(Integer.valueOf(_port));
    it.setPort(_portNumber);
    SetTpSrcActionCaseBuilder _setTpSrcActionCaseBuilder = new SetTpSrcActionCaseBuilder();
    SetTpSrcAction _build = it.build();
    SetTpSrcActionCaseBuilder _setSetTpSrcAction = _setTpSrcActionCaseBuilder.setSetTpSrcAction(_build);
    SetTpSrcActionCase _build_1 = _setSetTpSrcAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetVlanCfi sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetVlanCfiActionBuilder _setVlanCfiActionBuilder = new SetVlanCfiActionBuilder();
    final SetVlanCfiActionBuilder it = _setVlanCfiActionBuilder;
    int _cfi = sourceAction.getCfi();
    VlanCfi _vlanCfi = new VlanCfi(Integer.valueOf(_cfi));
    it.setVlanCfi(_vlanCfi);
    SetVlanCfiActionCaseBuilder _setVlanCfiActionCaseBuilder = new SetVlanCfiActionCaseBuilder();
    SetVlanCfiAction _build = it.build();
    SetVlanCfiActionCaseBuilder _setSetVlanCfiAction = _setVlanCfiActionCaseBuilder.setSetVlanCfiAction(_build);
    SetVlanCfiActionCase _build_1 = _setSetVlanCfiAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetVlanId sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetVlanIdActionBuilder _setVlanIdActionBuilder = new SetVlanIdActionBuilder();
    final SetVlanIdActionBuilder it = _setVlanIdActionBuilder;
    int _vlanId = sourceAction.getVlanId();
    VlanId _vlanId_1 = new VlanId(Integer.valueOf(_vlanId));
    it.setVlanId(_vlanId_1);
    SetVlanIdActionCaseBuilder _setVlanIdActionCaseBuilder = new SetVlanIdActionCaseBuilder();
    SetVlanIdAction _build = it.build();
    SetVlanIdActionCaseBuilder _setSetVlanIdAction = _setVlanIdActionCaseBuilder.setSetVlanIdAction(_build);
    SetVlanIdActionCase _build_1 = _setSetVlanIdAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SetVlanPcp sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SetVlanPcpActionBuilder _setVlanPcpActionBuilder = new SetVlanPcpActionBuilder();
    final SetVlanPcpActionBuilder it = _setVlanPcpActionBuilder;
    int _pcp = sourceAction.getPcp();
    VlanPcp _vlanPcp = new VlanPcp(Short.valueOf(((short) _pcp)));
    it.setVlanPcp(_vlanPcp);
    SetVlanPcpActionCaseBuilder _setVlanPcpActionCaseBuilder = new SetVlanPcpActionCaseBuilder();
    SetVlanPcpAction _build = it.build();
    SetVlanPcpActionCaseBuilder _setSetVlanPcpAction = _setVlanPcpActionCaseBuilder.setSetVlanPcpAction(_build);
    SetVlanPcpActionCase _build_1 = _setSetVlanPcpAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _toAction(final SwPath sourceAction, final int order) {
    ActionBuilder _actionBuilder = new ActionBuilder();
    final ActionBuilder actionBuilder = _actionBuilder.setOrder(Integer.valueOf(order));
    SwPathActionCaseBuilder _swPathActionCaseBuilder = new SwPathActionCaseBuilder();
    SwPathActionBuilder _swPathActionBuilder = new SwPathActionBuilder();
    SwPathAction _build = _swPathActionBuilder.build();
    SwPathActionCaseBuilder _setSwPathAction = _swPathActionCaseBuilder.setSwPathAction(_build);
    SwPathActionCase _build_1 = _setSwPathAction.build();
    actionBuilder.setAction(_build_1);
    return actionBuilder.build();
  }
  
  public static Address _toInetAddress(final Inet4Address address) {
    Ipv4Builder _ipv4Builder = new Ipv4Builder();
    final Ipv4Builder it = _ipv4Builder;
    String _addrString = InetAddresses.toAddrString(address);
    Ipv4Prefix _ipv4Prefix = new Ipv4Prefix(_addrString);
    it.setIpv4Address(_ipv4Prefix);
    return it.build();
  }
  
  public static Address _toInetAddress(final Inet6Address address) {
    Ipv6Builder _ipv6Builder = new Ipv6Builder();
    final Ipv6Builder it = _ipv6Builder;
    String _addrString = InetAddresses.toAddrString(address);
    Ipv6Prefix _ipv6Prefix = new Ipv6Prefix(_addrString);
    it.setIpv6Address(_ipv6Prefix);
    return it.build();
  }
  
  public static Uri toUri(final NodeConnector connector) {
    Object _iD = connector.getID();
    NodeConnectorId _nodeConnectorId = new NodeConnectorId(((String) _iD));
    return _nodeConnectorId;
  }
  
  public static MacAddress toMacAddress(final byte[] bytes) {
    StringBuilder _stringBuilder = new StringBuilder(18);
    final StringBuilder sb = _stringBuilder;
    for (final byte b : bytes) {
      {
        int _length = sb.length();
        boolean _greaterThan = (_length > 0);
        if (_greaterThan) {
          sb.append(":");
        }
        String _format = String.format("%02x", Byte.valueOf(b));
        sb.append(_format);
      }
    }
    String _string = sb.toString();
    MacAddress _macAddress = new MacAddress(_string);
    return _macAddress;
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow toMDSalflow(final Flow sourceFlow) {
    boolean _equals = Objects.equal(sourceFlow, null);
    if (_equals) {
      IllegalArgumentException _illegalArgumentException = new IllegalArgumentException();
      throw _illegalArgumentException;
    }
    FlowBuilder _flowBuilder = new FlowBuilder();
    final FlowBuilder it = _flowBuilder;
    short _hardTimeout = sourceFlow.getHardTimeout();
    it.setHardTimeout(Integer.valueOf(((int) _hardTimeout)));
    short _idleTimeout = sourceFlow.getIdleTimeout();
    it.setIdleTimeout(Integer.valueOf(((int) _idleTimeout)));
    long _id = sourceFlow.getId();
    BigInteger _valueOf = BigInteger.valueOf(_id);
    it.setCookie(_valueOf);
    short _priority = sourceFlow.getPriority();
    it.setPriority(Integer.valueOf(((int) _priority)));
    final List<Action> sourceActions = sourceFlow.getActions();
    ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> _arrayList = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();
    final ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> targetActions = _arrayList;
    int action = 0;
    for (final Action sourceAction : sourceActions) {
      {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action _action = MDFlowMapping.toAction(sourceAction, action);
        targetActions.add(_action);
        int _plus = (action + 1);
        action = _plus;
      }
    }
    Instructions _applyInstruction = MDFlowMapping.toApplyInstruction(targetActions);
    it.setInstructions(_applyInstruction);
    Match _match = sourceFlow.getMatch();
    org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match _match_1 = FromSalConversionsUtils.toMatch(_match);
    it.setMatch(_match_1);
    return it.build();
  }
  
  public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action toAction(final Action sourceAction, final int order) {
    if (sourceAction instanceof Controller) {
      return _toAction((Controller)sourceAction, order);
    } else if (sourceAction instanceof Drop) {
      return _toAction((Drop)sourceAction, order);
    } else if (sourceAction instanceof Flood) {
      return _toAction((Flood)sourceAction, order);
    } else if (sourceAction instanceof FloodAll) {
      return _toAction((FloodAll)sourceAction, order);
    } else if (sourceAction instanceof HwPath) {
      return _toAction((HwPath)sourceAction, order);
    } else if (sourceAction instanceof Loopback) {
      return _toAction((Loopback)sourceAction, order);
    } else if (sourceAction instanceof Output) {
      return _toAction((Output)sourceAction, order);
    } else if (sourceAction instanceof PopVlan) {
      return _toAction((PopVlan)sourceAction, order);
    } else if (sourceAction instanceof PushVlan) {
      return _toAction((PushVlan)sourceAction, order);
    } else if (sourceAction instanceof SetDlDst) {
      return _toAction((SetDlDst)sourceAction, order);
    } else if (sourceAction instanceof SetDlSrc) {
      return _toAction((SetDlSrc)sourceAction, order);
    } else if (sourceAction instanceof SetDlType) {
      return _toAction((SetDlType)sourceAction, order);
    } else if (sourceAction instanceof SetNextHop) {
      return _toAction((SetNextHop)sourceAction, order);
    } else if (sourceAction instanceof SetNwDst) {
      return _toAction((SetNwDst)sourceAction, order);
    } else if (sourceAction instanceof SetNwSrc) {
      return _toAction((SetNwSrc)sourceAction, order);
    } else if (sourceAction instanceof SetNwTos) {
      return _toAction((SetNwTos)sourceAction, order);
    } else if (sourceAction instanceof SetTpDst) {
      return _toAction((SetTpDst)sourceAction, order);
    } else if (sourceAction instanceof SetTpSrc) {
      return _toAction((SetTpSrc)sourceAction, order);
    } else if (sourceAction instanceof SetVlanCfi) {
      return _toAction((SetVlanCfi)sourceAction, order);
    } else if (sourceAction instanceof SetVlanId) {
      return _toAction((SetVlanId)sourceAction, order);
    } else if (sourceAction instanceof SetVlanPcp) {
      return _toAction((SetVlanPcp)sourceAction, order);
    } else if (sourceAction instanceof SwPath) {
      return _toAction((SwPath)sourceAction, order);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(sourceAction, order).toString());
    }
  }
  
  public static Address toInetAddress(final InetAddress address) {
    if (address instanceof Inet4Address) {
      return _toInetAddress((Inet4Address)address);
    } else if (address instanceof Inet6Address) {
      return _toInetAddress((Inet6Address)address);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(address).toString());
    }
  }
}
