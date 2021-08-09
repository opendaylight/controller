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
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.NodeName;

public final class GetEntityReply extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ImmutableSet<String> candidates;
    private final String owner;

    public GetEntityReply(final @Nullable String owner, final @Nullable Set<String> candidates) {
        this.owner = owner;
        this.candidates = candidates == null ? ImmutableSet.of() : ImmutableSet.copyOf(candidates);
    }

    public @NonNull GetEntityOutput toOutput() {
        final GetEntityOutputBuilder builder = new GetEntityOutputBuilder();
        if (owner != null) {
            builder.setOwnerNode(new NodeName(owner));
        }
        return builder
            .setCandidateNodes(candidates.stream().map(NodeName::new).collect(Collectors.toUnmodifiableList()))
            .build();
    }
}
