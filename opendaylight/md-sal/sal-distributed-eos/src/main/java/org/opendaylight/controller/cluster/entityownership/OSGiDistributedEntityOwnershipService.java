/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import com.google.common.annotations.Beta;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfigReader;
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
@Component(immediate = true, configurationPid = "org.opendaylight.controller.cluster.entity.owner.selection.strategies",
    property = "type=default")
public final class OSGiDistributedEntityOwnershipService implements DOMEntityOwnershipService {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiDistributedEntityOwnershipService.class);

    @Reference(target = "(type=distributed-operational)")
    DistributedDataStoreInterface operDatastore = null;

    private DistributedEntityOwnershipService delegate;

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
    // FIXME: 3.0.0: properties are keyed by String, this should be Map<String, Object>
    void activate(final Map<Object, Object> properties) {
        LOG.info("Distributed Entity Ownership Service starting");
        delegate = DistributedEntityOwnershipService.start(operDatastore.getActorUtils(),
            EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(properties));
        LOG.info("Distributed Entity Ownership Service started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Distributed Entity Ownership Service stopping");
        delegate.close();
        LOG.info("Distributed Entity Ownership Service stopped");
    }
}
