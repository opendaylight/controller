/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
public class ModuleConfig {

    private Config config;

    private static final String TAG_RPC_BROKER_NAME = "rpc-broker-name";
    private static final String TAG_RPC_REGISTRY_NAME = "registry-name";
    private static final String TAG_RPC_MGR_NAME = "rpc-manager-name";
    private static final String TAG_RPC_BROKER_PATH = "rpc-broker-path";
    private static final String TAG_RPC_REGISTRY_PATH = "rpc-registry-path";
    private static final String TAG_RPC_MGR_PATH = "rpc-manager-path";
    private static final String TAG_ACTOR_SYSTEM_NAME = "actor-system-name";
    private static final String TAG_MAILBOX = "mailbox";
    private static final String TAG_ASK_DURATION = "ask-duration";
    private static final String TAG_AWAIT_DURATION = "await-duration";
    private static final String TAG_GOSSIP_TICK_INTERVAL = "gossip-tick-interval";

    protected static final String TAG_METRIC_CAPTURE_ENABLED = "metric-capture-enabled";

    public ModuleConfig(Config config){
        this.config = config;
    }

    public Config get(){
        Config fallback = ConfigFactory.parseMap(asMap());
        return ConfigFactory.load().getConfig(getActorSystemName()).withFallback(fallback);
    }

    private Map<String, Object> asMap(){
        return config.root().unwrapped();
    }

    public static class Builder{
        private Map<String, String> configHolder;

        public Builder(String actorSystemName){
            configHolder = new HashMap<>();

            //actor system config
            configHolder.put(TAG_ACTOR_SYSTEM_NAME, actorSystemName);

            //Actor names
            configHolder.put(TAG_RPC_BROKER_NAME, "broker");
            configHolder.put(TAG_RPC_REGISTRY_NAME, "registry");
            configHolder.put(TAG_RPC_MGR_NAME, "rpc");

            //Actor paths
            configHolder.put(TAG_RPC_BROKER_PATH, "/user/rpc/broker");
            configHolder.put(TAG_RPC_REGISTRY_PATH, "/user/rpc/registry");
            configHolder.put(TAG_RPC_MGR_PATH, "/user/rpc");

            //actor config
            configHolder.put(TAG_MAILBOX, "bounded-mailbox");

            //durations
            configHolder.put(TAG_ASK_DURATION, "15s");
            configHolder.put(TAG_AWAIT_DURATION, "17s");
            configHolder.put(TAG_GOSSIP_TICK_INTERVAL, "500ms");

        }

        public Builder metricCaptureEnabled(boolean enabled){
            configHolder.put(TAG_METRIC_CAPTURE_ENABLED, String.valueOf(enabled));
            return this;
        }

        public ModuleConfig build(){
            return new ModuleConfig(ConfigFactory.parseMap(configHolder));
        }
    }

    public boolean isMetricCaptureEnabled() {
        if (config.hasPath(TAG_METRIC_CAPTURE_ENABLED))
            return config.getBoolean(TAG_METRIC_CAPTURE_ENABLED);

        else
            return false;
    }

    public String getRpcBrokerName(){
        return config.getString(TAG_RPC_BROKER_NAME);
    }

    public String getRpcRegistryName(){
        return config.getString(TAG_RPC_REGISTRY_NAME);
    }

    public String getRpcManagerName(){
        return config.getString(TAG_RPC_MGR_NAME);
    }

    public String getRpcBrokerPath(){
        return config.getString(TAG_RPC_BROKER_PATH);
    }

    public String getRpcRegistryPath(){
        return config.getString(TAG_RPC_REGISTRY_PATH);

    }

    public String getRpcManagerPath(){
        return config.getString(TAG_RPC_MGR_PATH);
    }

    public String getMailBoxName(){
        return config.getString(TAG_MAILBOX);
    }

    public String getActorSystemName(){
        return config.getString(TAG_ACTOR_SYSTEM_NAME);
    }

    public FiniteDuration getAskDuration(){
        return new FiniteDuration(config.getDuration(TAG_ASK_DURATION, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    public FiniteDuration getAwaitDuration(){
        return new FiniteDuration(config.getDuration(TAG_AWAIT_DURATION, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    public FiniteDuration getGossipTickInterval(){
        return new FiniteDuration(config.getDuration(TAG_GOSSIP_TICK_INTERVAL, TimeUnit.NANOSECONDS),
                                  TimeUnit.NANOSECONDS);
    }
}
