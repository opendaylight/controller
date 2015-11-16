/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Encapsulated configuration for a shard.
 */
public class ShardConfig {
    private final String name;
    private final Set<String> replicas;

    public ShardConfig(@Nonnull final String name, @Nonnull final Collection<String> replicas) {
        this.name = Preconditions.checkNotNull(name);
        this.replicas = ImmutableSet.copyOf(Preconditions.checkNotNull(replicas));
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Set<String> getReplicas() {
        return replicas;
    }
}