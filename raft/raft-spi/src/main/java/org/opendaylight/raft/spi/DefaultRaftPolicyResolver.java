/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.kohsuke.MetaInfServices;

/**
 * Default {@link RaftPolicyResolver}, discovering {@link RaftPolicy} instances via {@link ServiceLoader}.
 */
@Singleton
@MetaInfServices(value = RaftPolicyResolver.class)
@NonNullByDefault
public final class DefaultRaftPolicyResolver extends AbstractRaftPolicyResolver {
    private final ServiceLoader<RaftPolicy> loader;

    /**
     * Default constructor.
     */
    @Inject
    public DefaultRaftPolicyResolver() {
        loader = ServiceLoader.load(RaftPolicy.class);
    }

    @Override
    protected Stream<RaftPolicy> streamPolicies() {
        return loader.stream().map(Provider::get);
    }
}
