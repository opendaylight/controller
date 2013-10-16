package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.inject.Inject;

import static org.mockito.Mockito.*;

public class RoutedServiceTest extends AbstractTest {

    private SalFlowService first;
    private SalFlowService second;

    private SalFlowService consumerService;

    private RoutedRpcRegistration<SalFlowService> firstReg;
    private RoutedRpcRegistration<SalFlowService> secondReg;

    @Before
    public void setUp() throws Exception {
        first = mock(SalFlowService.class,"First Flow Service");
        second = mock(SalFlowService.class,"Second Flow Service");
    }

    @Test
    public void testServiceRegistration() {

        assertNotNull(getBroker());

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                firstReg = session.addRoutedRpcImplementation(SalFlowService.class, first);
            }
        };

        /**
         * Register first provider, which register first implementation of 
         * SalFlowService
         * 
         */
        getBroker().registerProvider(provider1, getBundleContext());
        assertNotNull("Registration should not be null", firstReg);
        assertSame(first, firstReg.getInstance());
        
        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                secondReg = session.addRoutedRpcImplementation(SalFlowService.class, second);
            }
        };
        getBroker().registerProvider(provider2, getBundleContext());
        assertNotNull("Registration should not be null", firstReg);
        assertNotSame(secondReg, firstReg);


        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                consumerService = session.getRpcService(SalFlowService.class);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());

        assertNotNull("MD-SAL instance of Flow Service should be returned", consumerService);
        assertNotSame("Provider instance and consumer instance should not be same.", first, consumerService);

        NodeRef nodeOne = createNodeRef("foo:node:1");


        /**
         * Provider 1 - register itself as provider for SalFlowService
         * for nodeOne
         */
        firstReg.registerPath(NodeContext.class, nodeOne.getValue());

        /**
         * Consumer creates addFlow Message for node one and sends
         * it to the MD-SAL
         * 
         */
        AddFlowInput firstMessage = createSampleAddFlow(nodeOne,1);
        consumerService.addFlow(firstMessage);
        
        /**
         * We verify if implementation of first provider received same
         * message from MD-SAL.
         * 
         */
        verify(first).addFlow(firstMessage);
        
        /**
         * Verifies that second instance was not invoked with first
         * message
         * 
         */
        verify(second, times(0)).addFlow(firstMessage);
        
        /**
         * Second provider registers as provider for nodeTwo
         * 
         */
        NodeRef nodeTwo = createNodeRef("foo:node:2");
        secondReg.registerPath(NodeContext.class, nodeTwo.getValue());
        
        
        /**
         * Consumer sends message to nodeTwo for three times.
         * Should be processed by second instance.
         */
        AddFlowInput secondMessage = createSampleAddFlow(nodeTwo,2);
        consumerService.addFlow(secondMessage);
        consumerService.addFlow(secondMessage);
        consumerService.addFlow(secondMessage);
        
        /**
         * We verify that second was invoked 3 times, with message
         * two as parameter, first was invoked 0 times.
         * 
         */
        verify(second, times(3)).addFlow(secondMessage);
        verify(first, times(0)).addFlow(secondMessage);
        
        
        /**
         * First provider unregisters as implementation of FlowService
         * for node one
         * 
         */
        firstReg.unregisterPath(NodeContext.class, nodeOne.getValue());
        
        
        /**
         * Second provider registers as implementation for FlowService
         * for node one
         * 
         */
        secondReg.registerPath(NodeContext.class, nodeOne.getValue());
        
        /**
         * Consumer sends third message to Node 1, should be processed
         * by second instance.
         * 
         */
        AddFlowInput thirdMessage = createSampleAddFlow(nodeOne,3);
        consumerService.addFlow(thirdMessage);
        
        /**
         * We verify that first provider was invoked 0 times,
         * second provider 1 time.
         */
        verify(first,times(0)).addFlow(thirdMessage);
        verify(second).addFlow(thirdMessage);
        
    }

    private static NodeRef createNodeRef(String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder().node(Nodes.class).node(Node.class, key)
                .toInstance();

        return new NodeRef(path);
    }

    static AddFlowInput createSampleAddFlow(NodeRef node,int cookie) {
        AddFlowInputBuilder ret = new AddFlowInputBuilder();
        ret.setNode(node);
        ret.setCookie(BigInteger.valueOf(cookie));
        return ret.build();
    }
}
