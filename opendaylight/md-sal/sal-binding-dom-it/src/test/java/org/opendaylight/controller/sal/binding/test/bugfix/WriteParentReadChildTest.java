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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.connect.dom.CrudTestUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.ImmutableList;

public class WriteParentReadChildTest extends AbstractDataServiceTest {

	private final static Logger log = Logger.getLogger(WriteParentReadChildTest.class.getName());
	
    private static final String FLOW_ID = "1234";    
    private static final short TABLE_ID = (short) 0;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId(FLOW_ID));
    private static final TableKey TABLE_KEY = new TableKey(TABLE_ID);
 
    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final InstanceIdentifier<Table> TABLE_INSTANCE_ID_BA = //
    InstanceIdentifier.builder(NODE_INSTANCE_ID_BA) //
            .augmentation(FlowCapableNode.class).child(Table.class, TABLE_KEY).build();

    private static final InstanceIdentifier<? extends DataObject> FLOW_INSTANCE_ID_BA = //
    InstanceIdentifier.builder(TABLE_INSTANCE_ID_BA) //
            .child(Flow.class, FLOW_KEY) //
            .toInstance();
    

    private static final String FLOW_ID_UP = "4321";
    private static final short TABLE_ID_UP = (short) 1;
    private static final String NODE_ID_UP = "node:2";
    
    private static final FlowKey FLOW_KEY_UP = new FlowKey(new FlowId(FLOW_ID_UP));
    private static final TableKey TABLE_KEY_UP = new TableKey(TABLE_ID_UP);
    private static final NodeKey NODE_KEY_UP = new NodeKey(new NodeId(NODE_ID_UP));

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA_UP = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY_UP).toInstance();
    
    private static final InstanceIdentifier<Table> TABLE_INSTANCE_ID_BA_UP = //
    InstanceIdentifier.builder(NODE_INSTANCE_ID_BA_UP) //
            .augmentation(FlowCapableNode.class).child(Table.class, TABLE_KEY_UP).build();
    
    private static final InstanceIdentifier<? extends DataObject> FLOW_INSTANCE_ID_BA_UP = //
    	    InstanceIdentifier.builder(TABLE_INSTANCE_ID_BA_UP) //
    	            .child(Flow.class, FLOW_KEY_UP) //
    	            .toInstance();
    /**
     *
     * The scenario tests writing parent node, which also contains child items
     * and then reading child directly, by specifying path to the child.
     *
     * Expected behaviour is child is returned.
     *
     * @throws Exception
     */
    @Test
    public void writeTableReadFlow() throws Exception {

        DataModificationTransaction modification = baDataService.beginTransaction();

        Flow flow = createFlow();

        Table table = new TableBuilder()
            .setKey(TABLE_KEY)
            .setFlow(ImmutableList.of(flow))
        .build();

        modification.putConfigurationData(TABLE_INSTANCE_ID_BA, table);
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());

        DataObject readedTable = baDataService.readConfigurationData(TABLE_INSTANCE_ID_BA);
        assertNotNull("Readed table should not be nul.", readedTable);
        assertTrue(readedTable instanceof Table);
        
        DataObject readedFlow = baDataService.readConfigurationData(FLOW_INSTANCE_ID_BA);
        assertNotNull("Readed flow should not be null.",readedFlow);
        assertTrue(readedFlow instanceof Flow);
        assertEquals(flow, readedFlow);
    }
    
    /**
     * crud test flow
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void my_flowTest() throws InterruptedException, ExecutionException{
    	
        Flow flow = createFlow();
        Flow flowUp = createFlowUp();
        
        DataObject createdFlow = CrudTestUtil.doCreateTest(flow, baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doReadTest(createdFlow, baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doUpdateTest(flowUp, createdFlow, baDataService, FLOW_INSTANCE_ID_BA_UP);
        CrudTestUtil.doRemoveTest(flowUp, baDataService, FLOW_INSTANCE_ID_BA_UP);
        
        log.info("Test CRUD done for : " + flow);
    }
    
    /**
     * BA
     * createFlow
     * @return
     */
    private Flow createFlow(){
    	return new FlowBuilder() //
        .setKey(FLOW_KEY) //
        .setMatch(new MatchBuilder() //
                .setVlanMatch(new VlanMatchBuilder() //
                        .setVlanId(new VlanIdBuilder() //
                                .setVlanId(new VlanId(10)) //
                                .build()) //
                        .build()) //
                .build()) //
                .setInstructions(new InstructionsBuilder() //
                    .setInstruction(ImmutableList.<Instruction>builder() //
                            .build()) //
                .build()) //
        .build();
    }
    
    private Flow createFlowUp(){
    	return new FlowBuilder() //
        .setKey(FLOW_KEY_UP) //
        .setMatch(new MatchBuilder() //
                .setVlanMatch(new VlanMatchBuilder() //
                        .setVlanId(new VlanIdBuilder() //
                                .setVlanId(new VlanId(10)) //
                                .build()) //
                        .build()) //
                .build()) //
                .setInstructions(new InstructionsBuilder() //
                    .setInstruction(ImmutableList.<Instruction>builder() //
                            .build()) //
                .build()) //
        .build();
    }
}