/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.Objects.requireNonNull;

import scala.concurrent.ExecutionContextExecutor;

public class Dispatchers {
    public static final String DEFAULT_DISPATCHER_PATH = "pekko.actor.default-dispatcher";
    public static final String CLIENT_DISPATCHER_PATH = "client-dispatcher";
    public static final String TXN_DISPATCHER_PATH = "txn-dispatcher";
    public static final String SHARD_DISPATCHER_PATH = "shard-dispatcher";
    public static final String NOTIFICATION_DISPATCHER_PATH = "notification-dispatcher";
    public static final String SERIALIZATION_DISPATCHER_PATH = "serialization-dispatcher";

    private final org.apache.pekko.dispatch.Dispatchers dispatchers;

    public enum DispatcherType {
        Client(CLIENT_DISPATCHER_PATH),
        Transaction(TXN_DISPATCHER_PATH),
        Shard(SHARD_DISPATCHER_PATH),
        Notification(NOTIFICATION_DISPATCHER_PATH),
        Serialization(SERIALIZATION_DISPATCHER_PATH);

        private final String path;

        DispatcherType(final String path) {
            this.path = path;
        }

        String path(final org.apache.pekko.dispatch.Dispatchers knownDispatchers) {
            if (knownDispatchers.hasDispatcher(path)) {
                return path;
            }
            return DEFAULT_DISPATCHER_PATH;
        }

        ExecutionContextExecutor dispatcher(final org.apache.pekko.dispatch.Dispatchers knownDispatchers) {
            if (knownDispatchers.hasDispatcher(path)) {
                return knownDispatchers.lookup(path);
            }
            return knownDispatchers.defaultGlobalDispatcher();
        }
    }

    public Dispatchers(final org.apache.pekko.dispatch.Dispatchers dispatchers) {
        this.dispatchers = requireNonNull(dispatchers, "dispatchers should not be null");
    }

    public ExecutionContextExecutor getDispatcher(final DispatcherType dispatcherType) {
        return dispatcherType.dispatcher(dispatchers);
    }

    public String getDispatcherPath(final DispatcherType dispatcherType) {
        return dispatcherType.path(dispatchers);
    }
}
