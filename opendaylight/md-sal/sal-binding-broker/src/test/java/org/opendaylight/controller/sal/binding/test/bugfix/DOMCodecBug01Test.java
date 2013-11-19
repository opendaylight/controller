package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.*;

/**
 * 
 * Testcase for https://bugs.opendaylight.org/show_bug.cgi?id=144
 * 
 * Cannot compile CoDec for org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow
 * 
 * @author ttkacik
 *
 */
public class DOMCodecBug01Test extends AbstractDataServiceTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName FLOW_ID_QNAME = QName.create(Flow.QNAME, "id");
    private static final QName FLOW_NODE_QNAME = QName.create(Flow.QNAME, "node");
    
    private static final long FLOW_ID = 1234;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder().node(Nodes.class)
            .node(Node.class, NODE_KEY).toInstance();
    
    
    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);
    
    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
                    .node(Nodes.QNAME) //
                    .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
                    .toInstance();
    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);
    
    private static final FlowKey FLOW_KEY = new FlowKey(FLOW_ID, NODE_REF);

    private static final Map<QName, Object> FLOW_KEY_BI = //
            ImmutableMap.<QName,Object>of(FLOW_ID_QNAME, FLOW_ID, FLOW_NODE_QNAME, NODE_REF);


    

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier FLOW_INSTANCE_ID_BI = //
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
                    .node(Flows.QNAME) //
                    .nodeWithKey(Flow.QNAME, FLOW_KEY_BI) //
                    .toInstance();
    private static final InstanceIdentifier<? extends DataObject> FLOW_INSTANCE_ID_BA = //
            InstanceIdentifier.builder() //
                .node(Flows.class) //
                .node(Flow.class, FLOW_KEY) //
                .toInstance();
    /**
     * 
     * When invoking following code in the consumer, user got an
     * IllegalStateException during creation of mapping between Java DTOs and
     * data-dom.
     * 
     * Exception was compilation error which was caused by incorect generation
     * of code.
     * 
     * 
     */
    @Test
    public void testIndirectGeneration() throws Exception {

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

        modification.putConfigurationData(FLOW_INSTANCE_ID_BA, flow.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());
        
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

    private void verifyDataAreStoredProperly() {
        CompositeNode biFlow = biDataService.readConfigurationData(FLOW_INSTANCE_ID_BI);
        assertNotNull(biFlow);
        CompositeNode biMatch = biFlow.getFirstCompositeByName(QName.create(Flow.QNAME,Match.QNAME.getLocalName()));
        assertNotNull(biMatch);
    }
}
