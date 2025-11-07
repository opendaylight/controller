/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.dispatch.MessageDispatcher;
import org.eclipse.jdt.annotation.NonNull;
import scala.concurrent.ExecutionContextExecutor;

// FIXME: 13.0.0: remove this class
public class Dispatchers {
    public enum DispatcherType {
        Client("client-dispatcher"),
        Transaction("txn-dispatcher"),
        Shard("shard-dispatcher"),
        Notification("notification-dispatcher"),
        Serialization("serialization-dispatcher");

        public static final @NonNull String DEFAULT_DISPATCHER_PATH = "pekko.actor.default-dispatcher";

        private final @NonNull String path;

        DispatcherType(final @NonNull String path) {
            this.path = requireNonNull(path);
        }

        public @NonNull String path() {
            return path;
        }

        public String dispatcherPathIn(final org.apache.pekko.dispatch.Dispatchers knownDispatchers) {
            return knownDispatchers.hasDispatcher(path) ? path : DEFAULT_DISPATCHER_PATH;
        }

        public MessageDispatcher dispatcherIn(final org.apache.pekko.dispatch.Dispatchers knownDispatchers) {
            return knownDispatchers.hasDispatcher(path) ? knownDispatchers.lookup(path)
                : knownDispatchers.defaultGlobalDispatcher();
        }
    }

    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String DEFAULT_DISPATCHER_PATH = DispatcherType.DEFAULT_DISPATCHER_PATH;
    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String CLIENT_DISPATCHER_PATH = DispatcherType.Client.path();
    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String TXN_DISPATCHER_PATH = DispatcherType.Transaction.path();
    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String SHARD_DISPATCHER_PATH = DispatcherType.Shard.path();
    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String NOTIFICATION_DISPATCHER_PATH = DispatcherType.Notification.path();
    @Deprecated(since = "12.0.2", forRemoval = true)
    public static final @NonNull String SERIALIZATION_DISPATCHER_PATH = DispatcherType.Serialization.path();

    private final org.apache.pekko.dispatch.Dispatchers dispatchers;

    @Deprecated(since = "12.0.2", forRemoval = true)
    public Dispatchers(final org.apache.pekko.dispatch.Dispatchers dispatchers) {
        this.dispatchers = requireNonNull(dispatchers, "dispatchers should not be null");
    }

    @Deprecated(since = "12.0.2", forRemoval = true)
    public ExecutionContextExecutor getDispatcher(final DispatcherType dispatcherType) {
        return dispatcherType.dispatcherIn(dispatchers);
    }

    @Deprecated(since = "12.0.2", forRemoval = true)
    public String getDispatcherPath(final DispatcherType dispatcherType) {
        return dispatcherType.dispatcherPathIn(dispatchers);
    }
}
