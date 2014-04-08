/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.connect.dom.CrudTestUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
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

public class FlowCrudTest extends AbstractDataServiceTest{

	private final static Logger log = Logger.getLogger(WriteParentReadChildTest.class.getName());
	
    private static final String FLOW_ID = "1234";    
    private static final short TABLE_ID = (short) 0;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId(FLOW_ID));
    private static final TableKey TABLE_KEY = new TableKey(TABLE_ID);

    private static final InstanceIdentifier<Flow> FLOW_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class).child(Node.class, NODE_KEY)
    		.augmentation(FlowCapableNode.class).child(Table.class, TABLE_KEY)
    		.child(Flow.class, FLOW_KEY).toInstance();
	
    @Test
    public void my_flowTest() throws InterruptedException, ExecutionException{
    	
        Flow flow = createFlow();
        Flow flowUp = createFlowUp();
        
        DataObject createdFlow = CrudTestUtil.doCreateTest(flow, baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doReadTest(createdFlow, baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doUpdateTest(flowUp, createdFlow, baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doRemoveTest(flowUp, baDataService, FLOW_INSTANCE_ID_BA);
        
        log.info("Test CRUD done for : " + flow + " and updated on : " + flowUp);
    }
	
    private Flow createFlow(){
    	
    	InstructionsBuilder instructions = new InstructionsBuilder();
        InstructionBuilder instruction = new InstructionBuilder();
        
        instruction.setOrder(10);
        ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        
        List<Action> actionList = new ArrayList<>();
        
        ActionBuilder action = new ActionBuilder();
        
        DecNwTtlBuilder decNwTtl = new DecNwTtlBuilder();
        actionList.add(new ActionBuilder().setAction(new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl.build()).build()).setOrder(0).build());
        applyActions.setAction(actionList);
       
        instruction.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions.build()).build());


        List<Instruction> instructionList = Collections.<Instruction>singletonList(instruction.build());
        instructions.setInstruction(instructionList );
    	
    	
    	return new FlowBuilder() //
        .setKey(FLOW_KEY) //
        .setMatch(new MatchBuilder() //
                .setVlanMatch(new VlanMatchBuilder() //
                        .setVlanId(new VlanIdBuilder() //
                                .setVlanId(new VlanId(10)) //
                                .build()) //
                        .build()) //
                .build()) //
        .setInstructions(instructions.build())//
        .build();
    }
	
    private Flow createFlowUp(){
    	
    	InstructionsBuilder instructions = new InstructionsBuilder();
        InstructionBuilder instruction = new InstructionBuilder();
        
        instruction.setOrder(10);
        ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        
        List<Action> actionList = new ArrayList<>();
        
        ActionBuilder action = new ActionBuilder();
               
        PopMplsActionBuilder popMplsAction = new PopMplsActionBuilder();
        popMplsAction.setEthernetType(34);
        actionList.add(new ActionBuilder().setAction(new PopMplsActionCaseBuilder().setPopMplsAction(popMplsAction.build()).build()).setOrder(10).build());
        
        applyActions.setAction(actionList );

        instruction.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions.build()).build());


        List<Instruction> instructionList = Collections.<Instruction>singletonList(instruction.build());
        instructions.setInstruction(instructionList );
    	
    	return new FlowBuilder() //
        .setKey(FLOW_KEY) //
        .setMatch(new MatchBuilder() //
                .setVlanMatch(new VlanMatchBuilder() //
                        .setVlanId(new VlanIdBuilder() //
                                .setVlanId(new VlanId(10)) //
                                .build()) //
                        .build()) //
                .build()) //
        .setInstructions(instructions.build()) //
         
        .build();
    }
}
