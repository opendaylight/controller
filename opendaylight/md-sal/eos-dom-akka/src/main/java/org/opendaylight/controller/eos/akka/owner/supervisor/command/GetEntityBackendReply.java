/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;

public final class GetEntityBackendReply extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ImmutableSet<String> candidates;
    private final String owner;

    public GetEntityBackendReply(final @Nullable String owner, final @Nullable Set<String> candidates) {
        this.owner = owner;
        this.candidates = candidates == null ? ImmutableSet.of() : ImmutableSet.copyOf(candidates);
    }

    public ImmutableSet<String> getCandidates() {
        return candidates;
    }

    public String getOwner() {
        return owner;
    }
}
