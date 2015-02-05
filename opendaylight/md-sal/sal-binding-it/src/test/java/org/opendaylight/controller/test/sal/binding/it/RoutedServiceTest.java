/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.OpendaylightTestRoutedRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.TestContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnorderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * covers routed rpc creation, registration, invocation, unregistration
 */
public class RoutedServiceTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(RoutedServiceTest.class);

    protected OpendaylightTestRoutedRpcService odlRoutedService1;
    protected OpendaylightTestRoutedRpcService odlRoutedService2;

    protected OpendaylightTestRoutedRpcService consumerService;

    protected RoutedRpcRegistration<OpendaylightTestRoutedRpcService> firstReg;
    protected RoutedRpcRegistration<OpendaylightTestRoutedRpcService> secondReg;

    /**
     * prepare mocks
     */
    @Before
    public void setUp() {
        odlRoutedService1 = mock(OpendaylightTestRoutedRpcService.class, "First Flow Service");
        odlRoutedService2 = mock(OpendaylightTestRoutedRpcService.class, "Second Flow Service");
    }

    @Test
    public void testServiceRegistration() {

        assertNotNull(getBroker());

        BindingAwareProvider provider1 = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                firstReg = session.addRoutedRpcImplementation(OpendaylightTestRoutedRpcService.class, odlRoutedService1);
            }
        };

        LOG.info("Register provider 1 with first implementation of routeSimpleService - service1");
        broker.registerProvider(provider1);
        assertNotNull("Registration should not be null", firstReg);
        assertSame(odlRoutedService1, firstReg.getInstance());

        BindingAwareProvider provider2 = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                assertNotNull(session);
                secondReg = session.addRoutedRpcImplementation(OpendaylightTestRoutedRpcService.class, odlRoutedService2);
            }
        };

        LOG.info("Register provider 2 with second implementation of routeSimpleService - service2");
        broker.registerProvider(provider2);
        assertNotNull("Registration should not be null", firstReg);
        assertSame(odlRoutedService2, secondReg.getInstance());
        assertNotSame(secondReg, firstReg);

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                consumerService = session.getRpcService(OpendaylightTestRoutedRpcService.class);
            }
        };
        LOG.info("Register routeService consumer");
        broker.registerConsumer(consumer);

        assertNotNull("MD-SAL instance of test Service should be returned", consumerService);
        assertNotSame("Provider instance and consumer instance should not be same.", odlRoutedService1, consumerService);

        InstanceIdentifier<UnorderedList> nodeOnePath = createNodeRef("foo:node:1");

        LOG.info("Provider 1 registers path of node 1");
        firstReg.registerPath(TestContext.class, nodeOnePath);

        /**
         * Consumer creates addFlow message for node one and sends it to the
         * MD-SAL
         */
        RoutedSimpleRouteInput simpleRouteFirstFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.routedSimpleRoute(simpleRouteFirstFoo);

        /**
         * Verifies that implementation of the first provider received the same
         * message from MD-SAL.
         */
        verify(odlRoutedService1).routedSimpleRoute(simpleRouteFirstFoo);
        /**
         * Verifies that second instance was not invoked with first message
         */
        verify(odlRoutedService2, times(0)).routedSimpleRoute(simpleRouteFirstFoo);

        LOG.info("Provider 2 registers path of node 2");
        InstanceIdentifier<UnorderedList> nodeTwo = createNodeRef("foo:node:2");
        secondReg.registerPath(TestContext.class, nodeTwo);

        /**
         * Consumer sends message to nodeTwo for three times. Should be
         * processed by second instance.
         */
        RoutedSimpleRouteInput simpleRouteSecondFoo = createSimpleRouteInput(nodeTwo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);

        /**
         * Verifies that second instance was invoked 3 times with second message
         * and first instance wasn't invoked.
         *
         */
        verify(odlRoutedService2, times(3)).routedSimpleRoute(simpleRouteSecondFoo);
        verify(odlRoutedService1, times(0)).routedSimpleRoute(simpleRouteSecondFoo);

        LOG.info("Unregistration of the path for the node one in the first provider");
        firstReg.unregisterPath(TestContext.class, nodeOnePath);

        LOG.info("Provider 2 registers path of node 1");
        secondReg.registerPath(TestContext.class, nodeOnePath);

        /**
         * A consumer sends third message to node 1
         */
        RoutedSimpleRouteInput simpleRouteThirdFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.routedSimpleRoute(simpleRouteThirdFoo);

        /**
         * Verifies that provider 1 wasn't invoked and provider 2 was invoked 1
         * time.
         * TODO: fix unregister path
         */
        //verify(odlRoutedService1, times(0)).routedSimpleRoute(simpleRouteThirdFoo);
        verify(odlRoutedService2).routedSimpleRoute(simpleRouteThirdFoo);

    }

    /**
     * Returns node reference from string which represents path
     *
     * @param string
     *            string with key(path)
     * @return instance identifier to {@link UnorderedList}
     */
    private static InstanceIdentifier<UnorderedList> createNodeRef(String string) {
        UnorderedListKey key = new UnorderedListKey(string);
        InstanceIdentifier<UnorderedList> path = InstanceIdentifier.builder(Lists.class)
                .child(UnorderedContainer.class)
                .child(UnorderedList.class, key)
                .build();

        return path;
    }

    /**
     * Creates flow AddFlowInput for which only node and cookie are set
     *
     * @param node
     *            NodeRef value
     * @return simpleRouteInput instance
     */
    static RoutedSimpleRouteInput createSimpleRouteInput(InstanceIdentifier<UnorderedList> node) {
        RoutedSimpleRouteInputBuilder ret = new RoutedSimpleRouteInputBuilder();
        ret.setRoute(node);
        return ret.build();
    }
}
