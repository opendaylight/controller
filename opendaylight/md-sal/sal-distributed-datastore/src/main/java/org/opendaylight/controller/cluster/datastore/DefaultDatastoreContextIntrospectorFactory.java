/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.spi.RaftPolicyResolver;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

@Singleton
@NonNullByDefault
public final class DefaultDatastoreContextIntrospectorFactory extends AbstractDatastoreContextIntrospectorFactory {
    private final BindingNormalizedNodeSerializer serializer;
    private final RaftPolicyResolver raftPolicyResolver;

    @Inject
    public DefaultDatastoreContextIntrospectorFactory(final RaftPolicyResolver raftPolicyResolver,
            final BindingNormalizedNodeSerializer serializer) {
        this.raftPolicyResolver = requireNonNull(raftPolicyResolver);
        this.serializer = requireNonNull(serializer);
    }

    @Override
    RaftPolicyResolver raftPolicyResolver() {
        return raftPolicyResolver;
    }

    @Override
    BindingNormalizedNodeSerializer serializer() {
        return serializer;
    }
}
