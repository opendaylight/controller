/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;

/**
 * <p>
 * The EntityOwnershipService provides the means for a component/application to request ownership for a given
 * Entity on the current cluster member. Entity ownership is always tied to a process and two components on the same
 * process cannot register a candidate for a given Entity.
 * </p>
 * <p>
 * A component/application may also register interest in the ownership status of an Entity. The listener would be
 * notified whenever the ownership status changes.
 * </p>
 */
public interface EntityOwnershipService {

    /**
     * Registers a candidate for ownership of the given entity. Only one such request can be made per entity
     * per process. If multiple requests for registering a candidate for a given entity are received in the
     * current process a CandidateAlreadyRegisteredException will be thrown.
     * <p>
     * The registration is performed asynchronously and any registered {@link EntityOwnershipListener} is
     * notified of ownership status changes for the entity.
     *
     * @param entity the entity which the Candidate wants to own
     * @return a registration object that can be used to unregister the Candidate
     * @throws org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException
     */
    EntityOwnershipCandidateRegistration registerCandidate(@Nonnull Entity entity)
            throws CandidateAlreadyRegisteredException;

    /**
     * Registers a listener that is interested in ownership changes for entities of the given entity type. The
     * listener is notified whenever its process instance is granted ownership of the entity and also whenever
     * it loses ownership. On registration the listener will be notified of all entities its process instance
     * currently owns at the time of registration.
     *
     * @param entityType the type of entities whose ownership status the Listener is interested in
     * @param listener the listener that is interested in the entities
     * @return a registration object that can be used to unregister the Listener
     */
    EntityOwnershipListenerRegistration registerListener(@Nonnull String entityType, @Nonnull EntityOwnershipListener listener);

    /**
     * Gets the current ownership state information for an entity.
     *
     * @param forEntity the entity to query.
     * @return an Optional EntityOwnershipState whose instance is present if the entity is found
     */
    Optional<EntityOwnershipState> getOwnershipState(@Nonnull Entity forEntity);

    /**
     * Check if a local candidate is registered for the given entity
     *
     * @param entity
     * @return true if a candidate was registered locally, false otherwise
     */
    boolean isCandidateRegistered(@Nonnull Entity entity);
}
