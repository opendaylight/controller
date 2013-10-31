package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class RoutedServiceTest extends AbstractTest {

    private SalFlowService salFlowService1;
    private SalFlowService salFlowService2;

    private SalFlowService consumerService;

    private RoutedRpcRegistration<SalFlowService> firstReg;
    private RoutedRpcRegistration<SalFlowService> secondReg;

    @Before
    public void setUp() throws Exception {
        salFlowService1 = mock(SalFlowService.class, "First Flow Service");
        salFlowService2 = mock(SalFlowService.class, "Second Flow Service");
    }

    @Test
    public void testServiceRegistration() {

        assertNotNull(getBroker());

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                firstReg = session.addRoutedRpcImplementation(SalFlowService.class, salFlowService1);
            }
        };

        /**
         * Register provider 1 with first implementation of SalFlowService -
         * service1
         * 
         */
        broker.registerProvider(provider1, getBundleContext());
        assertNotNull("Registration should not be null", firstReg);
        assertSame(salFlowService1, firstReg.getInstance());

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                secondReg = session.addRoutedRpcImplementation(SalFlowService.class, salFlowService2);
            }
        };

        /**
         * Register provider 2 with first implementation of SalFlowService -
         * service2
         * 
         */
        broker.registerProvider(provider2, getBundleContext());
        assertNotNull("Registration should not be null", firstReg);
        assertSame(salFlowService2, secondReg.getInstance());
        assertNotSame(secondReg, firstReg);

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                consumerService = session.getRpcService(SalFlowService.class);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());

        assertNotNull("MD-SAL instance of Flow Service should be returned", consumerService);
        assertNotSame("Provider instance and consumer instance should not be same.", salFlowService1, consumerService);

        NodeRef nodeOne = createNodeRef("foo:node:1");

        /**
         * Provider 1 registers path of node 1
         */
        firstReg.registerPath(NodeContext.class, nodeOne.getValue());

        /**
         * Consumer creates addFlow message for node one and sends it to the
         * MD-SAL
         * 
         */
        AddFlowInput addFlowFirstMessage = createSampleAddFlow(nodeOne, 1);
        consumerService.addFlow(addFlowFirstMessage);

        /**
         * Verifies that implementation of the first provider received the same
         * message from MD-SAL.
         * 
         */
        verify(salFlowService1).addFlow(addFlowFirstMessage);

        /**
         * Verifies that second instance was not invoked with first message
         * 
         */
        verify(salFlowService2, times(0)).addFlow(addFlowFirstMessage);

        /**
         * Provider 2 registers path of node 2
         * 
         */
        NodeRef nodeTwo = createNodeRef("foo:node:2");
        secondReg.registerPath(NodeContext.class, nodeTwo.getValue());

        /**
         * Consumer sends message to nodeTwo for three times. Should be
         * processed by second instance.
         */
        AddFlowInput AddFlowSecondMessage = createSampleAddFlow(nodeTwo, 2);
        consumerService.addFlow(AddFlowSecondMessage);
        consumerService.addFlow(AddFlowSecondMessage);
        consumerService.addFlow(AddFlowSecondMessage);

        /**
         * Verifies that second instance was invoked 3 times with second message
         * and first instance wasn't invoked.
         * 
         */
        verify(salFlowService2, times(3)).addFlow(AddFlowSecondMessage);
        verify(salFlowService1, times(0)).addFlow(AddFlowSecondMessage);

        /**
         * Unregisteration of the path for the node one in the first provider
         * 
         */
        firstReg.unregisterPath(NodeContext.class, nodeOne.getValue());

        /**
         * Provider 2 registers path of node 1
         * 
         */
        secondReg.registerPath(NodeContext.class, nodeOne.getValue());

        /**
         * A consumer sends third message to node 1
         * 
         */
        AddFlowInput AddFlowThirdMessage = createSampleAddFlow(nodeOne, 3);
        consumerService.addFlow(AddFlowThirdMessage);

        /**
         * Verifies that provider 1 wasn't invoked and provider 2 was invoked 1
         * time.
         */
        verify(salFlowService1, times(0)).addFlow(AddFlowThirdMessage);
        verify(salFlowService2).addFlow(AddFlowThirdMessage);

    }

    /**
     * Returns node reference from string which represents path
     * 
     * @param string
     *            string with key(path)
     * @return instance of the type NodeRef
     */
    private static NodeRef createNodeRef(String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder().node(Nodes.class).node(Node.class, key)
                .toInstance();

        return new NodeRef(path);
    }

    /**
     * Creates flow AddFlowInput for which only node and cookie are set
     * 
     * @param node
     *            NodeRef value
     * @param cookie
     *            integer with cookie value
     * @return AddFlowInput instance
     */
    static AddFlowInput createSampleAddFlow(NodeRef node, int cookie) {
        AddFlowInputBuilder ret = new AddFlowInputBuilder();
        ret.setNode(node);
        ret.setCookie(BigInteger.valueOf(cookie));
        return ret.build();
    }
}
