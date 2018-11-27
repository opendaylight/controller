/*
 * Copyright (c) 2018 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

/**
 * Local ShardManager message to register a callback to be notified of shard availability changes. The reply to
 * this message is a {@link org.opendaylight.yangtools.concepts.Registration} instance wrapped in a
 * {@link akka.actor.Status.Success}.
 *
 * @author Thomas Pantelis
 */
public class RegisterForShardAvailabilityChanges {
    private final Consumer<String> callback;

    public RegisterForShardAvailabilityChanges(Consumer<String> callback) {
        this.callback = requireNonNull(callback);
    }

    public Consumer<String> getCallback() {
        return callback;
    }
}
