/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListener;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Unit tests for DOMNotificationRouter.
 *
 * @author Thomas Pantelis
 */
public class DOMNotificationRouterTest {
    private static final ContainerNode BODY = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
        new NodeIdentifier(QName.create(TestModel.TEST_QNAME.getModule(), "test-notification")))
            .withChild(ImmutableNodes.leafNode(QName.create(TestModel.TEST_QNAME.getModule(), "value-leaf"), "foo"))
                .build();
    private static final Instant INSTANT = Instant.now();

    private static SchemaPath notificationSchemaPath;

    private final org.opendaylight.mdsal.dom.broker.DOMNotificationRouter mdsalRouter =
            org.opendaylight.mdsal.dom.broker.DOMNotificationRouter.create(16);
    private final DOMNotificationRouter legacyRouter =
            DOMNotificationRouter.create(mdsalRouter, mdsalRouter, mdsalRouter);
    private final TestLegacyDOMNotificationListener testLegacyListener = new TestLegacyDOMNotificationListener();
    private final TestMdsalDOMNotificationListener testMdsalListener = new TestMdsalDOMNotificationListener();

    @BeforeClass
    public static void staticSetup() {
        final SchemaContext schemaContext = TestModel.createTestContext();

        Module testModule = schemaContext.findModule("odl-datastore-test", TestModel.TEST_QNAME.getRevision()).get();
        NotificationDefinition notificationDefinition = null;
        for (NotificationDefinition def: testModule.getNotifications()) {
            if (def.getQName().getLocalName().equals("test-notification")) {
                notificationDefinition = def;
                break;
            }
        }

        assertNotNull("test-notification not found in " + testModule.getNotifications(), notificationDefinition);
        notificationSchemaPath = notificationDefinition.getPath();
    }

    @Test
    public void testLegacyListenerAndPublish() throws InterruptedException, ExecutionException, TimeoutException {
        final ListenerRegistration<TestLegacyDOMNotificationListener> reg =
                legacyRouter.registerNotificationListener(testLegacyListener, notificationSchemaPath);

        legacyRouter.putNotification(new TestLegacyDOMNotification()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, null);

        legacyRouter.offerNotification(new TestLegacyDOMNotification()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, null);

        legacyRouter.offerNotification(new TestLegacyDOMNotification(), 100, TimeUnit.MILLISECONDS)
            .get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, null);

        legacyRouter.offerNotification(new TestLegacyDOMEvent()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, Date.from(INSTANT));

        reg.close();

        legacyRouter.offerNotification(new TestLegacyDOMNotification()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyNotReceived();
    }

    @Test
    public void testLegacyListenerAndMdsalPublish()
            throws InterruptedException, ExecutionException, TimeoutException {
        legacyRouter.registerNotificationListener(testLegacyListener, notificationSchemaPath);

        mdsalRouter.offerNotification(new TestMdsalDOMNotification()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, null);

        mdsalRouter.offerNotification(new TestMdsalDOMEvent()).get(5, TimeUnit.SECONDS);
        testLegacyListener.verifyReceived(notificationSchemaPath, BODY, Date.from(INSTANT));
    }

    @Test
    public void testMdsalListenerAndLegacyPublish()
            throws InterruptedException, ExecutionException, TimeoutException {
        mdsalRouter.registerNotificationListener(testMdsalListener, notificationSchemaPath);

        legacyRouter.offerNotification(new TestLegacyDOMNotification()).get(5, TimeUnit.SECONDS);
        testMdsalListener.verifyReceived(notificationSchemaPath, BODY, null);

        legacyRouter.offerNotification(new TestLegacyDOMEvent()).get(5, TimeUnit.SECONDS);
        testMdsalListener.verifyReceived(notificationSchemaPath, BODY, INSTANT);
    }

    @Test
    public void testRegisterSubscriptionListener() throws InterruptedException, ExecutionException, TimeoutException {
        TestLegacyDOMNotificationSubscriptionListener listener = new TestLegacyDOMNotificationSubscriptionListener();
        final ListenerRegistration<TestLegacyDOMNotificationSubscriptionListener> subscriptionReg =
                legacyRouter.registerSubscriptionListener(listener);

        listener.verifyReceived();

        final ListenerRegistration<TestLegacyDOMNotificationListener> listenerReg =
                legacyRouter.registerNotificationListener(testLegacyListener, notificationSchemaPath);

        listener.verifyReceived(notificationSchemaPath);

        listenerReg.close();

        listener.verifyReceived();

        subscriptionReg.close();

        legacyRouter.registerNotificationListener(testLegacyListener, notificationSchemaPath);

        listener.verifyNotReceived();
    }

    private static class TestLegacyDOMNotificationListener implements DOMNotificationListener {
        SettableFuture<DOMNotification> receivedNotification = SettableFuture.create();

        @Override
        public void onNotification(DOMNotification notification) {
            receivedNotification.set(notification);
        }

        void verifyReceived(SchemaPath path, ContainerNode body, @Nullable Date eventTime)
                throws InterruptedException, ExecutionException, TimeoutException {
            final DOMNotification actual = receivedNotification.get(5, TimeUnit.SECONDS);
            assertEquals(path, actual.getType());
            assertEquals(body, actual.getBody());

            if (eventTime != null) {
                assertTrue("Expected DOMEvent", actual instanceof DOMEvent);
                assertEquals(eventTime, ((DOMEvent)actual).getEventTime());
            } else {
                assertFalse("Unexpected DOMEvent", actual instanceof DOMEvent);
            }

            receivedNotification = SettableFuture.create();
        }

        void verifyNotReceived() {
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
            assertFalse("Unexpected notification", receivedNotification.isDone());
        }
    }

    private static class TestMdsalDOMNotificationListener
            implements org.opendaylight.mdsal.dom.api.DOMNotificationListener {
        SettableFuture<org.opendaylight.mdsal.dom.api.DOMNotification> receivedNotification = SettableFuture.create();

        @Override
        public void onNotification(org.opendaylight.mdsal.dom.api.DOMNotification notification) {
            receivedNotification.set(notification);
        }

        void verifyReceived(SchemaPath path, ContainerNode body, @Nullable Instant eventTime)
                throws InterruptedException, ExecutionException, TimeoutException {
            final org.opendaylight.mdsal.dom.api.DOMNotification actual =
                    receivedNotification.get(5, TimeUnit.SECONDS);
            assertEquals(path, actual.getType());
            assertEquals(body, actual.getBody());

            if (eventTime != null) {
                assertTrue("Expected DOMEvent", actual instanceof org.opendaylight.mdsal.dom.api.DOMEvent);
                assertEquals(eventTime, ((org.opendaylight.mdsal.dom.api.DOMEvent)actual).getEventInstant());
            } else {
                assertFalse("Unexpected DOMEvent", actual instanceof org.opendaylight.mdsal.dom.api.DOMEvent);
            }

            receivedNotification = SettableFuture.create();
        }
    }

    private static class TestLegacyDOMNotificationSubscriptionListener implements DOMNotificationSubscriptionListener {
        SettableFuture<Set<SchemaPath>> receivedNotification = SettableFuture.create();

        @Override
        public void onSubscriptionChanged(Set<SchemaPath> currentTypes) {
            receivedNotification.set(currentTypes);
        }

        void verifyReceived(SchemaPath... paths)
                throws InterruptedException, ExecutionException, TimeoutException {
            final Set<SchemaPath> actual = receivedNotification.get(5, TimeUnit.SECONDS);
            assertEquals(ImmutableSet.copyOf(paths), actual);
            receivedNotification = SettableFuture.create();
        }

        void verifyNotReceived() {
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
            assertFalse("Unexpected notification", receivedNotification.isDone());
        }
    }

    private static class TestLegacyDOMNotification implements DOMNotification {
        @Override
        public SchemaPath getType() {
            return notificationSchemaPath;
        }

        @Override
        public ContainerNode getBody() {
            return BODY;
        }
    }

    private static class TestLegacyDOMEvent extends TestLegacyDOMNotification implements DOMEvent {
        @Override
        public Date getEventTime() {
            return Date.from(INSTANT);
        }
    }

    private static class TestMdsalDOMNotification implements org.opendaylight.mdsal.dom.api.DOMNotification {
        @Override
        public SchemaPath getType() {
            return notificationSchemaPath;
        }

        @Override
        public ContainerNode getBody() {
            return BODY;
        }
    }

    private static class TestMdsalDOMEvent extends TestMdsalDOMNotification
            implements org.opendaylight.mdsal.dom.api.DOMEvent {
        @Override
        public Instant getEventInstant() {
            return INSTANT;
        }
    }
}
