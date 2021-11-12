/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public final class GetEntitiesBackendReply extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ImmutableSetMultimap<DOMEntity, String> candidates;
    private final ImmutableMap<DOMEntity, String> owners;

    public GetEntitiesBackendReply(final Map<DOMEntity, String> owners, final Map<DOMEntity, Set<String>> candidates) {
        final ImmutableSetMultimap.Builder<DOMEntity, String> builder = ImmutableSetMultimap.builder();
        for (Map.Entry<DOMEntity, Set<String>> entry : candidates.entrySet()) {
            builder.putAll(entry.getKey(), entry.getValue());
        }
        this.candidates = builder.build();
        this.owners = ImmutableMap.copyOf(owners);
    }

    public ImmutableSetMultimap<DOMEntity, String>  getCandidates() {
        return candidates;
    }

    public ImmutableMap<DOMEntity, String> getOwners() {
        return owners;
    }
}
