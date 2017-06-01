/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.clustering.testutil;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import com.google.common.base.Optional;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;

/**
 * Fake EntityOwnershipService suitable for non-clustered component tests.
 *
 * @author Michael Vorburger
 */
public abstract class TestEntityOwnershipService implements EntityOwnershipService {

    private static final EntityOwnershipState STATE = new EntityOwnershipState(true, true);

    public static EntityOwnershipService newInstance() {
        return Mockito.mock(TestEntityOwnershipService.class, realOrException());
    }

    @Override
    public EntityOwnershipCandidateRegistration registerCandidate(Entity entity) {
        return Mockito.mock(EntityOwnershipCandidateRegistration.class, realOrException());
    }

    @Override
    public EntityOwnershipListenerRegistration registerListener(String entityType, EntityOwnershipListener listener) {
        return Mockito.mock(EntityOwnershipListenerRegistration.class, realOrException());
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(Entity forEntity) {
        return Optional.of(STATE);
    }

    @Override
    public boolean isCandidateRegistered(Entity entity) {
        return true;
    }
}
