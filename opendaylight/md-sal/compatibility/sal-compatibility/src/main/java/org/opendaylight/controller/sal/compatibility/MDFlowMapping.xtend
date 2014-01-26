/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.net.InetAddresses
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.ArrayList
import org.opendaylight.controller.sal.action.Controller
import org.opendaylight.controller.sal.action.Drop
import org.opendaylight.controller.sal.action.Flood
import org.opendaylight.controller.sal.action.FloodAll
import org.opendaylight.controller.sal.action.HwPath
import org.opendaylight.controller.sal.action.Loopback
import org.opendaylight.controller.sal.action.Output
import org.opendaylight.controller.sal.action.PopVlan
import org.opendaylight.controller.sal.action.PushVlan
import org.opendaylight.controller.sal.action.SetDlDst
import org.opendaylight.controller.sal.action.SetDlSrc
import org.opendaylight.controller.sal.action.SetDlType
import org.opendaylight.controller.sal.action.SetNextHop
import org.opendaylight.controller.sal.action.SetNwDst
import org.opendaylight.controller.sal.action.SetNwSrc
import org.opendaylight.controller.sal.action.SetNwTos
import org.opendaylight.controller.sal.action.SetTpDst
import org.opendaylight.controller.sal.action.SetTpSrc
import org.opendaylight.controller.sal.action.SetVlanCfi
import org.opendaylight.controller.sal.action.SetVlanId
import org.opendaylight.controller.sal.action.SetVlanPcp
import org.opendaylight.controller.sal.action.SwPath
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6Builder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp

import static extension org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder
import java.util.Collections
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.action._case.FloodActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.all.action._case.FloodAllActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.loopback.action._case.LoopbackActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.sw.path.action._case.SwPathActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId

public class MDFlowMapping {

    private new() {
        throw new UnsupportedOperationException()
    }

    public static def flowAdded(Flow sourceFlow) {
        if (sourceFlow == null)
            throw new IllegalArgumentException();
        val it = new FlowAddedBuilder();

        hardTimeout = sourceFlow.hardTimeout as int
        idleTimeout = sourceFlow.idleTimeout as int
        cookie = BigInteger.valueOf(sourceFlow.id)
        priority = sourceFlow.priority as int

        val sourceActions = sourceFlow.actions;
        val targetActions = new ArrayList<Action>();
        for (sourceAction : sourceActions) {
            targetActions.add(sourceAction.toAction());
        }
        instructions = targetActions.toApplyInstruction();
        match = sourceFlow.match.toMatch();
        tableId = new Integer(0).shortValue
        return it.build();

    }
    
    public static def toMDFlow(Flow sourceFlow, String flowId) {
       if (sourceFlow == null)
            throw new IllegalArgumentException();
       val it = new FlowBuilder();
       hardTimeout = sourceFlow.hardTimeout as int
       idleTimeout = sourceFlow.idleTimeout as int
       cookie = BigInteger.valueOf(sourceFlow.id)
       priority = sourceFlow.priority as int
       id = new FlowId(flowId)
    
       val sourceActions = sourceFlow.actions;
       val targetActions = new ArrayList<Action>();
       for (sourceAction : sourceActions) {
           targetActions.add(sourceAction.toAction());
       }
       instructions = targetActions.toApplyInstruction();
       match = sourceFlow.match.toMatch();
       tableId = new Integer(0).shortValue
       return it.build();
    }
    
    public static def Instructions toApplyInstruction(ArrayList<Action> actions) {
        val it = new InstructionsBuilder;
        val applyActions = new InstructionBuilder;
        applyActions.instruction = new ApplyActionsCaseBuilder().setApplyActions(new ApplyActionsBuilder().setAction(actions).build()).build()
        applyActions.setOrder(new Integer(0))
        instruction = Collections.<Instruction>singletonList(applyActions.build)
        return it.build;
    }

    public static def removeFlowInput(Node sourceNode, Flow sourceFlow) {
        val source = flowAdded(sourceFlow);
        val it = new RemoveFlowInputBuilder(source as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        node = sourceNode.toNodeRef()
        return it.build();
    }

    public static def addFlowInput(Node sourceNode, Flow sourceFlow) {
        val source = flowAdded(sourceFlow);
        val it = new AddFlowInputBuilder(source as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        it.setNode(sourceNode.toNodeRef)
        return it.build();
    }

    public static def updateFlowInput(Node sourceNode, Flow oldFlow, Flow newFlow) {
        val it = new UpdateFlowInputBuilder();
        val sourceOld = flowAdded(newFlow);

        val original = new OriginalFlowBuilder(sourceOld as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        val sourceNew = flowAdded(newFlow);
        val updated = new UpdatedFlowBuilder(sourceNew as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        originalFlow = original.build()
        updatedFlow = updated.build();
        node = sourceNode.toNodeRef()
        return it.build();
    }

    public static dispatch def toAction(Controller sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new ControllerActionCaseBuilder().setControllerAction(new ControllerActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Drop sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new DropActionCaseBuilder().setDropAction(new DropActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Flood sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new FloodActionCaseBuilder().setFloodAction(new FloodActionBuilder().build).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(FloodAll sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new FloodAllActionCaseBuilder().setFloodAllAction(new FloodAllActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(HwPath sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new HwPathActionCaseBuilder().setHwPathAction(new HwPathActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Loopback sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new LoopbackActionCaseBuilder().setLoopbackAction(new LoopbackActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Output sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new OutputActionBuilder();
        outputNodeConnector = sourceAction.port.toUri;
        actionBuilder.action = new OutputActionCaseBuilder().setOutputAction(it.build()).build();
        return actionBuilder.build();

    }

    public static dispatch def toAction(PopVlan sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new PopVlanActionCaseBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(PushVlan sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new PushVlanActionBuilder();
        cfi = new VlanCfi(sourceAction.cfi);
        vlanId = new VlanId(sourceAction.vlanId);
        pcp = sourceAction.pcp;
        tag = sourceAction.tag;
        actionBuilder.action = new PushVlanActionCaseBuilder().setPushVlanAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlDstActionBuilder();
        address = sourceAction.dlAddress.toMacAddress();
        actionBuilder.action = new SetDlDstActionCaseBuilder().setSetDlDstAction(it.build()).build;
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlSrcActionBuilder();
        address = sourceAction.dlAddress.toMacAddress;
        actionBuilder.action = new SetDlSrcActionCaseBuilder().setSetDlSrcAction(it.build()).build;
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlType sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlTypeActionBuilder();
        dlType = new EtherType(sourceAction.dlType as long);
        actionBuilder.action = new SetDlTypeActionCaseBuilder().setSetDlTypeAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNextHop sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNextHopActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = new SetNextHopActionCaseBuilder().setSetNextHopAction(it.build).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwDstActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = new SetNwDstActionCaseBuilder().setSetNwDstAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwSrcActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = new SetNwSrcActionCaseBuilder().setSetNwSrcAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwTos sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwTosActionBuilder();
        tos = sourceAction.nwTos;
        actionBuilder.action = new SetNwTosActionCaseBuilder().setSetNwTosAction(it.build).build;
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetTpDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetTpDstActionBuilder();
        port = new PortNumber(sourceAction.port);
        actionBuilder.action = new SetTpDstActionCaseBuilder().setSetTpDstAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetTpSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetTpSrcActionBuilder();
        port = new PortNumber(sourceAction.port);
        actionBuilder.action = new SetTpSrcActionCaseBuilder().setSetTpSrcAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanCfi sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetVlanCfiActionBuilder();
        vlanCfi = new VlanCfi(sourceAction.cfi);
        actionBuilder.action = new SetVlanCfiActionCaseBuilder().setSetVlanCfiAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanId sourceAction) {
        val actionBuilder = new ActionBuilder();

        val it = new SetVlanIdActionBuilder();
        vlanId = new VlanId(sourceAction.vlanId);
        actionBuilder.action = new SetVlanIdActionCaseBuilder().setSetVlanIdAction(it.build()).build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanPcp sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetVlanPcpActionBuilder();
        vlanPcp = new VlanPcp(sourceAction.pcp as short);
        actionBuilder.action = new SetVlanPcpActionCaseBuilder().setSetVlanPcpAction(it.build).build;
        return actionBuilder.build();
    }

    public static dispatch def toAction(SwPath sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new SwPathActionCaseBuilder().setSwPathAction(new SwPathActionBuilder().build()).build();
        return actionBuilder.build();
    }

    public static def dispatch Address toInetAddress(Inet4Address address) {
        val it = new Ipv4Builder
        ipv4Address = new Ipv4Prefix(InetAddresses.toAddrString(address))
        return it.build()
    }

    public static def dispatch Address toInetAddress(Inet6Address address) {
        val it = new Ipv6Builder
        ipv6Address = new Ipv6Prefix(InetAddresses.toAddrString(address))
        return it.build()
    }

    public static def Uri toUri(NodeConnector connector) {
        return new NodeConnectorId(connector.ID as String);
    }

    public static def MacAddress toMacAddress(byte[] bytes) {
        val sb = new StringBuilder(18);
        for (byte b : bytes) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        return new MacAddress(sb.toString());
    }
	
	public static def toMDSalflow(Flow sourceFlow) {
        if (sourceFlow == null)
            throw new IllegalArgumentException();
        val it = new FlowBuilder();

        hardTimeout = sourceFlow.hardTimeout as int
        idleTimeout = sourceFlow.idleTimeout as int
        cookie = BigInteger.valueOf(sourceFlow.id)
        priority = sourceFlow.priority as int

        val sourceActions = sourceFlow.actions;
        val targetActions = new ArrayList<Action>();
        for (sourceAction : sourceActions) {
            targetActions.add(sourceAction.toAction());
        }
        instructions = targetActions.toApplyInstruction();
        match = sourceFlow.match.toMatch();
        return it.build();
	}
	
}
