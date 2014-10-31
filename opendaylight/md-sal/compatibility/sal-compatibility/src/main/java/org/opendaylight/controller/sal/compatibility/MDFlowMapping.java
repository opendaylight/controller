/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
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
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.action._case.FloodActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.all.action._case.FloodAllActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.loopback.action._case.LoopbackActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MDFlowMapping {
    private MDFlowMapping() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> toMDActions(final List<Action> actions) {
        final ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> ret =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>(actions.size());
        int action = 0;
        for (final Action sourceAction : actions) {
            ret.add(toAction(sourceAction, action));
            action++;
        }

        return ret;
    }

    public static FlowAdded flowAdded(final Flow sourceFlow) {
        Preconditions.checkArgument(sourceFlow != null);

        return new FlowAddedBuilder()
        .setHardTimeout(Integer.valueOf(sourceFlow.getHardTimeout()))
        .setIdleTimeout(Integer.valueOf(sourceFlow.getIdleTimeout()))
        .setCookie(new FlowCookie(BigInteger.valueOf(sourceFlow.getId())))
        .setPriority(Integer.valueOf(sourceFlow.getPriority()))
        .setInstructions(MDFlowMapping.toApplyInstruction(toMDActions(sourceFlow.getActions())))
        .setMatch(FromSalConversionsUtils.toMatch(sourceFlow.getMatch()))
        .setTableId((short)0)
        .build();
    }

    private static FlowBuilder internalToMDFlow(final Flow sourceFlow) {
        Preconditions.checkArgument(sourceFlow != null);

        return new FlowBuilder()
        .setHardTimeout(Integer.valueOf(sourceFlow.getHardTimeout()))
        .setIdleTimeout(Integer.valueOf(sourceFlow.getIdleTimeout()))
        .setCookie(new FlowCookie(BigInteger.valueOf(sourceFlow.getId())))
        .setPriority(Integer.valueOf((sourceFlow.getPriority())))
        .setInstructions(MDFlowMapping.toApplyInstruction(toMDActions(sourceFlow.getActions())))
        .setMatch(FromSalConversionsUtils.toMatch(sourceFlow.getMatch()));
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow toMDFlow(final Flow sourceFlow, final String flowId) {
        return internalToMDFlow(sourceFlow)
                .setTableId((short)0)
                .setId(new FlowId(flowId))
                .build();
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow toMDSalflow(final Flow sourceFlow) {
        return internalToMDFlow(sourceFlow).build();
    }

    public static Instructions toApplyInstruction(final List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions) {
        return new InstructionsBuilder()
        .setInstruction(
                Collections.singletonList(
                        new InstructionBuilder()
                        .setOrder(0)
                        .setInstruction(
                                new ApplyActionsCaseBuilder()
                                .setApplyActions(new ApplyActionsBuilder().setAction(actions).build())
                                .build()
                                ).build())
                ).build();
    }

    public static RemoveFlowInput removeFlowInput(final Node sourceNode, final Flow sourceFlow) {
        final FlowAdded source = MDFlowMapping.flowAdded(sourceFlow);
        return new RemoveFlowInputBuilder((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) source)
        .setNode(NodeMapping.toNodeRef(sourceNode))
        .build();
    }

    public static AddFlowInput addFlowInput(final Node sourceNode, final Flow sourceFlow) {
        final FlowAdded source = MDFlowMapping.flowAdded(sourceFlow);
        return new AddFlowInputBuilder(((org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) source))
        .setNode(NodeMapping.toNodeRef(sourceNode))
        .build();
    }

    public static UpdateFlowInput updateFlowInput(final Node sourceNode, final Flow oldFlow, final Flow newFlow) {
        return new UpdateFlowInputBuilder()
        .setOriginalFlow(new OriginalFlowBuilder(MDFlowMapping.flowAdded(newFlow)).build())
        .setUpdatedFlow(new UpdatedFlowBuilder(MDFlowMapping.flowAdded(newFlow)).build())
        .setNode(NodeMapping.toNodeRef(sourceNode))
        .build();
    }

    private static ControllerActionCase _toAction(final Controller sourceAction) {
        return new ControllerActionCaseBuilder().setControllerAction(new ControllerActionBuilder().build()).build();
    }

    private static DropActionCase _toAction(final Drop sourceAction) {
        return new DropActionCaseBuilder().setDropAction(new DropActionBuilder().build()).build();
    }

    private static FloodActionCase _toAction(final Flood sourceAction) {
        return new FloodActionCaseBuilder().setFloodAction(new FloodActionBuilder().build()).build();
    }

    private static FloodAllActionCase _toAction(final FloodAll sourceAction) {
        return new FloodAllActionCaseBuilder().setFloodAllAction(new FloodAllActionBuilder().build()).build();
    }

    private static HwPathActionCase _toAction(final HwPath sourceAction) {
        return new HwPathActionCaseBuilder().setHwPathAction(new HwPathActionBuilder().build()).build();
    }

    private static LoopbackActionCase _toAction(final Loopback sourceAction) {
        return new LoopbackActionCaseBuilder().setLoopbackAction( new LoopbackActionBuilder().build()).build();
    }

    private static OutputActionCase _toAction(final Output sourceAction) {
        return new OutputActionCaseBuilder()
        .setOutputAction(
                new OutputActionBuilder().setOutputNodeConnector(MDFlowMapping.toUri(sourceAction.getPort())).build()
                ).build();
    }

    private static PopVlanActionCase _toAction(final PopVlan sourceAction) {
        PopVlanActionBuilder popVlanActionBuilder = new PopVlanActionBuilder();
        return new PopVlanActionCaseBuilder().setPopVlanAction(popVlanActionBuilder.build()).build();
    }

    private static PushVlanActionCase _toAction(final PushVlan sourceAction) {
        return new PushVlanActionCaseBuilder()
        .setPushVlanAction(
                new PushVlanActionBuilder()
                .setEthernetType(Integer.valueOf(sourceAction.getTag()))
                .build()
                ).build();
    }

    private static SetDlDstActionCase _toAction(final SetDlDst sourceAction) {
        return new SetDlDstActionCaseBuilder()
        .setSetDlDstAction(new SetDlDstActionBuilder().setAddress(MDFlowMapping.toMacAddress(sourceAction.getDlAddress())).build())
        .build();
    }

    private static SetDlSrcActionCase _toAction(final SetDlSrc sourceAction) {
        return new SetDlSrcActionCaseBuilder()
        .setSetDlSrcAction(new SetDlSrcActionBuilder().setAddress(MDFlowMapping.toMacAddress(sourceAction.getDlAddress())).build())
        .build();
    }

    private static SetDlTypeActionCase _toAction(final SetDlType sourceAction) {
        return new SetDlTypeActionCaseBuilder()
        .setSetDlTypeAction(new SetDlTypeActionBuilder().setDlType(new EtherType(Long.valueOf(sourceAction.getDlType()))).build())
        .build();
    }

    private static SetNextHopActionCase _toAction(final SetNextHop sourceAction) {
        return new SetNextHopActionCaseBuilder()
        .setSetNextHopAction(new SetNextHopActionBuilder().setAddress(MDFlowMapping.toInetAddress(sourceAction.getAddress())).build())
        .build();
    }

    private static SetNwDstActionCase _toAction(final SetNwDst sourceAction) {
        return new SetNwDstActionCaseBuilder()
        .setSetNwDstAction(new SetNwDstActionBuilder().setAddress(MDFlowMapping.toInetAddress(sourceAction.getAddress())).build())
        .build();
    }

    private static SetNwSrcActionCase _toAction(final SetNwSrc sourceAction) {
        return new SetNwSrcActionCaseBuilder()
        .setSetNwSrcAction(new SetNwSrcActionBuilder().setAddress(MDFlowMapping.toInetAddress(sourceAction.getAddress())).build())
        .build();
    }

    private static SetNwTosActionCase _toAction(final SetNwTos sourceAction) {
        return new SetNwTosActionCaseBuilder()
        .setSetNwTosAction(new SetNwTosActionBuilder().setTos(FromSalConversionsUtils.dscpToTos(sourceAction.getNwTos())).build())
        .build();
    }

    private static SetTpDstActionCase _toAction(final SetTpDst sourceAction) {
        return new SetTpDstActionCaseBuilder()
        .setSetTpDstAction(new SetTpDstActionBuilder().setPort(new PortNumber(sourceAction.getPort())).build())
        .build();
    }

    private static SetTpSrcActionCase _toAction(final SetTpSrc sourceAction) {
        return new SetTpSrcActionCaseBuilder()
        .setSetTpSrcAction(new SetTpSrcActionBuilder().setPort(new PortNumber(sourceAction.getPort())).build())
        .build();
    }

    private static SetVlanCfiActionCase _toAction(final SetVlanCfi sourceAction) {
        return new SetVlanCfiActionCaseBuilder()
        .setSetVlanCfiAction(new SetVlanCfiActionBuilder().setVlanCfi(new VlanCfi(sourceAction.getCfi())).build())
        .build();
    }

    private static SetVlanIdActionCase _toAction(final SetVlanId sourceAction) {
        return new SetVlanIdActionCaseBuilder()
        .setSetVlanIdAction(new SetVlanIdActionBuilder().setVlanId(new VlanId(sourceAction.getVlanId())).build())
        .build();
    }

    private static SetVlanPcpActionCase _toAction(final SetVlanPcp sourceAction) {
        return new SetVlanPcpActionCaseBuilder()
        .setSetVlanPcpAction(new SetVlanPcpActionBuilder().setVlanPcp(new VlanPcp((short) sourceAction.getPcp())).build())
        .build();
    }

    private static SwPathActionCase _toAction(final SwPath sourceAction) {
        return new SwPathActionCaseBuilder().setSwPathAction(new SwPathActionBuilder().build()).build();
    }

    public static Uri toUri(final NodeConnector connector) {
        return new NodeConnectorId(NodeMapping.OPENFLOW_ID_PREFIX + connector.getNode().getID() + ":" + (connector.getID()));
    }

    public static MacAddress toMacAddress(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(18);
        boolean first = true;

        for (final byte b : bytes) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return new MacAddress(sb.toString());
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action toAction(final Action sourceAction, final int order) {
        final ActionBuilder ret = new ActionBuilder().setOrder(order);

        if (sourceAction instanceof Controller) {
            ret.setAction(_toAction((Controller)sourceAction));
        } else if (sourceAction instanceof Drop) {
            ret.setAction(_toAction((Drop)sourceAction));
        } else if (sourceAction instanceof Flood) {
            ret.setAction(_toAction((Flood)sourceAction));
        } else if (sourceAction instanceof FloodAll) {
            ret.setAction(_toAction((FloodAll)sourceAction));
        } else if (sourceAction instanceof HwPath) {
            ret.setAction(_toAction((HwPath)sourceAction));
        } else if (sourceAction instanceof Loopback) {
            ret.setAction(_toAction((Loopback)sourceAction));
        } else if (sourceAction instanceof Output) {
            ret.setAction(_toAction((Output)sourceAction));
        } else if (sourceAction instanceof PopVlan) {
            ret.setAction(_toAction((PopVlan)sourceAction));
        } else if (sourceAction instanceof PushVlan) {
            ret.setAction(_toAction((PushVlan)sourceAction));
        } else if (sourceAction instanceof SetDlDst) {
            ret.setAction(_toAction((SetDlDst)sourceAction));
        } else if (sourceAction instanceof SetDlSrc) {
            ret.setAction(_toAction((SetDlSrc)sourceAction));
        } else if (sourceAction instanceof SetDlType) {
            ret.setAction(_toAction((SetDlType)sourceAction));
        } else if (sourceAction instanceof SetNextHop) {
            ret.setAction(_toAction((SetNextHop)sourceAction));
        } else if (sourceAction instanceof SetNwDst) {
            ret.setAction(_toAction((SetNwDst)sourceAction));
        } else if (sourceAction instanceof SetNwSrc) {
            ret.setAction(_toAction((SetNwSrc)sourceAction));
        } else if (sourceAction instanceof SetNwTos) {
            ret.setAction(_toAction((SetNwTos)sourceAction));
        } else if (sourceAction instanceof SetTpDst) {
            ret.setAction(_toAction((SetTpDst)sourceAction));
        } else if (sourceAction instanceof SetTpSrc) {
            ret.setAction(_toAction((SetTpSrc)sourceAction));
        } else if (sourceAction instanceof SetVlanCfi) {
            ret.setAction(_toAction((SetVlanCfi)sourceAction));
        } else if (sourceAction instanceof SetVlanId) {
            ret.setAction(_toAction((SetVlanId)sourceAction));
        } else if (sourceAction instanceof SetVlanPcp) {
            ret.setAction(_toAction((SetVlanPcp)sourceAction));
        } else if (sourceAction instanceof SwPath) {
            ret.setAction(_toAction((SwPath)sourceAction));
        } else {
            throw new IllegalArgumentException(String.format("Unhandled action class %s", sourceAction.getClass()));
        }

        return ret.build();
    }

    public static Address toInetAddress(final InetAddress address) {
        if (address instanceof Inet4Address) {
            return new Ipv4Builder()
            .setIpv4Address(new Ipv4Prefix(InetAddresses.toAddrString(address) + "/32"))
            .build();
        }
        if (address instanceof Inet6Address) {
            return new Ipv6Builder()
            .setIpv6Address(new Ipv6Prefix(InetAddresses.toAddrString(address) + "/128"))
            .build();
        }

        throw new IllegalArgumentException(String.format("Unhandled address class %s", address.getClass()));
    }
}
