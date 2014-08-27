/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.TestActorRef;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class RemoteRpcProviderConfigTest {

    @Test
    public void testConfigDefaults() {

        Config c = ConfigFactory.parseFile(new File("application.conf"));
        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("unit-test").build();

        //Assert on configurations from common config
        Assert.assertFalse(config.isMetricCaptureEnabled()); //should be disabled by default
        Assert.assertNotNull(config.getMailBoxCapacity());
        Assert.assertNotNull(config.getMailBoxName());
        Assert.assertNotNull(config.getMailBoxPushTimeout());

        //rest of the configurations should be set
        Assert.assertNotNull(config.getActorSystemName());
        Assert.assertNotNull(config.getRpcBrokerName());
        Assert.assertNotNull(config.getRpcBrokerPath());
        Assert.assertNotNull(config.getRpcManagerName());
        Assert.assertNotNull(config.getRpcManagerPath());
        Assert.assertNotNull(config.getRpcRegistryName());
        Assert.assertNotNull(config.getRpcRegistryPath());
        Assert.assertNotNull(config.getAskDuration());
        Assert.assertNotNull(config.getGossipTickInterval());



    }

    @Test
    public void testConfigCustomizations() {

        AkkaConfigurationReader reader = new TestConfigReader();

        final int expectedCapacity = 100;
        String timeOutVal = "10ms";
        FiniteDuration expectedTimeout = FiniteDuration.create(10, TimeUnit.MILLISECONDS);

        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("unit-test")
                .metricCaptureEnabled(true)//enable metric capture
                .mailboxCapacity(expectedCapacity)
                .mailboxPushTimeout(timeOutVal)
                .withConfigReader(reader)
                .build();

        Assert.assertTrue(config.isMetricCaptureEnabled());
        Assert.assertEquals(expectedCapacity, config.getMailBoxCapacity().intValue());
        Assert.assertEquals(expectedTimeout.toMillis(), config.getMailBoxPushTimeout().toMillis());

        //Now check this config inside an actor
        ActorSystem system = ActorSystem.create("unit-test", config.get());
        TestActorRef<ConfigTestActor> configTestActorTestActorRef =
                TestActorRef.create(system, Props.create(ConfigTestActor.class));

        ConfigTestActor actor = configTestActorTestActorRef.underlyingActor();
        Config actorConfig = actor.getConfig();

        config = new RemoteRpcProviderConfig(actorConfig);

        Assert.assertTrue(config.isMetricCaptureEnabled());
        Assert.assertEquals(expectedCapacity, config.getMailBoxCapacity().intValue());
        Assert.assertEquals(expectedTimeout.toMillis(), config.getMailBoxPushTimeout().toMillis());
    }

    public static class ConfigTestActor extends UntypedActor {

        private Config actorSystemConfig;

        public ConfigTestActor() {
            this.actorSystemConfig = getContext().system().settings().config();
        }

        @Override
        public void onReceive(Object message) throws Exception {
        }

        /**
         * Only for testing. NEVER expose actor's internal state like this.
         *
         * @return
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