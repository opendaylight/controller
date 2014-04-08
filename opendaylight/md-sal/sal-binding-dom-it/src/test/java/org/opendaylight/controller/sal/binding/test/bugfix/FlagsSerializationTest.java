/**
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
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.connect.dom.CrudTestUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
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
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

import com.google.common.collect.ImmutableSet;

public class FlagsSerializationTest extends AbstractDataServiceTest{

    private final static Logger log = Logger
            .getLogger(FlagsSerializationTest.class.getName());

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName TABLE_ID_QNAME = QName.create(Table.QNAME, "id");
    private static final QName FLOW_ID_QNAME = QName.create(Flow.QNAME, "id");
    private static final QName FLOW_NODE_QNAME = QName.create(Flow.QNAME,
            "node");
    private static final String FLOW_ID = "1234";
    private static final short TABLE_ID = (short) 0;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId(FLOW_ID));
    private static final TableKey TABLE_KEY = new TableKey(TABLE_ID);

    private static final Map<QName, Object> NODE_KEY_BI = Collections
            .<QName, Object> singletonMap(NODE_ID_QNAME, NODE_ID);

    private static final Map<QName, Object> FLOW_KEY_BI = Collections
            .<QName, Object> singletonMap(FLOW_ID_QNAME, FLOW_ID);

    // private static final
    // org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
    // NODE_INSTANCE_ID_BI = //
    // org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
    // .node(Nodes.QNAME) //
    // .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
    // .toInstance();

    /**
     * InstanceIdentifier for flow - BA
     */

    private static final InstanceIdentifier<Flow> FLOW_INSTANCE_ID_BA = InstanceIdentifier
            .builder(Nodes.class).child(Node.class, NODE_KEY)
            .augmentation(FlowCapableNode.class).child(Table.class, TABLE_KEY)
            .child(Flow.class, FLOW_KEY).toInstance();

    // private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA =
    // InstanceIdentifier.builder(Nodes.class) //
    // .child(Node.class, NODE_KEY).toInstance();
    // private static final InstanceIdentifier<? extends DataObject>
    // FLOW_INSTANCE_ID_BA = //
    // InstanceIdentifier.builder(NODE_INSTANCE_ID_BA) //
    // .augmentation(FlowCapableNode.class)
    // .child(Table.class,TABLE_KEY)
    // .child(Flow.class, FLOW_KEY) //
    // .toInstance();

    // private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);

    // private static final NodeRef NODE_REF_UPDATE = new
    // NodeRef(NODE_INSTANCE_ID_BA_UPDATE);

    // private static final
    // org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
    // FLOW_INSTANCE_ID_BI = //
    // org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
    //
    // .node(Flows.QNAME) //
    // .nodeWithKey(Flow.QNAME, FLOW_KEY_BI) //
    // .toInstance();

    /**
     * InstanceIdentifier for flow - BI
     */
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().node(Nodes.QNAME)
            .nodeWithKey(Node.QNAME, NODE_ID_QNAME, "openflow:1").toInstance();
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier TABLE_INSTANCE_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder(this.NODE_INSTANCE_BI)
            .nodeWithKey(Table.QNAME, TABLE_ID_QNAME, 4).toInstance();
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier FLOW_INSTANCE_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder(this.TABLE_INSTANCE_BI)
            .nodeWithKey(Flow.QNAME, FLOW_ID_QNAME, 255).toInstance();

    private static final QName FLOW_FLAGS_QNAME = QName.create(Flow.QNAME,
            "flags");

    /**
     * test Test CRUD
     * 
     * @throws Exception
     */
    @Test
    public void my_testTestCRUD_Suite() throws Exception{
        FlowModFlags checkOverlapFlags = new FlowModFlags(true, false, false,
                false, false);
        ImmutableSet<String> domCheckOverlapFlags = ImmutableSet
                .<String> of("CHECK_OVERLAP");
        this.myTestFlag(checkOverlapFlags, domCheckOverlapFlags);

        FlowModFlags allFalseFlags = new FlowModFlags(false, false, false,
                false, false);
        ImmutableSet<String> domAllFalseFlags = ImmutableSet.<String> of();
        this.myTestFlag(allFalseFlags, domAllFalseFlags);

        FlowModFlags allTrueFlags = new FlowModFlags(true, true, true, true,
                true);
        ImmutableSet<String> domAllTrueFlags = ImmutableSet.<String> of(
                "CHECK_OVERLAP", "NO_BYT_COUNTS", "NO_PKT_COUNTS",
                "RESET_COUNTS", "SEND_FLOW_REM");
        this.myTestFlag(allTrueFlags, domAllTrueFlags);

        FlowModFlags nullFlags = null;
        ImmutableSet<String> domNullFlags = null;
        this.myTestFlag(null, null);
    }

    /**
     * BA && //BI Create && CRUD test flags
     * 
     * @param flagsToTest
     * @param domFlags
     * @throws Exception
     */
    private void myTestFlag(final FlowModFlags flagsToTest,
            final ImmutableSet<String> domFlags) throws Exception{
        Flow flow = this.my_createFlowBA(flagsToTest);
        Flow upFlow = this.my_createUpFlowBA(flagsToTest);

        assertNotNull(flow);

        DataObject createdObject = CrudTestUtil.doCreateTest(flow,
                this.baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doReadTest(flow, this.baDataService, FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doUpdateTest(upFlow, createdObject, this.baDataService,
                FLOW_INSTANCE_ID_BA);
        CrudTestUtil.doRemoveTest(upFlow, this.baDataService,
                FLOW_INSTANCE_ID_BA);

        log.info("Test CRUD done for : " + flow);

        /* qname is not valid to schema list flow */
        // CompositeNode cm = my_createFlowBI();
        // CrudTestUtil.doCreateTest(cm, biDataService, FLOW_INSTANCE_BI);
    }

    private Flow my_createUpFlowBA(final FlowModFlags flagsToTest){

        InstructionsBuilder instructions = new InstructionsBuilder();
        InstructionBuilder instruction = new InstructionBuilder();

        instruction.setOrder(10);
        ApplyActionsBuilder applyActions = new ApplyActionsBuilder();

        List<Action> actionList = new ArrayList<>();

        DecNwTtlBuilder decNwTtl = new DecNwTtlBuilder();
        actionList.add(new ActionBuilder()
                .setAction(
                        new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl.build())
                                .build()).setOrder(0).build());
        applyActions.setAction(actionList);

        instruction.setInstruction(new ApplyActionsCaseBuilder()
                .setApplyActions(applyActions.build()).build());

        List<Instruction> instructionList = Collections
                .<Instruction> singletonList(instruction.build());
        instructions.setInstruction(instructionList);

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

    /**
     * in use is other test - my_testTestCRUD_Suite
     * 
     * @throws Exception
     */
    // @Test
    public void testIndirectGeneration() throws Exception{

        FlowModFlags checkOverlapFlags = new FlowModFlags(true, false, false,
                false, false);
        ImmutableSet<String> domCheckOverlapFlags = ImmutableSet
                .<String> of("CHECK_OVERLAP");
        this.testFlags(checkOverlapFlags, domCheckOverlapFlags);

        FlowModFlags allFalseFlags = new FlowModFlags(false, false, false,
                false, false);
        ImmutableSet<String> domAllFalseFlags = ImmutableSet.<String> of();
        this.testFlags(allFalseFlags, domAllFalseFlags);

        FlowModFlags allTrueFlags = new FlowModFlags(true, true, true, true,
                true);
        ImmutableSet<String> domAllTrueFlags = ImmutableSet.<String> of(
                "CHECK_OVERLAP", "NO_BYT_COUNTS", "NO_PKT_COUNTS",
                "RESET_COUNTS", "SEND_FLOW_REM");
        this.testFlags(allTrueFlags, domAllTrueFlags);

        FlowModFlags nullFlags = null;
        ImmutableSet<String> domNullFlags = null;
        this.testFlags(null, null);
    }

    /**
     * methode for Test testIndirectGeneration
     * 
     * @param flagsToTest
     * @param domFlags
     * @throws Exception
     */
    private void testFlags(final FlowModFlags flagsToTest,
            final ImmutableSet<String> domFlags) throws Exception{
        Flow flow = this.createFlow(flagsToTest);
        assertNotNull(flow);

        CompositeNode domFlow = this.biDataService
                .readConfigurationData(this.mappingService
                        .toDataDom(FLOW_INSTANCE_ID_BA));

        assertNotNull(domFlow);
        org.opendaylight.yangtools.yang.data.api.Node<?> readedFlags = domFlow
                .getFirstSimpleByName(FLOW_FLAGS_QNAME);

        if(domFlags != null){
            assertNotNull(readedFlags);
            assertEquals(domFlags, readedFlags.getValue());
        } else{
            assertNull(readedFlags);
        }
        assertEquals(flagsToTest, flow.getFlags());

        DataModificationTransaction transaction = this.baDataService
                .beginTransaction();
        transaction.removeConfigurationData(FLOW_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

    }

    /**
     * methode for Test testIndirectGeneration
     * 
     * @param flagsToTest
     * @return
     * @throws Exception
     */
    private Flow createFlow(final FlowModFlags flagsToTest) throws Exception{

        DataModificationTransaction modification = this.baDataService
                .beginTransaction();

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
        actionList.add(new ActionBuilder()
                .setAction(
                        new PopMplsActionCaseBuilder().setPopMplsAction(
                                popMplsAction.build()).build()).setOrder(10)
                .build());

        applyActions.setAction(actionList);

        instruction.setInstruction(new ApplyActionsCaseBuilder()
                .setApplyActions(applyActions.build()).build());

        List<Instruction> instructionList = Collections
                .<Instruction> singletonList(instruction.build());
        instructions.setInstruction(instructionList);

        flow.setInstructions(instructions.build());
        modification.putConfigurationData(FLOW_INSTANCE_ID_BA, flow.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());

        return (Flow) this.baDataService
                .readConfigurationData(FLOW_INSTANCE_ID_BA);
    }

    /**
     * create flow
     * 
     * @param flagsToTest
     * @return
     * @throws Exception
     */
    private Flow my_createFlowBA(final FlowModFlags flagsToTest)
            throws Exception{

        DataModificationTransaction modification = this.baDataService
                .beginTransaction();

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
        actionList.add(new ActionBuilder()
                .setAction(
                        new PopMplsActionCaseBuilder().setPopMplsAction(
                                popMplsAction.build()).build()).setOrder(10)
                .build());

        applyActions.setAction(actionList);

        instruction.setInstruction(new ApplyActionsCaseBuilder()
                .setApplyActions(applyActions.build()).build());

        List<Instruction> instructionList = Collections
                .<Instruction> singletonList(instruction.build());
        instructions.setInstruction(instructionList);

        flow.setInstructions(instructions.build());
        modification.putConfigurationData(FLOW_INSTANCE_ID_BA, flow.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());

        return (Flow) this.baDataService
                .readConfigurationData(FLOW_INSTANCE_ID_BA);
    }

    /**
     * create composite node
     * 
     * @return
     * @throws Exception
     */
    private CompositeNode my_createFlowBI() throws Exception{
        // /* BI_DataService is working with XML Elements so we are building
        // Flow "add pattern" from String pattern which are able to find in the
        // end of this class */
        org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification = this.biDataService
                .beginTransaction();

        // InputStream paramInputStream = new
        // ByteArrayInputStream(getFlowTestScriptXmlToStringForBuildCompositeNode().getBytes("UTF-8"));
        // InputSource paramInputSource = new InputSource(paramInputStream);
        //
        // Document flow =
        // DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(paramInputSource);
        // Element element = flow.getDocumentElement();
        // String namespace = element.getNamespaceURI();
        // String localname = element.getLocalName();

        QName comN = QName.create("cn", "2013-12-09", "comNode");

        List<org.opendaylight.yangtools.yang.data.api.Node<?>> value = new ArrayList<org.opendaylight.yangtools.yang.data.api.Node<?>>();

        SimpleNode simpleNode = NodeFactory.createImmutableSimpleNode(
                new QName(Flow.QNAME, "flowId"), null, "flowID");

        value.add(simpleNode);

        CompositeNode node1Node = NodeFactory.createImmutableCompositeNode(
                new QName(Flows.QNAME, "flow"), null, value);

        modification.putConfigurationData(this.FLOW_INSTANCE_BI, node1Node);

        RpcResult<TransactionStatus> result = modification.commit().get();
        assertNotNull("Result of the commit should not be null.", result);
        assertEquals(
                "Successfully committed transaction should be equal to result of commit.",
                TransactionStatus.COMMITED, result.getResult());

        return this.biDataService.readConfigurationData(this.FLOW_INSTANCE_BI);
    }

    /**
     * xml schema for build flow
     * 
     * @return String
     */
    @SuppressWarnings("unused")
    private static String getFlowTestScriptXmlToStringForBuildCompositeNode(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><flow xmlns=\"urn:opendaylight:flow:inventory\"> <strict>false</strict>    <instructions>    <instruction>    <order>0</order>    <apply-actions>    <action>    <order>0</order>    <drop-action/>    </action>    </apply-actions>    </instruction>    </instructions>    <table_id>2</table_id>    <id>125</id>    <cookie_mask>255</cookie_mask>    <installHw>false</installHw>    <match>    <ethernet-match>    <ethernet-type>    <type>2048</type>    </ethernet-type>    </ethernet-match>    <ipv4-source>10.0.0.1</ipv4-source>    </match>    <hard-timeout>12</hard-timeout>    <flags>FlowModFlags [_cHECKOVERLAP=false, _rESETCOUNTS=false, _nOPKTCOUNTS=false, _nOBYTCOUNTS=false, _sENDFLOWREM=false]</flags>    <cookie>2</cookie>    <idle-timeout>34</idle-timeout>    <flow-name>FooXf2</flow-name>    <priority>2</priority>    <barrier>false</barrier></flow>";
    }
}
