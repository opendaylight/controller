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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.Futures;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnorderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListKey;
import org.opendaylight.yangtools.concepts.Registration;
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

    protected RoutedSimpleRoute routedSimpleRouteRpc1;
    protected RoutedSimpleRoute routedSimpleRouteRpc2;

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
        routedSimpleRouteRpc1 = mock(RoutedSimpleRoute.class, "First Flow Rpc");
        routedSimpleRouteRpc2 = mock(RoutedSimpleRoute.class, "Second Flow Rpc");
        Mockito.when(routedSimpleRouteRpc1.invoke(Mockito.<RoutedSimpleRouteInput>any()))
            .thenReturn(Futures.<RpcResult<RoutedSimpleRouteOutput>>immediateFuture(null));
        Mockito.when(routedSimpleRouteRpc2.invoke(Mockito.<RoutedSimpleRouteInput>any()))
            .thenReturn(Futures.<RpcResult<RoutedSimpleRouteOutput>>immediateFuture(null));
    }

    @Test
    public void testServiceRegistration() {
        LOG.info("Register provider 1 with first implementation of routeSimpleService - rpc1 of node 1");
        final InstanceIdentifier<UnorderedList> nodeOnePath = createNodeRef("foo:node:1");
        final InstanceIdentifier<UnorderedList> nodeTwo = createNodeRef("foo:node:2");

        Registration firstReg = rpcProviderService.registerRpcImplementations(
            ImmutableClassToInstanceMap.of(RoutedSimpleRoute.class, routedSimpleRouteRpc1),  Set.of(nodeOnePath));
        assertNotNull("Registration should not be null", firstReg);

        LOG.info("Register provider 2 with second implementation of routeSimpleService - rpc2 of node 2");

        Registration secondReg = rpcProviderService.registerRpcImplementations(
            ImmutableClassToInstanceMap.of(RoutedSimpleRoute.class, routedSimpleRouteRpc2), Set.of(nodeTwo));
        assertNotNull("Registration should not be null", firstReg);
        assertNotSame(secondReg, firstReg);

        RoutedSimpleRoute consumerService = rpcConsumerRegistry.getRpc(RoutedSimpleRoute.class);
        assertNotNull("MD-SAL instance of test Service should be returned", consumerService);
        assertNotSame("Provider instance and consumer instance should not be same.", routedSimpleRouteRpc1,
                consumerService);

        /**
         * Consumer creates addFlow message for node one and sends it to the MD-SAL.
         */
        final RoutedSimpleRouteInput simpleRouteFirstFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.invoke(simpleRouteFirstFoo);

        /**
         * Verifies that implementation of the first instance received the same message from MD-SAL.
         */
        verify(routedSimpleRouteRpc1).invoke(simpleRouteFirstFoo);
        /**
         * Verifies that second instance was not invoked with first message
         */
        verify(routedSimpleRouteRpc2, times(0)).invoke(simpleRouteFirstFoo);

        /**
         * Consumer sends message to nodeTwo for three times. Should be processed by second instance.
         */
        final RoutedSimpleRouteInput simpleRouteSecondFoo = createSimpleRouteInput(nodeTwo);
        consumerService.invoke(simpleRouteSecondFoo);
        consumerService.invoke(simpleRouteSecondFoo);
        consumerService.invoke(simpleRouteSecondFoo);

        /**
         * Verifies that second instance was invoked 3 times with second message and first instance wasn't invoked.
         */
        verify(routedSimpleRouteRpc2, times(3)).invoke(simpleRouteSecondFoo);
        verify(routedSimpleRouteRpc1, times(0)).invoke(simpleRouteSecondFoo);

        LOG.info("Unregistration of the path for the node one in the first provider");
        firstReg.close();

        LOG.info("Provider 2 registers path of node 1");
        secondReg.close();
        secondReg = rpcProviderService.registerRpcImplementations(
            ImmutableClassToInstanceMap.of(RoutedSimpleRoute.class, routedSimpleRouteRpc2), Set.of(nodeOnePath));

        /**
         * A consumer sends third message to node 1.
         */
        final RoutedSimpleRouteInput simpleRouteThirdFoo = createSimpleRouteInput(nodeOnePath);
        consumerService.invoke(simpleRouteThirdFoo);

        /**
         * Verifies that provider 1 wasn't invoked and provider 2 was invoked 1 time.
         * TODO: fix unregister path
         */
        verify(routedSimpleRouteRpc2).invoke(simpleRouteThirdFoo);
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
