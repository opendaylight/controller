/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.util.it;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.test.sal.binding.it.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;


public class AbstractLazyNotificationTransformerTest extends AbstractTest {

    private NotificationProviderService notifyProviderService;

    @Before
    public void setUp() throws Exception {
    }

    /*
     * Check to make sure that the AbstractLazyNotificationTransformer meets its contracts:
     *
     * 1)  That it does not subscribe for notifications it consumes until another module subscribes for
     *     notifications it produces
     * 2)  That it does subscribe for notifications it consumes when another module subscribes for
     *     notifications it produces
     * 3)  That if there are modules that subscribe to notifications it provides before it is instantiated
     *     that it immediately subscribes to notifications it consumes
     *
     * Note: The reason this is a single test case instead of a test case per contract is because pax-exam
     *       starts up and tears down a new OSGI container for each test case, which is hugely expensive and
     *       time consuming.  Since these contracts can be checked within a single container, they are checked
     *       within a single test.
     *
     */
    @Test
    public void lazyNotificationTransformerTest() throws Exception {
        //  Check to make sure we have a broker
        assertNotNull(broker);
        checkContractDoesNotSubscribeUntilSubscribedTo();
        checkContractDoesSubscribeWhenSubscribedTo();
        checkContractSubscribeWhenSubscriptionsPreExist();

    }

    private void publishFlowAdded() throws InterruptedException {
        notifyProviderService.publish(new FlowAddedBuilder().build());
        // Sleep briefly to make sure that there is time for the notifications to be delivered.
        Thread.sleep(100);
    }

    private TestConsumer newConsumer() {
        TestConsumer consumer = new TestConsumer();
        // registerConsumer method calls onSessionInitialized method above
        broker.registerConsumer(consumer, getBundleContext());
        return consumer;
    }

    private TestLazyNotificationTransformer newLazyNotificationTransformer() {
        // Create our LazyNotificationTransformer
        TestLazyNotificationTransformer transformer = new TestLazyNotificationTransformer();
        // register our LazyNotificationProvider
        broker.registerProvider(transformer, getBundleContext());
        // Steal the notification provider service so it can be used by the test case
        notifyProviderService = transformer.stealNotificationServiceProvider();
        assertNotNull(notifyProviderService);
        return transformer;
    }
    /*
     * Checks the contract
     * 1)  That it does not subscribe for notifications it consumes until another module subscribes for
     *     notifications it produces
     */
    private void checkContractDoesNotSubscribeUntilSubscribedTo() throws Exception {
        TestLazyNotificationTransformer transformer = newLazyNotificationTransformer();
        TestConsumer consumer = newConsumer();
        // Note, the consumer is *not* subscribed, which should mean the transformer is not subscribed
        publishFlowAdded();
        //  Transformer should not be subscribed yet, and so should not receive notifications
        transformer.assertNotificationCount(0);
        //  Consumer should not receive notifications from the transformer as it is not subscribed for events
        consumer.assertNotificationCount(0);
        transformer.close();
    }

    /*
     * Checks the contract
     * 2)  That it does subscribe for notifications it consumes when another module subscribes for
     *     notifications it produces
     */
    private void checkContractDoesSubscribeWhenSubscribedTo() throws Exception {
        TestLazyNotificationTransformer transformer = newLazyNotificationTransformer();
        TestConsumer consumer = newConsumer();
        // Subscribe the consumer, which should mean the transformer is subscribed
        Registration<NotificationListener> listenerRegistration = notifyProviderService.registerNotificationListener(consumer);
        publishFlowAdded();
        // transformer should now have subscribed for its notifications since consumer has subscribed
        transformer.assertNotificationCount(1);
        // consumer should receive its notification
        consumer.assertNotificationCount(1);
        transformer.close();
        listenerRegistration.close();
    }

    /*
     * Checks the contract
     * 3)  That if there are modules that subscribe to notifications it provides before it is instantiated
     *     that it immediately subscribes to notifications it consumes
     */
    private void checkContractSubscribeWhenSubscriptionsPreExist() throws Exception {
        TestConsumer consumer = newConsumer();
        // Subscribe the consumer, which should mean the transformer is subscribed
        Registration<NotificationListener> listenerRegistration = notifyProviderService.registerNotificationListener(consumer);
        // Now create a transformer when we already have subscribers to events it generates.
        TestLazyNotificationTransformer transformer = newLazyNotificationTransformer();
        publishFlowAdded();
        // transformer should now have subscribed for its notifications since consumer has subscribed
        transformer.assertNotificationCount(1);
        // consumer should receive its notification
        consumer.assertNotificationCount(1);
        transformer.close();
        listenerRegistration.close();
    }
}
