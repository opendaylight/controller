/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package test.mock;

import org.junit.Test;
import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.FRMTest;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.SalMeterServiceMock;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MeterListenerTest extends FRMTest {
    RpcProviderRegistry rpcProviderRegistryMock = new RpcProviderRegistryMock();
    NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void addTwoMetersTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcProviderRegistryMock);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        MeterKey meterKey = new MeterKey(new MeterId((long) 2000));
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meterKey);
        Meter meter = new MeterBuilder().setKey(meterKey).setMeterName("meter_one").build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        SalMeterServiceMock salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        List<AddMeterInput> addMeterCalls = salMeterService.getAddMeterCalls();
        assertEquals(1, addMeterCalls.size());
        assertEquals("DOM-0", addMeterCalls.get(0).getTransactionUri().getValue());

        meterKey = new MeterKey(new MeterId((long) 2001));
        meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meterKey);
        meter = new MeterBuilder().setKey(meterKey).setMeterName("meter_two").setBarrier(true).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        addMeterCalls = salMeterService.getAddMeterCalls();
        assertEquals(2, addMeterCalls.size());
        assertEquals("DOM-1", addMeterCalls.get(1).getTransactionUri().getValue());
        assertEquals(meterII, addMeterCalls.get(1).getMeterRef().getValue());

        forwardingRulesManager.close();
    }

    @Test
    public void updateMeterTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcProviderRegistryMock);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        MeterKey meterKey = new MeterKey(new MeterId((long) 2000));
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meterKey);
        Meter meter = new MeterBuilder().setKey(meterKey).setMeterName("meter_one").setBarrier(false).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        SalMeterServiceMock salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        List<AddMeterInput> addMeterCalls = salMeterService.getAddMeterCalls();
        assertEquals(1, addMeterCalls.size());
        assertEquals("DOM-0", addMeterCalls.get(0).getTransactionUri().getValue());

        meter = new MeterBuilder().setKey(meterKey).setMeterName("meter_two").setBarrier(true).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        List<UpdateMeterInput> updateMeterCalls = salMeterService.getUpdateMeterCalls();
        assertEquals(1, updateMeterCalls.size());
        assertEquals("DOM-1", updateMeterCalls.get(0).getTransactionUri().getValue());
        assertEquals(meterII, updateMeterCalls.get(0).getMeterRef().getValue());

        forwardingRulesManager.close();
    }

    @Test
    public void removeMeterTest() throws Exception {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcProviderRegistryMock);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        MeterKey meterKey = new MeterKey(new MeterId((long) 2000));
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meterKey);
        Meter meter = new MeterBuilder().setKey(meterKey).setMeterName("meter_one").build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        SalMeterServiceMock salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        List<AddMeterInput> addMeterCalls = salMeterService.getAddMeterCalls();
        assertEquals(1, addMeterCalls.size());
        assertEquals("DOM-0", addMeterCalls.get(0).getTransactionUri().getValue());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, meterII);
        assertCommit(writeTx.submit());
        salMeterService = (SalMeterServiceMock) forwardingRulesManager.getSalMeterService();
        List<RemoveMeterInput> removeMeterCalls = salMeterService.getRemoveMeterCalls();
        assertEquals(1, removeMeterCalls.size());
        assertEquals("DOM-1", removeMeterCalls.get(0).getTransactionUri().getValue());
        assertEquals(meterII, removeMeterCalls.get(0).getMeterRef().getValue());

        forwardingRulesManager.close();
    }

}
