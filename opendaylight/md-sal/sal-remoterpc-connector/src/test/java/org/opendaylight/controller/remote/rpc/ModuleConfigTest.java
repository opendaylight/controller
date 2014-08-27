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

public class ModuleConfigTest {

    @Test
    public void testModuleConfigDefaults(){
        ModuleConfig config = new ModuleConfig.Builder().build();

        //metrics capture should be disabled
        Assert.assertFalse(config.isMetricCaptureEnabled());

        //rest of the configurations should be set
        Assert.assertNotNull(config.getActorSystemName());
        Assert.assertNotNull(config.getMailBoxName());
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
        ModuleConfig config = new ModuleConfig.Builder().
                                  metricCaptureEnabled(true).
                                  build();

        Assert.assertTrue(config.isMetricCaptureEnabled());

        ActorSystem system = ActorSystem.create("unit-test", getMergedConfig(config));
        TestActorRef<ConfigTestActor> configTestActorTestActorRef =
                TestActorRef.create(system, Props.create(ConfigTestActor.class));

        ConfigTestActor actor = configTestActorTestActorRef.underlyingActor();
        Config actorConfig = actor.getConfig();

        Assert.assertTrue(actorConfig.hasPath(ModuleConfig.TAG_METRIC_CAPTURE_ENABLED));
        Assert.assertTrue(actorConfig.getBoolean(ModuleConfig.TAG_METRIC_CAPTURE_ENABLED));
    }

    private Config getMergedConfig(ModuleConfig config){
        Config fallback = ConfigFactory.parseMap(config.asMap());
        return ConfigFactory.load().getConfig("unit-test").withFallback(fallback);
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