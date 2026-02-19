/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * Encapsulated configuration for a shard.
 */
public class ShardConfig {
    private final String name;
    private final Set<MemberName> replicas;

    public ShardConfig(final @NonNull String name, final @NonNull Collection<MemberName> replicas) {
        this.name = requireNonNull(name);
        this.replicas = ImmutableSet.copyOf(replicas);
    }

    public @NonNull String getName() {
        return name;
    }

    public @NonNull Set<MemberName> getReplicas() {
        return replicas;
    }
}
