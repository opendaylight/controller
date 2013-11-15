package org.opendaylight.controller.sal.compatibility;

import com.google.common.net.InetAddresses
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.ArrayList
import java.util.List
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionBuilder
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder

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
        return it.build();

    }
    
    public static def Instructions toApplyInstruction(ArrayList<Action> actions) {
        val it = new InstructionsBuilder;
        val applyActions = new InstructionBuilder;
        applyActions.instruction = new ApplyActionsBuilder().setAction(actions).build()
        instruction = Collections.<Instruction>singletonList(applyActions.build)
        return it.build;
    }

    public static def flowStatisticsInput(Node sourceNode, Flow sourceFlow) {
        val source = flowAdded(sourceFlow);
        val it = new GetFlowStatisticsInputBuilder(source as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        node = sourceNode.toNodeRef();
        return it.build();
    }

    public static def removeFlowInput(Node sourceNode, Flow sourceFlow) {
        val source = flowAdded(sourceFlow);
        val it = new RemoveFlowInputBuilder(source as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
        return it.build();
    }

    public static def addFlowInput(Node sourceNode, Flow sourceFlow) {
        val source = flowAdded(sourceFlow);
        val it = new AddFlowInputBuilder(source as org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow);
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
        actionBuilder.action = new ControllerActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Drop sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new DropActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Flood sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new FloodActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(FloodAll sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new FloodAllActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(HwPath sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new HwPathActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Loopback sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new LoopbackActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(Output sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new OutputActionBuilder();
        outputNodeConnector = sourceAction.port.toUri;
        actionBuilder.action = it.build();
        return actionBuilder.build();

    }

    public static dispatch def toAction(PopVlan sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new PopVlanActionBuilder().build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(PushVlan sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new PushVlanActionBuilder();
        cfi = new VlanCfi(sourceAction.cfi);
        vlanId = new VlanId(sourceAction.vlanId);
        pcp = sourceAction.pcp;
        tag = sourceAction.tag;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlDstActionBuilder();
        address = sourceAction.dlAddress.toMacAddress();
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlSrcActionBuilder();
        address = sourceAction.dlAddress.toMacAddress;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetDlType sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetDlTypeActionBuilder();
        dlType = new EtherType(sourceAction.dlType as long);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNextHop sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNextHopActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwDstActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwSrcActionBuilder();
        val inetAddress = sourceAction.address;
        address = inetAddress.toInetAddress;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetNwTos sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetNwTosActionBuilder();
        tos = sourceAction.nwTos;
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetTpDst sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetTpDstActionBuilder();
        port = new PortNumber(sourceAction.port);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetTpSrc sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetTpSrcActionBuilder();
        port = new PortNumber(sourceAction.port);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanCfi sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetVlanCfiActionBuilder();
        vlanCfi = new VlanCfi(sourceAction.cfi);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanId sourceAction) {
        val actionBuilder = new ActionBuilder();

        val it = new SetVlanIdActionBuilder();
        vlanId = new VlanId(sourceAction.vlanId);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SetVlanPcp sourceAction) {
        val actionBuilder = new ActionBuilder();
        val it = new SetVlanPcpActionBuilder();
        vlanPcp = new VlanPcp(sourceAction.pcp as short);
        actionBuilder.action = it.build();
        return actionBuilder.build();
    }

    public static dispatch def toAction(SwPath sourceAction) {
        val actionBuilder = new ActionBuilder();
        actionBuilder.action = new SwPathActionBuilder().build();
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
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
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
}
