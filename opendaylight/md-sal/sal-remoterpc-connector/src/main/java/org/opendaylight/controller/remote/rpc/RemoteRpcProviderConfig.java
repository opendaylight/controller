/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.util.Timeout;
import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import scala.concurrent.duration.FiniteDuration;

public class RemoteRpcProviderConfig extends CommonConfig {

    protected static final String TAG_RPC_BROKER_NAME = "rpc-broker-name";
    protected static final String TAG_RPC_REGISTRAR_NAME = "rpc-registrar-name";
    protected static final String TAG_RPC_REGISTRY_NAME = "registry-name";
    protected static final String TAG_RPC_MGR_NAME = "rpc-manager-name";
    protected static final String TAG_RPC_BROKER_PATH = "rpc-broker-path";
    protected static final String TAG_RPC_REGISTRY_PATH = "rpc-registry-path";
    protected static final String TAG_RPC_MGR_PATH = "rpc-manager-path";
    protected static final String TAG_ASK_DURATION = "ask-duration";

    private static final String TAG_GOSSIP_TICK_INTERVAL = "gossip-tick-interval";
    private static final String TAG_RPC_REGISTRY_PERSISTENCE_ID = "rpc-registry-persistence-id";

    //locally cached values
    private Timeout cachedAskDuration;
    private FiniteDuration cachedGossipTickInterval;

    public RemoteRpcProviderConfig(final Config config) {
        super(config);
    }

    public String getRpcBrokerName() {
        return get().getString(TAG_RPC_BROKER_NAME);
    }

    public String getRpcRegistrarName() {
        return get().getString(TAG_RPC_REGISTRAR_NAME);
    }

    public String getRpcRegistryName() {
        return get().getString(TAG_RPC_REGISTRY_NAME);
    }

    public String getRpcManagerName() {
        return get().getString(TAG_RPC_MGR_NAME);
    }

    public String getRpcBrokerPath() {
        return get().getString(TAG_RPC_BROKER_PATH);
    }

    public String getRpcRegistryPath() {
        return get().getString(TAG_RPC_REGISTRY_PATH);
    }

    public String getRpcRegistryPersistenceId() {
        return get().getString(TAG_RPC_REGISTRY_PERSISTENCE_ID);
    }

    public String getRpcManagerPath() {
        return get().getString(TAG_RPC_MGR_PATH);
    }

    public Timeout getAskDuration() {
        if (cachedAskDuration != null) {
            return cachedAskDuration;
        }

        cachedAskDuration = new Timeout(new FiniteDuration(
                get().getDuration(TAG_ASK_DURATION, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS));

        return cachedAskDuration;
    }

    public FiniteDuration getGossipTickInterval() {
        if (cachedGossipTickInterval != null) {
            return cachedGossipTickInterval;
        }

        cachedGossipTickInterval = new FiniteDuration(
                get().getDuration(TAG_GOSSIP_TICK_INTERVAL, TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);

        return cachedGossipTickInterval;
    }

    /**
     * This is called via blueprint xml as the builder pattern can't be used.
     */
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
            justification = "Findbugs flags this as an unconfirmed cast of return value but the build method clearly "
                + "returns RemoteRpcProviderConfig. Perhaps it's confused b/c the build method is overloaded and "
                + "and differs in return type from the base class.")
    public static RemoteRpcProviderConfig newInstance(final String actorSystemName, final boolean metricCaptureEnabled,
            final int mailboxCapacity) {
        return new Builder(actorSystemName).metricCaptureEnabled(metricCaptureEnabled)
                .mailboxCapacity(mailboxCapacity).build();
    }

    public static class Builder extends CommonConfig.Builder<Builder> {

        public Builder(final String actorSystemName) {
            super(actorSystemName);

            //Actor names
            configHolder.put(TAG_RPC_BROKER_NAME, "broker");
            configHolder.put(TAG_RPC_REGISTRAR_NAME, "registrar");
            configHolder.put(TAG_RPC_REGISTRY_NAME, "registry");
            configHolder.put(TAG_RPC_MGR_NAME, "rpc");

            //Actor paths
            configHolder.put(TAG_RPC_BROKER_PATH, "/user/rpc/broker");
            configHolder.put(TAG_RPC_REGISTRY_PATH, "/user/rpc/registry");
            configHolder.put(TAG_RPC_MGR_PATH, "/user/rpc");

            //durations
            configHolder.put(TAG_ASK_DURATION, "15s");
            configHolder.put(TAG_GOSSIP_TICK_INTERVAL, "500ms");

            // persistence
            configHolder.put(TAG_RPC_REGISTRY_PERSISTENCE_ID, "remote-rpc-registry");
        }

        public Builder gossipTickInterval(final String interval) {
            configHolder.put(TAG_GOSSIP_TICK_INTERVAL, interval);
            return this;
        }

        @Override
        public RemoteRpcProviderConfig build() {
            return new RemoteRpcProviderConfig(merge());
        }
    }
}
