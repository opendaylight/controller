/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import scala.concurrent.duration.FiniteDuration;

public class RemoteOpsProviderConfigTest {

    @Test
    public void testConfigDefaults() {
        RemoteOpsProviderConfig config = new RemoteOpsProviderConfig.Builder("unit-test").build();

        //Assert on configurations from common config
        assertFalse(config.isMetricCaptureEnabled()); //should be disabled by default
        assertNotNull(config.getMailBoxCapacity());
        assertNotNull(config.getMailBoxName());
        assertNotNull(config.getMailBoxPushTimeout());

        //rest of the configurations should be set
        assertNotNull(config.getActorSystemName());
        assertNotNull(config.getRpcBrokerName());
        assertNotNull(config.getRpcBrokerPath());
        assertNotNull(config.getRpcManagerName());
        assertNotNull(config.getRpcManagerPath());
        assertNotNull(config.getRpcRegistryName());
        assertNotNull(config.getActionRegistryName());
        assertNotNull(config.getRpcRegistryPath());
        assertNotNull(config.getActionRegistryPath());
        assertNotNull(config.getAskDuration());
        assertNotNull(config.getGossipTickInterval());
    }

    @Test
    public void testConfigCustomizations() {

        AkkaConfigurationReader reader = new TestConfigReader();

        final int expectedCapacity = 100;
        final var expectedTimeout = new FiniteDuration(10, TimeUnit.MILLISECONDS);

        RemoteOpsProviderConfig config = new RemoteOpsProviderConfig.Builder("unit-test")
                .metricCaptureEnabled(true)//enable metric capture
                .mailboxCapacity(expectedCapacity)
                .mailboxPushTimeout("10ms")
                .withConfigReader(reader)
                .build();

        assertTrue(config.isMetricCaptureEnabled());
        assertEquals(expectedCapacity, config.getMailBoxCapacity().intValue());
        assertEquals(expectedTimeout, config.getMailBoxPushTimeout());

        //Now check this config inside an actor
        ActorSystem system = ActorSystem.create("unit-test", config.get());
        TestActorRef<ConfigTestActor> configTestActorTestActorRef =
                TestActorRef.create(system, Props.create(ConfigTestActor.class));

        ConfigTestActor actor = configTestActorTestActorRef.underlyingActor();
        Config actorConfig = actor.getConfig();

        config = new RemoteOpsProviderConfig(actorConfig);

        assertTrue(config.isMetricCaptureEnabled());
        assertEquals(expectedCapacity, config.getMailBoxCapacity().intValue());
        assertEquals(expectedTimeout, config.getMailBoxPushTimeout());
    }

    public static class ConfigTestActor extends UntypedAbstractActor {

        private final Config actorSystemConfig;

        public ConfigTestActor() {
            actorSystemConfig = getContext().system().settings().config();
        }

        @Override
        public void onReceive(final Object message) {
        }

        /**
         * Only for testing. NEVER expose actor's internal state like this.
         */
        public Config getConfig() {
            return actorSystemConfig;
        }
    }

    public static class TestConfigReader implements AkkaConfigurationReader {

        @Override
        public Config read() {
            return ConfigFactory.parseResources("application.conf");

        }
    }
}
