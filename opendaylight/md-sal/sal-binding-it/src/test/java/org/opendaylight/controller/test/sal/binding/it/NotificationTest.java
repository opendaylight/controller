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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * testing nitification delivery
 */
public class NotificationTest extends AbstractTest {

    protected InventoryListener listener1;
    protected InventoryListener listener2;

    protected ListenerRegistration<NotificationListener> listener1Reg;
    protected ListenerRegistration<NotificationListener> listener2Reg;

    protected NotificationProviderService notifyProviderService;

    /**
     * prepare notification listeners
     */
    @Before
    public void setUp() {
        listener1 = new InventoryListener();
        listener2 = new InventoryListener();
    }

    /**
     * Test notification publish mechanism
     *
     * @throws Exception
     */
    @Test
    public void notificationTest() throws Exception {
        /**
         * The registration of the Provider 1.
         */
        AbstractTestProvider provider1 = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                notifyProviderService = session.getSALService(NotificationProviderService.class);
            }
        };

        // registerProvider method calls onSessionInitiated method above
        broker.registerProvider(provider1);
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
        broker.registerConsumer(consumer1);

        assertNotNull(listener1Reg);

        /**
         * The notification of type FlowAdded with cookie ID 0 is created. The
         * delay 100ms to make sure that the notification was delivered to
         * listener.
         */
        notifyProviderService.publish(createNodeUpdatedNotification(0));
        Thread.sleep(100);

        /**
         * Check that one notification was delivered and has correct cookie.
         *
         */
        assertEquals(1, listener1.updatedNodes.size());
        assertEquals("0", listener1.updatedNodes.get(0).getId().getValue());

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
        broker.registerProvider(provider);

        /**
         * 3 notifications are published
         */
        notifyProviderService.publish(createNodeUpdatedNotification(5));
        notifyProviderService.publish(createNodeUpdatedNotification(10));
        notifyProviderService.publish(createNodeUpdatedNotification(2));

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
        assertEquals(4, listener1.updatedNodes.size());
        assertEquals(3, listener2.updatedNodes.size());

        /**
         * The second listener is closed (unregistered)
         *
         */
        listener2Reg.close();

        /**
         *
         * The notification 5 is published
         */
        notifyProviderService.publish(createNodeUpdatedNotification(10));

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
        assertEquals(5, listener1.updatedNodes.size());
        assertEquals(3, listener2.updatedNodes.size());

    }

    /**
     * Creates instance of the type FlowAdded. Only cookie value is set. It is
     * used only for testing purpose.
     *
     * @param i
     *            cookie value
     * @return instance of the type FlowAdded
     */
    public static NodeUpdated createNodeUpdatedNotification(int i) {
        NodeUpdatedBuilder nodeUpdatedBld = new NodeUpdatedBuilder();
        nodeUpdatedBld.setId(new NodeId(String.valueOf(i)));
        return nodeUpdatedBld.build();
    }

    /**
     * Implements
     * {@link OpendaylightInventoryListener} and contains attributes which keep lists of objects of
     * the type {@link NodeUpdated} and {@link NodeRemoved}.
     * The lists are defined for nodes which were added or removed.
     */
    protected static class InventoryListener implements OpendaylightInventoryListener {

        List<NodeUpdated> updatedNodes = new ArrayList<>();

        @Override
        public void onNodeConnectorRemoved(NodeConnectorRemoved arg0) {
            // NOOP
        }

        @Override
        public void onNodeConnectorUpdated(NodeConnectorUpdated arg0) {
            // NOOP
        }

        @Override
        public void onNodeRemoved(NodeRemoved arg0) {
            // NOOP
        }

        @Override
        public void onNodeUpdated(NodeUpdated arg0) {
            updatedNodes.add(arg0);
        }
    }
}
