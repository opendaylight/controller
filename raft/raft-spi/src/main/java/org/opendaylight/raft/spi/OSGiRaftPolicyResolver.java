/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * {@link RaftPolicyResolver} for OSGi.
 */
@Component(service = RaftPolicyResolver.class)
@NonNullByDefault
public final class OSGiRaftPolicyResolver extends AbstractRaftPolicyResolver {
    private final List<RaftPolicy> policies;

    @Activate
    public OSGiRaftPolicyResolver(
            @Reference(policyOption = ReferencePolicyOption.GREEDY) final List<RaftPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    @Override
    protected Stream<RaftPolicy> streamPolicies() {
        return policies.stream();
    }
}
