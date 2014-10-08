/**
* Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package test.mock;

import org.junit.Test;
import org.opendaylight.controller.frm.impl.FRMConfig;
import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.FRMTest;
import test.mock.util.SalFlowServiceMock;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FlowListenerTest extends FRMTest {

    private final FRMConfig frmConfig = FRMConfig.builder().setCleanAlienFlowsOnReconcil(false).build();

    @Test
    public void addTwoFlowsTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow2"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(2, addFlowCalls.size());
        assertEquals("DOM-1", addFlowCalls.get(1).getTransactionUri().getValue());
        assertEquals(2, addFlowCalls.get(1).getTableId().intValue());
        assertEquals(flowII, addFlowCalls.get(1).getFlowRef().getValue());

        forwardingRulesManager.close();
    }

    @Test
    public void updateFlowTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();
        addFlowCapableNode(s1Key);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).setOutGroup((long) 5).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<UpdateFlowInput> updateFlowCalls = salFlowService.getUpdateFlowCalls();
        assertEquals(1, updateFlowCalls.size());
        assertEquals("DOM-1", updateFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, updateFlowCalls.get(0).getFlowRef().getValue());

        forwardingRulesManager.close();
    }

    @Test
    public void updateFlowScopeTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        IpMatch ipMatch = new IpMatchBuilder().setIpDscp(new Dscp((short)4)).build();
        Match match = new MatchBuilder().setIpMatch(ipMatch).build();
        Flow flow = new FlowBuilder().setMatch(match).setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        ipMatch = new IpMatchBuilder().setIpDscp(new Dscp((short)5)).build();
        match = new MatchBuilder().setIpMatch(ipMatch).build();
        flow = new FlowBuilder().setMatch(match).setKey(flowKey).setTableId((short) 2).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<UpdateFlowInput> updateFlowCalls = salFlowService.getUpdateFlowCalls();
        assertEquals(1, updateFlowCalls.size());
        assertEquals("DOM-1", updateFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, updateFlowCalls.get(0).getFlowRef().getValue());
        assertEquals(ipMatch, updateFlowCalls.get(0).getUpdatedFlow().getMatch().getIpMatch());
        forwardingRulesManager.close();
    }

    @Test
    public void deleteFlowTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, flowII);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<RemoveFlowInput> removeFlowCalls = salFlowService.getRemoveFlowCalls();
        assertEquals(1, removeFlowCalls.size());
        assertEquals("DOM-1", removeFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, removeFlowCalls.get(0).getFlowRef().getValue());

        forwardingRulesManager.close();
    }
}
