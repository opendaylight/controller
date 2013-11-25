package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static org.junit.Assert.*;

public class DOMCodecBug01Test extends AbstractDataServiceTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName FLOW_ID_QNAME = QName.create(Flow.QNAME, "id");
    private static final QName FLOW_NODE_QNAME = QName.create(Flow.QNAME, "node");
    private static final long FLOW_ID = 1234;
    private static final String NODE_ID = "node:1";

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
    private static final InstanceIdentifier<? extends DataObject> FLOW_INSTANCE_ID_BA = //
    InstanceIdentifier.builder(Flows.class) //
            .child(Flow.class, FLOW_KEY) //
            .toInstance();



    /**
     *
     * Testcase for https://bugs.opendaylight.org/show_bug.cgi?id=
     *
     * Cannot compile CoDec for
     * org.opendaylight.yang.gen.v1.urn.opendaylight.flow
     * .config.rev130819.flows.Flow
     *
     * When invoking following code in the consumer, user got an
     * IllegalStateException during creation of mapping between Java DTOs and
     * data-dom.
     *
     * Exception was compilation error which was caused by incorect generation
     * of code.
     *
     * Reported by Depthi V V
     *
     */
    @Test
    public void testIndirectGeneration() throws Exception {

        ExecutorService basePool = Executors.newFixedThreadPool(2);
        ListeningExecutorService listenablePool = MoreExecutors.listeningDecorator(basePool);

        createFlow();

        Object lock = new Object();
        CreateFlowTask task1 = new CreateFlowTask(lock);
        CreateFlowTask task2 = new CreateFlowTask(lock);
        CreateFlowTask task3 = new CreateFlowTask(lock);

        ListenableFuture<Void> task1Future = listenablePool.submit(task1);
        ListenableFuture<Void> task2Future = listenablePool.submit(task2);
        ListenableFuture<Void> task3Future = listenablePool.submit(task3);


        @SuppressWarnings("unchecked")
        ListenableFuture<List<Void>> compositeFuture = Futures.allAsList(task1Future,task2Future,task3Future);

        Thread.sleep(500);
        //lock.notifyAll();
        compositeFuture.get();

        verifyDataAreStoredProperly();

        DataModificationTransaction modification2 = baDataService.beginTransaction();
        modification2.removeConfigurationData(FLOW_INSTANCE_ID_BA);

        DataObject originalData = modification2.getOriginalConfigurationData().get(FLOW_INSTANCE_ID_BA);
        assertNotNull(originalData);
        RpcResult<TransactionStatus> ret2 = modification2.commit().get();

        assertNotNull(ret2);
        assertEquals(TransactionStatus.COMMITED, ret2.getResult());

        // Data are not in the store.
        assertNull(baDataService.readOperationalData(FLOW_INSTANCE_ID_BA));

    }

    private void createFlow() throws Exception {

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
        modification.putConfigurationData(FLOW_INSTANCE_ID_BA, flow.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());
    }
    
    private void createFlow2() throws Exception {
        DataModificationTransaction modification = baDataService.beginTransaction();
        long id = 123;
        FlowKey key = new FlowKey(id, new NodeRef(NODE_INSTANCE_ID_BA));
        InstanceIdentifier<?> path1;
        FlowBuilder flow = new FlowBuilder();
        flow.setKey(key);
        MatchBuilder match = new MatchBuilder();
        Ipv4MatchBuilder ipv4Match = new Ipv4MatchBuilder();
        // ipv4Match.setIpv4Destination(new Ipv4Prefix(cliInput.get(4)));
        match.setLayer4Match(new TcpMatchBuilder().build());
        flow.setMatch(match.build());
        DropAction dropAction = new DropActionBuilder().build();
        //   ActionBuilder action = new ActionBuilder();

        //  List<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Action> actions = Collections
             //   .singletonList(action.build());
        //   flow.setAction(actions);
        flow.setPriority(2);
        System.out.println("Putting the configuration Data................");
        path1 = InstanceIdentifier.builder(Flows.class).child(Flow.class, key).toInstance();
       // DataObject cls = (DataObject) modification.readConfigurationData(path1);
        modification.putConfigurationData(path1, flow.build());
        modification.commit();

    }

    private class CreateFlowTask implements Callable<Void> {

        public CreateFlowTask(Object startSync) {
        }

        @Override
        public Void call() {
            try {
                //startSyncObject.wait();
                //Thread.sleep(500);
                createFlow();
                createFlow2();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private void verifyDataAreStoredProperly() {
        CompositeNode biFlows = biDataService.readConfigurationData(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.of(Flows.QNAME));
        assertNotNull(biFlows);
        CompositeNode biFlow = biFlows.getFirstCompositeByName(Flow.QNAME);
        assertNotNull(biFlow);
    }


}
