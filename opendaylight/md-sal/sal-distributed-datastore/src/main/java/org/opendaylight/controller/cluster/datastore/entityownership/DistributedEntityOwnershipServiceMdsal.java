/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.api.clustering.Entity;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListener;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.adapter.clustering.BindingDOMEntityOwnershipServiceAdapter;
import org.opendaylight.mdsal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipService;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javassist.ClassPool;

/**
 * Blueprint activator for {@link EntityOwnershipService} OSGi service
 */
public class DistributedEntityOwnershipServiceMdsal implements EntityOwnershipService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDistributedEntityOwnershipServiceMdsal.class);

    private final BindingDOMEntityOwnershipServiceAdapter delegator;
    private ListenerRegistration<SchemaContextListener> schemaCtxListReg;

    private DistributedEntityOwnershipServiceMdsal(final DOMEntityOwnershipService domEos,
            final SchemaService schemaService) {
        final DataObjectSerializerGenerator streamWriter = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        // FIXME : BindingToNormalizedNodeCodec has to be a singleton instance in OSGi, so try to find a way to do it in MD-SAL project
        final BindingNormalizedNodeCodecRegistry registry = new BindingNormalizedNodeCodecRegistry(streamWriter);
        final BindingToNormalizedNodeCodec codec = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(), registry);
        schemaCtxListReg = schemaService.registerSchemaContextListener(codec);
        codec.onGlobalContextUpdated(schemaService.getGlobalContext());
        this.delegator = new BindingDOMEntityOwnershipServiceAdapter(domEos, codec);
        LOG.debug("BindingEntityOwnershipService was started...");
    }

    /**
     * Method is defined for bluepring OSGi service activator
     *
     * @param domEntityOwnershipService
     * @return {@link EntityOwnershipService} OSGi service
     */
    public static DistributedEntityOwnershipServiceMdsal start(
            final DOMEntityOwnershipService domEntityOwnershipService, final SchemaService schemaService) {
        LOG.debug("Start DistributedEntityOwnershipServiceMdsal");
        return new DistributedEntityOwnershipServiceMdsal(domEntityOwnershipService, schemaService);
    }

    @Override
    public EntityOwnershipCandidateRegistration registerCandidate(final Entity entity)
            throws CandidateAlreadyRegisteredException {
        LOG.debug("registerCandidate method called for Entity {}", entity);
        Preconditions.checkNotNull(entity, "entity cannot be null");

        return delegator.registerCandidate(entity);
    }

    @Override
    public EntityOwnershipListenerRegistration registerListener(final String entityType,
            final EntityOwnershipListener listener) {
        LOG.debug("registerListener method was called for Entity {}", entityType);
        Preconditions.checkNotNull(entityType, "entityType cannot be null");
        Preconditions.checkNotNull(listener, "listener cannot be null");

        return delegator.registerListener(entityType, listener);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final Entity forEntity) {
        LOG.debug("getOwnershipState method called for Entity {}", forEntity);
        Preconditions.checkNotNull(forEntity, "forEntity cannot be null");

        return delegator.getOwnershipState(forEntity);
    }

    @Override
    public boolean isCandidateRegistered(@Nonnull final Entity entity) {
        LOG.debug("isCandidateRegistered method called for Entity {}", entity);
        return delegator.isCandidateRegistered(entity);
    }

    @Override
    public void close() {
        LOG.debug("BindingEntityOwnershipService was closed ...");
        if (schemaCtxListReg != null) {
            schemaCtxListReg.close();
            schemaCtxListReg = null;
        }
    }
}
