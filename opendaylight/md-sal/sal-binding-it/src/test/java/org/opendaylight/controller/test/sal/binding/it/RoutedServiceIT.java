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

import com.google.common.util.concurrent.Futures;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.OpendaylightTestRoutedRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnorderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListKey;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Covers routed rpc creation, registration, invocation, unregistration.
 */
public class RoutedServiceIT extends AbstractIT {

    private static final Logger LOG = LoggerFactory
            .getLogger(RoutedServiceIT.class);

    protected OpendaylightTestRoutedRpcService odlRoutedService1;
    protected OpendaylightTestRoutedRpcService odlRoutedService2;

    @Inject
    @Filter(timeout = 120 * 1000)
    RpcProviderService rpcProviderService;

    @Inject
    @Filter(timeout = 120 * 1000)
    RpcConsumerRegistry rpcConsumerRegistry;

    /**
     * Prepare mocks.
     */
    @Before
    public void setUp() {
        odlRoutedService1 = mock(OpendaylightTestRoutedRpcService.class, "First Flow Service");
        odlRoutedService2 = mock(OpendaylightTestRoutedRpcService.class, "Second Flow Service");
        Mockito.when(odlRoutedService1.routedSimpleRoute(Mockito.<RoutedSimpleRouteInput>any()))
            .thenReturn(Futures.<RpcResult<RoutedSimpleRouteOutput>>immediateFuture(null));
        Mockito.when(odlRoutedService2.routedSimpleRoute(Mockito.<RoutedSimpleRouteInput>any()))
            .thenReturn(Futures.<RpcResult<RoutedSimpleRouteOutput>>immediateFuture(null));
    }

    @Test
    public void testServiceRegistration() {
        LOG.info("Register provider 1 with first implementation of routeSimpleService - service1 of node 1");
        final InstanceIdentifier<UnorderedList> nodeOnePath = createNodeRef("foo:node:1");
        final InstanceIdentifier<UnorderedList> nodeTwo = createNodeRef("foo:node:2");

        ObjectRegistration<OpendaylightTestRoutedRpcService> firstReg = rpcProviderService.registerRpcImplementation(
            OpendaylightTestRoutedRpcService.class, odlRoutedService1,  Set.of(nodeOnePath));
        assertNotNull("Registration should not be null", firstReg);
        assertSame(odlRoutedService1, firstReg.getInstance());

        LOG.info("Register provider 2 with second implementation of routeSimpleService - service2 of node 2");

        ObjectRegistration<OpendaylightTestRoutedRpcService> secondReg = rpcProviderService.registerRpcImplementation(
            OpendaylightTestRoutedRpcService.class, odlRoutedService2, Set.of(nodeTwo));
        assertNotNull("Registration should not be null", firstReg);
        assertSame(odlRoutedService2, secondReg.getInstance());
        assertNotSame(secondReg, firstReg);

        OpendaylightTestRoutedRpcService consumerService =
                rpcConsumerRegistry.getRpcService(OpendaylightTestRoutedRpcService.class);
        assertNotNull("MD-SAL instance of test Service should be returned", consumerService);
        assertNotSame("Provider instance and consumer instance should not be same.", odlRoutedService1,
                consumerService);

        /**
         * Consumer creates addFlow message for node one and sends it to the MD-SAL.
         */
        final RoutedSimpleRouteInput simpleRouteFirstFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.routedSimpleRoute(simpleRouteFirstFoo);

        /**
         * Verifies that implementation of the first provider received the same message from MD-SAL.
         */
        verify(odlRoutedService1).routedSimpleRoute(simpleRouteFirstFoo);
        /**
         * Verifies that second instance was not invoked with first message
         */
        verify(odlRoutedService2, times(0)).routedSimpleRoute(simpleRouteFirstFoo);

        /**
         * Consumer sends message to nodeTwo for three times. Should be processed by second instance.
         */
        final RoutedSimpleRouteInput simpleRouteSecondFoo = createSimpleRouteInput(nodeTwo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);
        consumerService.routedSimpleRoute(simpleRouteSecondFoo);

        /**
         * Verifies that second instance was invoked 3 times with second message and first instance wasn't invoked.
         */
        verify(odlRoutedService2, times(3)).routedSimpleRoute(simpleRouteSecondFoo);
        verify(odlRoutedService1, times(0)).routedSimpleRoute(simpleRouteSecondFoo);

        LOG.info("Unregistration of the path for the node one in the first provider");
        firstReg.close();

        LOG.info("Provider 2 registers path of node 1");
        secondReg.close();
        secondReg = rpcProviderService.registerRpcImplementation(
            OpendaylightTestRoutedRpcService.class, odlRoutedService2, Set.of(nodeOnePath));

        /**
         * A consumer sends third message to node 1.
         */
        final RoutedSimpleRouteInput simpleRouteThirdFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.routedSimpleRoute(simpleRouteThirdFoo);

        /**
         * Verifies that provider 1 wasn't invoked and provider 2 was invoked 1 time.
         * TODO: fix unregister path
         */
        //verify(odlRoutedService1, times(0)).routedSimpleRoute(simpleRouteThirdFoo);
        verify(odlRoutedService2).routedSimpleRoute(simpleRouteThirdFoo);
    }

    /**
     * Returns node reference from string which represents path.
     *
     * @param string string with key(path)
     * @return instance identifier to {@link UnorderedList}
     */
    private static InstanceIdentifier<UnorderedList> createNodeRef(final String string) {
        return InstanceIdentifier.builder(Lists.class)
                .child(UnorderedContainer.class)
                .child(UnorderedList.class, new UnorderedListKey(string))
                .build();
    }

    /**
     * Creates flow AddFlowInput for which only node and cookie are set.
     *
     * @param node NodeRef value
     * @return simpleRouteInput instance
     */
    static RoutedSimpleRouteInput createSimpleRouteInput(final InstanceIdentifier<UnorderedList> node) {
        return new RoutedSimpleRouteInputBuilder().setRoute(node).build();
    }
}
