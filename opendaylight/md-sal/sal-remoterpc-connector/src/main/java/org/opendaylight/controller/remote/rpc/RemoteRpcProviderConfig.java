/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import com.typesafe.config.Config;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.remote.rpc.utils.AkkaConfigurationReader;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 */
public class RemoteRpcProviderConfig extends CommonConfig {

    protected static final String TAG_RPC_BROKER_NAME = "rpc-broker-name";
    protected static final String TAG_RPC_REGISTRY_NAME = "registry-name";
    protected static final String TAG_RPC_MGR_NAME = "rpc-manager-name";
    protected static final String TAG_RPC_BROKER_PATH = "rpc-broker-path";
    protected static final String TAG_RPC_REGISTRY_PATH = "rpc-registry-path";
    protected static final String TAG_RPC_MGR_PATH = "rpc-manager-path";
    protected static final String TAG_ASK_DURATION = "ask-duration";
    protected static final String TAG_AWAIT_DURATION = "await-duration";
    private static final String TAG_GOSSIP_TICK_INTERVAL = "gossip-tick-interval";

    public RemoteRpcProviderConfig(Config config){
        super(config);
    }

    public String getRpcBrokerName(){
        return get().getString(TAG_RPC_BROKER_NAME);
    }

    public String getRpcRegistryName(){
        return get().getString(TAG_RPC_REGISTRY_NAME);
    }

    public String getRpcManagerName(){
        return get().getString(TAG_RPC_MGR_NAME);
    }

    public String getRpcBrokerPath(){
        return get().getString(TAG_RPC_BROKER_PATH);
    }

    public String getRpcRegistryPath(){
        return get().getString(TAG_RPC_REGISTRY_PATH);

    }

    public String getRpcManagerPath(){
        return get().getString(TAG_RPC_MGR_PATH);
    }


    public FiniteDuration getAskDuration(){
        return new FiniteDuration(get().getDuration(TAG_ASK_DURATION, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    public FiniteDuration getAwaitDuration(){
        return new FiniteDuration(get().getDuration(TAG_AWAIT_DURATION, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    public FiniteDuration getGossipTickInterval(){
        return new FiniteDuration(get().getDuration(TAG_GOSSIP_TICK_INTERVAL, TimeUnit.NANOSECONDS),
                                  TimeUnit.NANOSECONDS);
    }

    public static class Builder extends CommonConfig.Builder<Builder>{

        private AkkaConfigurationReader reader = null;

        public Builder(String actorSystemName){
            super(actorSystemName);

            //Actor names
            configHolder.put(TAG_RPC_BROKER_NAME, "broker");
            configHolder.put(TAG_RPC_REGISTRY_NAME, "registry");
            configHolder.put(TAG_RPC_MGR_NAME, "rpc");

            //Actor paths
            configHolder.put(TAG_RPC_BROKER_PATH, "/user/rpc/broker");
            configHolder.put(TAG_RPC_REGISTRY_PATH, "/user/rpc/registry");
            configHolder.put(TAG_RPC_MGR_PATH, "/user/rpc");

            //durations
            configHolder.put(TAG_ASK_DURATION, "15s");
            configHolder.put(TAG_AWAIT_DURATION, "17s");
            configHolder.put(TAG_GOSSIP_TICK_INTERVAL, "500ms");

        }

        public Builder withConfigReader(AkkaConfigurationReader reader){
            this.reader = reader;
            return this;
        }

        public RemoteRpcProviderConfig build(){
            Config config;
            if (reader == null)
                config = merge(configHolder, (String) configHolder.get(TAG_ACTOR_SYSTEM_NAME));
            else
                config = merge(configHolder, reader.read().getConfig((String) configHolder.get(TAG_ACTOR_SYSTEM_NAME)));

            return new RemoteRpcProviderConfig(config);
        }
    }


}
