package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;











import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;











import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.impl.connect.dom.CommitHandlersTransactions;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.collect.ImmutableMap;

public class ChangeOriginatedInDomBrokerTest extends AbstractDataServiceTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName FLOW_ID_QNAME = QName.create(Flow.QNAME, "id");
    private static final QName FLOW_NODE_QNAME = QName.create(Flow.QNAME, "node");
    private static final long FLOW_ID = 1234;
    private static final String NODE_ID = "node:1";
    
    private DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modificationCapture;


    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);

    private static final FlowKey FLOW_KEY = new FlowKey(FLOW_ID, NODE_REF);

    private static final Map<QName, Object> FLOW_KEY_BI = //
    ImmutableMap.<QName, Object> of(FLOW_ID_QNAME, FLOW_ID, FLOW_NODE_QNAME, NODE_INSTANCE_ID_BI);

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier FLOW_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Flows.QNAME) //
            .nodeWithKey(Flow.QNAME, FLOW_KEY_BI) //
            .toInstance();
    
    private static final InstanceIdentifier<Flows> FLOWS_PATH_BA = //
            InstanceIdentifier.builder(Flows.class) //
                    .toInstance();
            
    
    private static final InstanceIdentifier<Flow> FLOW_INSTANCE_ID_BA = //
    InstanceIdentifier.builder(Flows.class) //
            .child(Flow.class, FLOW_KEY) //
            .toInstance();
    
    @Test
    public void simpleModifyOperation() throws Exception {
        
        registerCommitHandler();
        
        CompositeNode domflow = createXmlFlow();
        DataModificationTransaction biTransaction = biDataService.beginTransaction();
        biTransaction.putConfigurationData(FLOW_INSTANCE_ID_BI, domflow);
        RpcResult<TransactionStatus> biResult = biTransaction.commit().get();
        
        assertNotNull(modificationCapture);
        Flow flow = (Flow) modificationCapture.getCreatedConfigurationData().get(FLOW_INSTANCE_ID_BA);
        assertNotNull(flow);
        assertNotNull(flow.getMatch());
        assertEquals(TransactionStatus.COMMITED, biResult.getResult());
        
    }

    

    private void registerCommitHandler() {
        DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> flowTestCommitHandler = new DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>() {
            
            
            @Override
            public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
                    DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
                modificationCapture = modification;
                return CommitHandlersTransactions.allwaysSuccessfulTransaction(modification);
            }
            
            
        };
        Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> registration = baDataService.registerCommitHandler(FLOWS_PATH_BA, flowTestCommitHandler);
        assertNotNull(registration);
    }
    
    private CompositeNode createXmlFlow() {
        
        FlowBuilder flow = new FlowBuilder();
        MatchBuilder match = new MatchBuilder();
        VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(10);
        vlanBuilder.setVlanId(vlanIdBuilder.setVlanId(vlanId).build());
        match.setVlanMatch(vlanBuilder.build());

        flow.setKey(FLOW_KEY);
        flow.setMatch(match.build());
        flow.setNode(NODE_REF);
        InstructionsBuilder instructions = new InstructionsBuilder();
        InstructionBuilder instruction = new InstructionBuilder();
        ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        List<Action> actionList = new ArrayList<>();
        PopMplsActionBuilder popMplsAction = new PopMplsActionBuilder();
        popMplsAction.setEthernetType(34);
        actionList.add(new ActionBuilder().setAction(popMplsAction.build()).build());

        applyActions.setAction(actionList );
        


        instruction.setInstruction(applyActions.build());


        List<Instruction> instructionList = Collections.<Instruction>singletonList(instruction.build());
        instructions.setInstruction(instructionList );

        flow.setInstructions(instructions.build());
        
        CompositeNode domFlow = mappingService.toDataDom(flow.build());
        return domFlow;
    }
}
