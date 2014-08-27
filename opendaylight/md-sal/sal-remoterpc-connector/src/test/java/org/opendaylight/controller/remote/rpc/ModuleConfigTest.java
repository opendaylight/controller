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
import org.junit.Assert;
import org.junit.Test;

public class ModuleConfigTest {

    @Test
    public void testModuleConfigDefaults(){
        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("unit-test").build();

        //metrics capture should be disabled
        Assert.assertFalse(config.isMetricCaptureEnabled());

        //rest of the configurations should be set
        Assert.assertNotNull(config.getActorSystemName());
        Assert.assertNotNull(config.getRpcBrokerName());
        Assert.assertNotNull(config.getRpcBrokerPath());
        Assert.assertNotNull(config.getRpcManagerName());
        Assert.assertNotNull(config.getRpcManagerPath());
        Assert.assertNotNull(config.getRpcRegistryName());
        Assert.assertNotNull(config.getRpcRegistryPath());
        Assert.assertNotNull(config.getAskDuration());
        Assert.assertNotNull(config.getAwaitDuration());
        Assert.assertNotNull(config.getGossipTickInterval());

    }

    @Test
    public void testModuleConfigWithMetricsCaptureEnabled(){
        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("unit-test").
                                  metricCaptureEnabled(true).
                                  build();

        Assert.assertTrue(config.isMetricCaptureEnabled());

        //Now check this config inside an actor
        ActorSystem system = ActorSystem.create("unit-test", config.get());
        TestActorRef<ConfigTestActor> configTestActorTestActorRef =
                TestActorRef.create(system, Props.create(ConfigTestActor.class));

        ConfigTestActor actor = configTestActorTestActorRef.underlyingActor();
        Config actorConfig = actor.getConfig();
        System.out.println(actorConfig.root().render());
        config = new RemoteRpcProviderConfig(actorConfig);

        Assert.assertTrue(config.isMetricCaptureEnabled());
    }

    @Test
    public void testModuleConfigWithBoundedMailboxCapacity(){
        int expectedTimeout = 500;
        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("unit-test").
                                  mailboxCapacity(expectedTimeout).
                                  build();

        Assert.assertNotNull(config.getMailBoxCapacity());
    }

    public static class ConfigTestActor extends UntypedActor {

        private Config actorSystemConfig;

        public ConfigTestActor(){
            this.actorSystemConfig = getContext().system().settings().config();
        }

        @Override
        public void onReceive(Object message) throws Exception {
        }

        /**
         * Only for testing. NEVER expose actor's internal state like this.
         * @return
         */
        public Config getConfig(){
            return actorSystemConfig;
        }
    }
}