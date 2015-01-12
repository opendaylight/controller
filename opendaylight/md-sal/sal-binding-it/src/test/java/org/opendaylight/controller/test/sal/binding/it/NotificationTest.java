/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

@Ignore
public class NotificationTest extends AbstractTest {

    private final FlowListener listener1 = new FlowListener();
    private final FlowListener listener2 = new FlowListener();

    private ListenerRegistration<NotificationListener> listener1Reg;
    private ListenerRegistration<NotificationListener> listener2Reg;

    private NotificationProviderService notifyProviderService;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void notificationTest() throws Exception {
        /**
         *
         * The registration of the Provider 1.
         *
         */
        AbstractTestProvider provider1 = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                notifyProviderService = session.getSALService(NotificationProviderService.class);
            }
        };

        // registerProvider method calls onSessionInitiated method above
        broker.registerProvider(provider1, getBundleContext());
        assertNotNull(notifyProviderService);

        /**
         *
         * The registration of the Consumer 1. It retrieves Notification Service
         * from MD-SAL and registers SalFlowListener as notification listener
         *
         */
        BindingAwareConsumer consumer1 = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                NotificationService notificationService = session.getSALService(NotificationService.class);
                assertNotNull(notificationService);
                listener1Reg = notificationService.registerNotificationListener(listener1);
            }
        };
        // registerConsumer method calls onSessionInitialized method above
        broker.registerConsumer(consumer1, getBundleContext());

        assertNotNull(listener1Reg);

        /**
         * The notification of type FlowAdded with cookie ID 0 is created. The
         * delay 100ms to make sure that the notification was delivered to
         * listener.
         */
        notifyProviderService.publish(flowAdded(0));
        Thread.sleep(100);

        /**
         * Check that one notification was delivered and has correct cookie.
         *
         */
        assertEquals(1, listener1.addedFlows.size());
        assertEquals(0, listener1.addedFlows.get(0).getCookie().getValue().intValue());

        /**
         * The registration of the Consumer 2. SalFlowListener is registered
         * registered as notification listener.
         */
        BindingAwareProvider provider = new BindingAwareProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                listener2Reg = session.getSALService(NotificationProviderService.class).registerNotificationListener(
                        listener2);
            }
        };

        // registerConsumer method calls onSessionInitialized method above
        broker.registerProvider(provider, getBundleContext());

        /**
         * 3 notifications are published
         */
        notifyProviderService.publish(flowAdded(5));
        notifyProviderService.publish(flowAdded(10));
        notifyProviderService.publish(flowAdded(2));

        /**
         * The delay 100ms to make sure that the notifications were delivered to
         * listeners.
         */
        Thread.sleep(100);

        /**
         * Check that 3 notification was delivered to both listeners (first one
         * received 4 in total, second 3 in total).
         *
         */
        assertEquals(4, listener1.addedFlows.size());
        assertEquals(3, listener2.addedFlows.size());

        /**
         * The second listener is closed (unregistered)
         *
         */
        listener2Reg.close();

        /**
         *
         * The notification 5 is published
         */
        notifyProviderService.publish(flowAdded(10));

        /**
         * The delay 100ms to make sure that the notification was delivered to
         * listener.
         */
        Thread.sleep(100);

        /**
         * Check that first consumer received 5 notifications in total, second
         * consumer received only three. Last notification was never received by
         * second consumer because its listener was unregistered.
         *
         */
        assertEquals(5, listener1.addedFlows.size());
        assertEquals(3, listener2.addedFlows.size());

    }

    /**
     * Creates instance of the type FlowAdded. Only cookie value is set. It is
     * used only for testing purpose.
     *
     * @param i
     *            cookie value
     * @return instance of the type FlowAdded
     */
    public static FlowAdded flowAdded(int i) {
        FlowAddedBuilder ret = new FlowAddedBuilder();
        ret.setCookie(new FlowCookie(BigInteger.valueOf(i)));
        return ret.build();
    }

    /**
     *
     * Implements
     * {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener
     * SalFlowListener} and contains attributes which keep lists of objects of
     * the type
     * {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819. NodeFlow
     * NodeFlow}. The lists are defined for flows which were added, removed or
     * updated.
     */
    private static class FlowListener implements SalFlowListener {

        List<FlowAdded> addedFlows = new ArrayList<>();
        List<FlowRemoved> removedFlows = new ArrayList<>();
        List<FlowUpdated> updatedFlows = new ArrayList<>();

        @Override
        public void onFlowAdded(FlowAdded notification) {
            addedFlows.add(notification);
        }

        @Override
        public void onFlowRemoved(FlowRemoved notification) {
            removedFlows.add(notification);
        };

        @Override
        public void onFlowUpdated(FlowUpdated notification) {
            updatedFlows.add(notification);
        }

        @Override
        public void onSwitchFlowRemoved(SwitchFlowRemoved notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNodeErrorNotification(NodeErrorNotification notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNodeExperimenterErrorNotification(
                NodeExperimenterErrorNotification notification) {
            // TODO Auto-generated method stub

        }

    }
}
