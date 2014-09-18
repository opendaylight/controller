/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class Bug1772Test extends AbstractDataBrokerTest {

    private static final NodeKey NODE_0_KEY = new NodeKey(new NodeId("test:0"));


    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() {
        try {
            return Collections.singleton(BindingReflections.getModuleInfo(Flow.class));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static final InstanceIdentifier<Flow> DEEP_WILDCARDED_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class) //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class) //
            .child(Flow.class) //
            .build();

    private static final TableKey TABLE_0_KEY = new TableKey((short) 0);

    private static final InstanceIdentifier<Table> NODE_0_TABLE_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, NODE_0_KEY) //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class, TABLE_0_KEY) //
            .build();


    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId("test"));

    private static final InstanceIdentifier<Flow> NODE_0_FLOW_PATH = NODE_0_TABLE_PATH.child(Flow.class, FLOW_KEY);

    private static String MAC = "00:01:02:03:04:05";
    private static MacAddress MAC_DTO = new MacAddress(MAC);


    private static final List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> ACTIONS = ImmutableList.<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>builder()
            .add(action(0,new SetDlDstActionCaseBuilder()
                    .setSetDlDstAction(new SetDlDstActionBuilder().setAddress(MAC_DTO).build())
                    .build()))
            .add(action(1,new SetDlSrcActionCaseBuilder()
                    .setSetDlSrcAction(new SetDlSrcActionBuilder().setAddress(MAC_DTO).build())
                    .build()))
            .build();

    private static final Match MATCH = new MatchBuilder()
            .setEthernetMatch(new EthernetMatchBuilder()
            .setEthernetSource(new EthernetSourceBuilder().setAddress(new MacAddress(MAC)).build())
            .setEthernetDestination(new EthernetDestinationBuilder().setAddress(new MacAddress(MAC)).build())
            .build())
            .build();


    private static final Flow FLOW = new FlowBuilder() //
            .setKey(FLOW_KEY) //
            .setBarrier(true) //
            .setStrict(true) //
            .setMatch(MATCH)
            .setInstructions(new InstructionsBuilder()
            .setInstruction(ImmutableList.<Instruction>builder()
                    .add(new InstructionBuilder()
                            .setKey(new InstructionKey(0))
                            .setInstruction(new ApplyActionsCaseBuilder()
                    .setApplyActions(new ApplyActionsBuilder()
                    .setAction(ACTIONS)
                    .build()).build())
                    .build())
                    .build())
                    .build())
            .build();

            private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action action(int i, Action value) {
                return new ActionBuilder()
                    .setKey(new ActionKey(i))
                    .setAction(value)
                    .build();
            }

    @Test
    public void testMacAddressWtriteAndRead() throws InterruptedException, TimeoutException, ExecutionException {

        ReadWriteTransaction rwTx = getDataBroker().newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.OPERATIONAL, NODE_0_FLOW_PATH, FLOW,true);
        Flow data = rwTx.read(LogicalDatastoreType.OPERATIONAL, NODE_0_FLOW_PATH).get().get();

        assertEquals(MAC,data.getMatch().getEthernetMatch().getEthernetSource().getAddress().getValue());
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList = ((ApplyActionsCase) data.getInstructions().getInstruction().get(0).getInstruction()).getApplyActions().getAction();
        for(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action action : actionList) {
            Action innerAction = action.getAction();
            if(innerAction instanceof SetDlSrcActionCase) {
                assertEquals(MAC, ((SetDlSrcActionCase) innerAction).getSetDlSrcAction().getAddress().getValue());
            } else if(innerAction instanceof SetDlDstActionCase) {
                assertEquals(MAC, ((SetDlDstActionCase )innerAction).getSetDlDstAction().getAddress().getValue());
            } else {
                fail("Additional action present.");
            }
        }
    }



}
