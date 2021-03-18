/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.candidate.command;

import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Sent to Candidate registry to register the candidate for a given entity.
 */
public class RegisterCandidate implements CandidateRegistryCommand {
    private final DOMEntity entity;
    private final String candidate;

    public RegisterCandidate(final DOMEntity entity, final String candidate) {
        this.entity = entity;
        this.candidate = candidate;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    public String getCandidate() {
        return candidate;
    }

    @Override
    public String toString() {
        return "RegisterCandidate{"
                + "entity=" + entity
                + ", candidate='" + candidate + '\''
                + '}';
    }
}
