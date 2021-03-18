/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.service;

import com.google.common.annotations.Beta;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
public class OSGINativeEntityOwnershipService implements DOMEntityOwnershipService {
    private static final Logger LOG = LoggerFactory.getLogger(OSGINativeEntityOwnershipService.class);

    @Reference(target = "(type=distributed-operational)")
    DistributedDataStoreInterface operDatastore = null;

    NativeEntityOwnershipService delegate;

    @Override
    public DOMEntityOwnershipCandidateRegistration registerCandidate(final DOMEntity entity)
            throws CandidateAlreadyRegisteredException {
        return delegate.registerCandidate(entity);
    }

    @Override
    public DOMEntityOwnershipListenerRegistration registerListener(final String entityType,
                                                                   final DOMEntityOwnershipListener listener) {
        return delegate.registerListener(entityType, listener);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity forEntity) {
        return delegate.getOwnershipState(forEntity);
    }

    @Override
    public boolean isCandidateRegistered(final DOMEntity forEntity) {
        return delegate.isCandidateRegistered(forEntity);
    }

    @Activate
    void activate(final Map<Object, Object> properties) throws ExecutionException, InterruptedException {
        LOG.info("Native Entity Ownership Service starting");
        delegate = NativeEntityOwnershipService.start(operDatastore.getActorUtils());
        LOG.info("Native Entity Ownership Service started");
    }

    @Deactivate
    void deactivate() throws Exception {
        LOG.info("Native Entity Ownership Service stopping");
        delegate.close();
        LOG.info("Native Entity Ownership Service stopped");
    }
}
