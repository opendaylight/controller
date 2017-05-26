/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.clustering.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Adapter that bridges between the legacy pre-Boron EntityOwnershipService and DOMEntityOwnershipService interfaces.
 *
 * @author Thomas Pantelis
 */
public class LegacyEntityOwnershipServiceAdapter implements EntityOwnershipService, AutoCloseable {
    private final DOMEntityOwnershipService domService;

    public LegacyEntityOwnershipServiceAdapter(@Nonnull DOMEntityOwnershipService domService) {
        this.domService = Preconditions.checkNotNull(domService);
    }

    @Override
    public EntityOwnershipCandidateRegistration registerCandidate(Entity entity)
            throws CandidateAlreadyRegisteredException {
        try {
            return new EntityOwnershipCandidateRegistrationAdapter(domService.registerCandidate(
                    toDOMEntity(entity)), entity);
        } catch(org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException e) {
            throw new CandidateAlreadyRegisteredException(entity);
        }
    }

    @Override
    public EntityOwnershipListenerRegistration registerListener(String entityType, EntityOwnershipListener listener) {
        return new EntityOwnershipListenerRegistrationAdapter(entityType, listener,
                domService.registerListener(entityType, new DOMEntityOwnershipListenerAdapter(listener)));
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(Entity forEntity) {
        return toEntityOwnershipState(domService.getOwnershipState(toDOMEntity(forEntity)));
    }

    @Override
    public boolean isCandidateRegistered(Entity entity) {
        return domService.isCandidateRegistered(toDOMEntity(entity));
    }

    @Override
    public void close() {
    }

    private DOMEntity toDOMEntity(Entity from) {
        return new DOMEntity(from.getType(), from.getId());
    }

    private Optional<EntityOwnershipState> toEntityOwnershipState(
            Optional<org.opendaylight.mdsal.eos.common.api.EntityOwnershipState> from) {
        if(!from.isPresent()) {
            return Optional.absent();
        }

        org.opendaylight.mdsal.eos.common.api.EntityOwnershipState fromState = from.get();
        return Optional.of(new EntityOwnershipState(
                fromState == org.opendaylight.mdsal.eos.common.api.EntityOwnershipState.IS_OWNER,
                fromState != org.opendaylight.mdsal.eos.common.api.EntityOwnershipState.NO_OWNER));
    }

    private static class EntityOwnershipCandidateRegistrationAdapter extends AbstractObjectRegistration<Entity>
            implements EntityOwnershipCandidateRegistration {
        private final DOMEntityOwnershipCandidateRegistration domRegistration;

        EntityOwnershipCandidateRegistrationAdapter(DOMEntityOwnershipCandidateRegistration domRegistration,
                Entity entity) {
            super(entity);
            this.domRegistration = Preconditions.checkNotNull(domRegistration);
        }

        @Override
        protected void removeRegistration() {
            domRegistration.close();
        }
    }

    private static class EntityOwnershipListenerRegistrationAdapter extends AbstractObjectRegistration<EntityOwnershipListener>
            implements EntityOwnershipListenerRegistration {
        private final String entityType;
        private final DOMEntityOwnershipListenerRegistration domRegistration;

        EntityOwnershipListenerRegistrationAdapter(String entityType, EntityOwnershipListener listener,
                DOMEntityOwnershipListenerRegistration domRegistration) {
            super(listener);
            this.entityType = Preconditions.checkNotNull(entityType);
            this.domRegistration = Preconditions.checkNotNull(domRegistration);
        }

        @Override
        public String getEntityType() {
            return entityType;
        }

        @Override
        protected void removeRegistration() {
            domRegistration.close();
        }
    }

    private static class DOMEntityOwnershipListenerAdapter implements DOMEntityOwnershipListener {
        private final EntityOwnershipListener delegateListener;

        DOMEntityOwnershipListenerAdapter(EntityOwnershipListener delegateListener) {
            this.delegateListener = Preconditions.checkNotNull(delegateListener);
        }

        @Override
        public void ownershipChanged(DOMEntityOwnershipChange ownershipChange) {
            Entity entity = new Entity(ownershipChange.getEntity().getType(), ownershipChange.getEntity().
                    getIdentifier());
            delegateListener.ownershipChanged(new EntityOwnershipChange(entity,
                    ownershipChange.getState().wasOwner(), ownershipChange.getState().isOwner(),
                    ownershipChange.getState().hasOwner(), ownershipChange.inJeopardy()));
        }
    }
}
