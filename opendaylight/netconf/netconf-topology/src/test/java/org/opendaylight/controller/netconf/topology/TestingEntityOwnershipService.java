/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;

public class TestingEntityOwnershipService implements EntityOwnershipService {

    private final List<EntityOwnershipListener> listeners = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private Entity entity;
    private boolean masterSet = false;

    @Override
    public EntityOwnershipCandidateRegistration registerCandidate(final Entity entity)
            throws CandidateAlreadyRegisteredException {
        this.entity = entity;
        return new EntityOwnershipCandidateRegistration() {
            @Override
            public void close() {

            }

            @Override
            public Entity getInstance() {
                return entity;
            }
        };
    }

    @Override
    public EntityOwnershipListenerRegistration registerListener(final String entityType,
            final EntityOwnershipListener listener) {
        return new EntityOwnershipListenerRegistration() {
            @Nonnull
            @Override
            public String getEntityType() {
                return entityType;
            }

            @Override
            public void close() {
                listeners.remove(listener);
            }

            @Override
            public EntityOwnershipListener getInstance() {
                return listener;
            }
        };
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final Entity forEntity) {
        return null;
    }

    public void startService() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (final EntityOwnershipListener listener : listeners) {
                    if (!masterSet) {
                        listener.ownershipChanged(new EntityOwnershipChange(entity, false, true, true));
                        masterSet = true;
                    } else {
                        listener.ownershipChanged(new EntityOwnershipChange(entity, false, false, true));
                    }
                }

            }
        });
    }
}
