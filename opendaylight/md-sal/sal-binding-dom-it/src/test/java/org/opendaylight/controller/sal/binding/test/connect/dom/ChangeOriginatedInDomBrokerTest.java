/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;

// FIXME: Migrate to use new Data Broker APIs
@SuppressWarnings("deprecation")
public class ChangeOriginatedInDomBrokerTest extends AbstractDataServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeOriginatedInDomBrokerTest.class);

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName FLOW_ID_QNAME = QName.create(Flow.QNAME, "id");
    private static final QName TABLE_ID_QNAME = QName.create(Table.QNAME, "id");

    private static final String NODE_ID = "node:1";
    private static final FlowId FLOW_ID = new FlowId("1234");
    private static final Short TABLE_ID = Short.valueOf((short) 0);

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final FlowKey FLOW_KEY = new FlowKey(FLOW_ID);

    private final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> modificationCapture = SettableFuture.create();

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).build();

    private static final Map<QName, Object> FLOW_KEY_BI = //
    ImmutableMap.<QName, Object> of(FLOW_ID_QNAME, FLOW_ID.getValue());

    private static final Map<QName, Object> TABLE_KEY_BI = //
    ImmutableMap.<QName, Object> of(TABLE_ID_QNAME, TABLE_ID);;

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier FLOW_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .nodeWithKey(Table.QNAME, TABLE_KEY_BI) //
            .nodeWithKey(Flow.QNAME, FLOW_KEY_BI) //
            .build();
    private static final TableKey TABLE_KEY_BA = new TableKey((short) 0);

    private static final InstanceIdentifier<Flow> FLOWS_PATH_BA = //
            NODE_INSTANCE_ID_BA.builder() //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class, TABLE_KEY_BA) //
            .child(Flow.class) //
            .build();

    private static final InstanceIdentifier<Flow> FLOW_INSTANCE_ID_BA = //
    FLOWS_PATH_BA.firstIdentifierOf(Table.class).child(Flow.class, FLOW_KEY);

    @Test
    public void simpleModifyOperation() throws Exception {

        assertNull(biDataService.readConfigurationData(FLOW_INSTANCE_ID_BI));

        registerChangeListener();

        CompositeNode domflow = createTestFlow();
        DataModificationTransaction biTransaction = biDataService.beginTransaction();
        biTransaction.putConfigurationData(FLOW_INSTANCE_ID_BI, domflow);
        RpcResult<TransactionStatus> biResult = biTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, biResult.getResult());
        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = modificationCapture.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(event);
        LOG.info("Created Configuration :{}",event.getCreatedConfigurationData());
        Flow flow = (Flow) event.getCreatedConfigurationData().get(FLOW_INSTANCE_ID_BA);
        assertNotNull(flow);
        assertNotNull(flow.getMatch());
        assertEquals(TransactionStatus.COMMITED, biResult.getResult());

    }

    private void registerChangeListener() {
        baDataService.registerDataChangeListener(FLOWS_PATH_BA, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
                LOG.info("Data Change listener invoked.");
                modificationCapture.set(change);
            }
        });
    }

    private CompositeNode createTestFlow() {
        FlowBuilder flow = new FlowBuilder();
        flow.setKey(FLOW_KEY);
        Short tableId = 0;
        flow.setTableId(tableId);
        MatchBuilder match = new MatchBuilder();
        match.setIpMatch(new IpMatchBuilder().setIpProto(IpVersion.Ipv4).build());
        Ipv4MatchBuilder ipv4Match = new Ipv4MatchBuilder();
        // ipv4Match.setIpv4Destination(new Ipv4Prefix(cliInput.get(4)));
        Ipv4Prefix prefix = new Ipv4Prefix("10.0.0.1/24");
        ipv4Match.setIpv4Destination(prefix);
        Ipv4Match i4m = ipv4Match.build();
        match.setLayer3Match(i4m);
        flow.setMatch(match.build());



        // Create a drop action
        /*
         * Note: We are mishandling drop actions DropAction dropAction = new
         * DropActionBuilder().build(); ActionBuilder ab = new ActionBuilder();
         * ab.setAction(dropAction);
         */

        DecNwTtl decNwTtl = new DecNwTtlBuilder().build();
        ActionBuilder ab = new ActionBuilder();
        ActionKey actionKey = new ActionKey(0);
        ab.setKey(actionKey );
        ab.setAction(new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl).build());

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setOrder(0);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        // Add our instructions to the flow
        flow.setInstructions(isb.build());

        flow.setPriority(2);
        flow.setFlowName("Foo Name");
        CompositeNode domFlow = mappingService.toDataDom(flow.build());
        return domFlow;
    }
}
