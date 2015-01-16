/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.collect.ImmutableSet;

@SuppressWarnings("deprecation")
public class FlagsSerializationTest extends AbstractDataServiceTest {

    private static final String FLOW_ID = "1234";
    private static final short TABLE_ID = (short)0;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId(FLOW_ID));
    private static final TableKey TABLE_KEY = new TableKey(TABLE_ID);

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).build();

    private static final InstanceIdentifier<? extends DataObject> FLOW_INSTANCE_ID_BA = //
            NODE_INSTANCE_ID_BA.builder() //
            .augmentation(FlowCapableNode.class)
            .child(Table.class,TABLE_KEY)
            .child(Flow.class, FLOW_KEY) //
            .build();
    private static final QName FLOW_FLAGS_QNAME = QName.create(Flow.QNAME, "flags");

    @Test
    public void testIndirectGeneration() throws Exception {

        FlowModFlags checkOverlapFlags = new FlowModFlags(true,false,false,false,false);
        ImmutableSet<String> domCheckOverlapFlags = ImmutableSet.<String>of("CHECK_OVERLAP");
        testFlags(checkOverlapFlags,domCheckOverlapFlags);



        FlowModFlags allFalseFlags = new FlowModFlags(false,false,false,false,false);
        ImmutableSet<String> domAllFalseFlags = ImmutableSet.<String>of();
        testFlags(allFalseFlags,domAllFalseFlags);

        FlowModFlags allTrueFlags = new FlowModFlags(true,true,true,true,true);
        ImmutableSet<String> domAllTrueFlags = ImmutableSet.<String>of("CHECK_OVERLAP","NO_BYT_COUNTS", "NO_PKT_COUNTS", "RESET_COUNTS", "SEND_FLOW_REM");
        testFlags(allTrueFlags,domAllTrueFlags);

        testFlags(null,null);



    }

    private void testFlags(final FlowModFlags flagsToTest, final ImmutableSet<String> domFlags) throws Exception {
        Flow flow = createFlow(flagsToTest);
        assertNotNull(flow);

        CompositeNode domFlow = biDataService.readConfigurationData(mappingService.toDataDom(FLOW_INSTANCE_ID_BA));

        assertNotNull(domFlow);
        org.opendaylight.yangtools.yang.data.api.Node<?> readedFlags = domFlow.getFirstSimpleByName(FLOW_FLAGS_QNAME);

        if(domFlags != null) {
            assertNotNull(readedFlags);
            assertEquals(domFlags,readedFlags.getValue());
        } else {
            assertNull(readedFlags);
        }
        assertEquals(flagsToTest, flow.getFlags());

        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeConfigurationData(FLOW_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

    }

    private Flow createFlow(final FlowModFlags flagsToTest) throws Exception {

        DataModificationTransaction modification = baDataService.beginTransaction();

        FlowBuilder flow = new FlowBuilder();
        MatchBuilder match = new MatchBuilder();
        VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(10);
        vlanBuilder.setVlanId(vlanIdBuilder.setVlanId(vlanId).build());
        match.setVlanMatch(vlanBuilder.build());

        flow.setKey(FLOW_KEY);
        flow.setMatch(match.build());

        flow.setFlags(flagsToTest);

        InstructionsBuilder instructions = new InstructionsBuilder();
        InstructionBuilder instruction = new InstructionBuilder();

        instruction.setOrder(10);
        ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        List<Action> actionList = new ArrayList<>();
        PopMplsActionBuilder popMplsAction = new PopMplsActionBuilder();
        popMplsAction.setEthernetType(34);
        actionList.add(new ActionBuilder().setAction(new PopMplsActionCaseBuilder().setPopMplsAction(popMplsAction.build()).build()).setOrder(10).build());

        applyActions.setAction(actionList );

        instruction.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions.build()).build());


        List<Instruction> instructionList = Collections.<Instruction>singletonList(instruction.build());
        instructions.setInstruction(instructionList );

        flow.setInstructions(instructions.build());
        modification.putConfigurationData(FLOW_INSTANCE_ID_BA, flow.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());
        return (Flow) baDataService.readConfigurationData(FLOW_INSTANCE_ID_BA);
    }
}
