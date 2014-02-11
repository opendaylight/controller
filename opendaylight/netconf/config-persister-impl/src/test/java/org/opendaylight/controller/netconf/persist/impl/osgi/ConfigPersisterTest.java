/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusherConfiguration;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusherConfigurationBuilder;

public class ConfigPersisterTest {

    private MockedBundleContext ctx;
    private ConfigPersisterActivator configPersisterActivator;
    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private static final String NETCONF_ADDRESS = "localhost";
    private static final String NETCONF_PORT = "18383";
    private static NioEventLoopGroup eventLoopGroup;

    private void setUpContextAndStartPersister(Thread.UncaughtExceptionHandler exHandler, String requiredCapability, ConfigPusherConfiguration configuration)
            throws Exception {
        MockedBundleContext.DummyAdapterWithInitialSnapshot.expectedCapability = requiredCapability;
        ctx = new MockedBundleContext(NETCONF_ADDRESS, NETCONF_PORT);
        configPersisterActivator = new ConfigPersisterActivator(getThreadFactory(exHandler), mBeanServer,
                configuration);
        configPersisterActivator.start(ctx.getBundleContext());
    }

    @BeforeClass
    public static void setUp() throws Exception {
        eventLoopGroup = new NioEventLoopGroup();
    }

    @After
    public void tearDown() throws Exception {
        configPersisterActivator.stop(ctx.getBundleContext());
    }

    @AfterClass
    public static void closeNettyGroup() throws Exception {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void testPersisterNetconfNotStarting() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();

        setUpContextAndStartPersister(handler, "cap2", getConfiguration(100, 100).build());

        waitTestToFinish(2000);

        handler.assertException("connect to netconf endpoint", RuntimeException.class,
                "Could not connect to netconf server");
    }

    @Test
    public void testPersisterNotAllCapabilitiesProvided() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();
        ConfigPusherConfiguration cfg = getConfiguration(500, 1000)
                .withNetconfCapabilitiesWaitTimeoutMs(1000).build();

        setUpContextAndStartPersister(handler, "required-cap", cfg);

        try (MockNetconfEndpoint endpoint = startMockNetconfEndpoint("cap1")) {

            waitTestToFinish(2500);

            handler.assertException("retrieve required capabilities from netconf endpoint", RuntimeException.class,
                    "Expected but not found:[required-cap]");
        }
    }

    @Test
    public void testPersisterNoResponseFromNetconfAfterEdit() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();
        ConfigPusherConfiguration cfg = getConfigurationWIthOnePushAttempt();

        setUpContextAndStartPersister(handler, "cap1", cfg);

        try (MockNetconfEndpoint endpoint = startMockNetconfEndpoint("cap1")) {

            waitTestToFinish(3000);

            handler.assertException("receive response from netconf endpoint", IllegalStateException.class,
                    "Unable to load", IllegalStateException.class,
                    "Cannot read message from localhost/127.0.0.1:18383", 3);

            assertEquals(1 + 2, endpoint.getReceivedMessages().size());
            assertHelloMessage(endpoint.getReceivedMessages().get(1));
            assertEditMessage(endpoint.getReceivedMessages().get(2));
        }
    }

    private ConfigPusherConfiguration getConfigurationWIthOnePushAttempt() {
        return getConfiguration(500, 1000)
                    .withNetconfCapabilitiesWaitTimeoutMs(1000)
                    .withNetconfPushConfigAttempts(1)
                    .withNetconfPushConfigDelayMs(100)
                    .withNetconfSendMessageMaxAttempts(3)
                    .withNetconfSendMessageDelayMs(500).build();
    }

    @Test
    public void testPersisterSuccessfulPush() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();
        ConfigPusherConfiguration cfg = getConfigurationForSuccess();

        setUpContextAndStartPersister(handler, "cap1", cfg);

        try (MockNetconfEndpoint endpoint = startMockNetconfEndpoint("cap1", MockNetconfEndpoint.okMessage,
                MockNetconfEndpoint.okMessage)) {

            waitTestToFinish(4000);

            handler.assertException("register as JMX listener", RuntimeException.class,
                    "Cannot register as JMX listener to netconf");

            assertEquals(1 + 3, endpoint.getReceivedMessages().size());
            assertCommitMessage(endpoint.getReceivedMessages().get(3));
        }
    }

    private ConfigPusherConfiguration getConfigurationForSuccess() {
        return getConfiguration(500, 1000)
                    .withNetconfCapabilitiesWaitTimeoutMs(1000)
                    .withNetconfPushConfigAttempts(3)
                    .withNetconfPushConfigDelayMs(100)
                    .withNetconfSendMessageMaxAttempts(3)
                    .withNetconfSendMessageDelayMs(500).build();
    }

    @Test
    public void testPersisterConflictingVersionException() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();
        ConfigPusherConfiguration cfg = getConfigurationWIthOnePushAttempt();

        setUpContextAndStartPersister(handler, "cap1", cfg);

        try (MockNetconfEndpoint endpoint = startMockNetconfEndpoint("cap1", MockNetconfEndpoint.okMessage,
                MockNetconfEndpoint.conflictingVersionErrorMessage)) {

            Thread.sleep(4000);

            handler.assertException("register as JMX listener", IllegalStateException.class,
                    "Maximum attempt count has been reached for pushing", ConflictingVersionException.class, "Optimistic lock failed", 1);

            assertEquals(1 + 3, endpoint.getReceivedMessages().size());
            assertCommitMessage(endpoint.getReceivedMessages().get(3));
        }

        // TODO test also success on 2nd attempt after conflicting version Ex
    }

    @Test
    public void testPersisterSuccessfulPushAndSuccessfulJMXRegistration() throws Exception {
        final TestingExceptionHandler handler = new TestingExceptionHandler();
        ConfigPusherConfiguration cfg = getConfigurationForSuccess();

        setUpContextAndStartPersister(handler, "cap1", cfg);

        try (MockNetconfEndpoint endpoint = startMockNetconfEndpoint("cap1", MockNetconfEndpoint.okMessage,
                MockNetconfEndpoint.okMessage); DefaultCommitNotificationProducer jMXNotifier = startJMXCommitNotifier()) {

            Thread.sleep(2000);

            handler.assertNoException();

            assertEquals(1 + 3, endpoint.getReceivedMessages().size());
        }
    }

    private ConfigPusherConfigurationBuilder getConfiguration(int connectionAttemptDelayMs, int connectionAttemptTimeoutMs) {
        return ConfigPusherConfigurationBuilder.aConfigPusherConfiguration()
                .withEventLoopGroup(eventLoopGroup)
                .withConnectionAttemptDelayMs(connectionAttemptDelayMs)
                .withConnectionAttemptTimeoutMs(connectionAttemptTimeoutMs)
                .withNetconfCapabilitiesWaitTimeoutMs(44)
                .withNetconfAddress(new InetSocketAddress(NETCONF_ADDRESS, Integer.valueOf(NETCONF_PORT)));
    }

    private void waitTestToFinish(int i) throws InterruptedException {
        Thread.sleep(i);
    }


    private DefaultCommitNotificationProducer startJMXCommitNotifier() {
        return new DefaultCommitNotificationProducer(mBeanServer);
    }

    private void assertEditMessage(String netconfMessage) {
        assertThat(netconfMessage,
                JUnitMatchers.containsString(MockedBundleContext.DummyAdapterWithInitialSnapshot.CONFIG_SNAPSHOT));
    }

    private void assertCommitMessage(String netconfMessage) {
        assertThat(netconfMessage, JUnitMatchers.containsString("<commit"));
    }

    private void assertHelloMessage(String netconfMessage) {
        assertThat(netconfMessage,
                JUnitMatchers.containsString("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">"));
        assertThat(netconfMessage, JUnitMatchers.containsString("<capability>"));
    }

    private MockNetconfEndpoint startMockNetconfEndpoint(String capability, String... messages) {
        return new MockNetconfEndpoint(capability, NETCONF_PORT, messages);
    }

    public ThreadFactory getThreadFactory(final Thread.UncaughtExceptionHandler exHandler) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "config-persister-testing-activator");
                thread.setUncaughtExceptionHandler(exHandler);
                return thread;
            }
        };
    }
}
