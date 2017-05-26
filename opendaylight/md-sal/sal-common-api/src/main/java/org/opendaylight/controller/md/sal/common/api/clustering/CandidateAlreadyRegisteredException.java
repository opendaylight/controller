/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Thrown when a Candidate has already been registered for a given Entity. This could be due to a component doing a
 * duplicate registration or two different components within the same process trying to register a Candidate.
 */
public class CandidateAlreadyRegisteredException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Entity entity;

    public CandidateAlreadyRegisteredException(@Nonnull Entity entity) {
        super(String.format("Candidate has already been registered for %s",
                Preconditions.checkNotNull(entity, "entity should not be null")));
        this.entity = entity;
    }

    /**
     *
     * @return the entity for which a Candidate has already been registered in the current process
     */
    @Nonnull
    public Entity getEntity() {
        return entity;
    }
}
