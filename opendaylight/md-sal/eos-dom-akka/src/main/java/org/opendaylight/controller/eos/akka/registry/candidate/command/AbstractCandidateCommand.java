/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.candidate.command;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public abstract class AbstractCandidateCommand extends CandidateRegistryCommand {
    private final @NonNull DOMEntity entity;
    private final @NonNull String candidate;

    AbstractCandidateCommand(final DOMEntity entity, final String candidate) {
        this.entity = requireNonNull(entity);
        this.candidate = requireNonNull(candidate);
    }

    public final @NonNull DOMEntity getEntity() {
        return entity;
    }

    public final @NonNull String getCandidate() {
        return candidate;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("entity", entity).add("candidate", candidate).toString();
    }
}
