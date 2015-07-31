/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

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
     * Registers as a Candidate that wants to own the given Entity. Only one such request can be made per process.
     * If multiple requests for registering a Candidate for a given Entity are received in the current process a
     * CandidateAlreadyRegisteredException will be thrown
     *
     * @param entity the entity which the Candidate wants to own
     * @param candidate the Candidate that wants to own the entity
     * @return a registration object that can be used to unregister the Candidate
     * @throws org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException
     */
    EntityOwnershipCandidateRegistration registerCandidate(Entity entity, EntityOwnershipCandidate candidate)
            throws CandidateAlreadyRegisteredException;

    /**
     * Registers a Listener that is interested in the ownership status of the given Entity. On registration the Listener
     * will be notified of the ownership status of the given Entity at the time of registration.
     *
     * @param entity the Entity whose ownership status the Listener is interested in
     * @param listener the Listener that is interested in the entity
     * @return a registration object that can be used to unregister the Listener
     */
    EntityOwnershipListenerRegistration registerListener(Entity entity, EntityOwnershipListener listener);

}
