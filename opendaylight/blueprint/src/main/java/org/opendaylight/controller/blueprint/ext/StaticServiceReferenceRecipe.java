/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.function.Consumer;
import org.apache.aries.blueprint.container.AbstractServiceReferenceRecipe;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Blueprint bean recipe for a static OSGi service reference, meaning it obtains the service instance once
 * and doesn't react to service removal. In addition the returned object is the actual service instance and
 * not a proxy.
 *
 * @author Thomas Pantelis
 */
class StaticServiceReferenceRecipe extends AbstractServiceReferenceRecipe {
    private static final Logger LOG = LoggerFactory.getLogger(StaticServiceReferenceRecipe.class);

    private static final SatisfactionListener NOOP_LISTENER = satisfiable -> {

    };

    private volatile ServiceReference<?> trackedServiceReference;
    private volatile Object trackedService;
    private Consumer<Object> serviceSatisfiedCallback;

    StaticServiceReferenceRecipe(final String name, final ExtendedBlueprintContainer blueprintContainer,
            final String interfaceClass) {
        super(name, blueprintContainer, new MandatoryServiceReferenceMetadata(name, interfaceClass), null, null,
                Collections.emptyList());
    }

    void startTracking(final Consumer<Object> newServiceSatisfiedCallback) {
        this.serviceSatisfiedCallback = newServiceSatisfiedCallback;
        super.start(NOOP_LISTENER);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void track(final ServiceReference reference) {
        retrack();
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void untrack(final ServiceReference reference) {
        LOG.debug("{}: In untrack {}", getName(), reference);

        if (trackedServiceReference == reference) {
            LOG.debug("{}: Current reference has been untracked", getName(), trackedServiceReference);
        }
    }

    @Override
    protected void retrack() {
        LOG.debug("{}: In retrack", getName());

        if (trackedServiceReference == null) {
            trackedServiceReference = getBestServiceReference();

            LOG.debug("{}: getBestServiceReference: {}", getName(), trackedServiceReference);

            if (trackedServiceReference != null && serviceSatisfiedCallback != null) {
                serviceSatisfiedCallback.accept(internalCreate());
            }
        }
    }

    @Override
    protected void doStop() {
        LOG.debug("{}: In doStop", getName());

        if (trackedServiceReference != null && trackedService != null) {
            try {
                getBundleContextForServiceLookup().ungetService(trackedServiceReference);
            } catch (final IllegalStateException e) {
                // In case the service no longer exists, ignore.
            }

            trackedServiceReference = null;
            trackedService = null;
        }
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        ServiceReference<?> localTrackedServiceReference = trackedServiceReference;

        LOG.debug("{}: In internalCreate: trackedServiceReference: {}", getName(), localTrackedServiceReference);

        // being paranoid - internalCreate should only get called once
        if (trackedService != null) {
            return trackedService;
        }

        Preconditions.checkNotNull(localTrackedServiceReference, "trackedServiceReference is null");

        trackedService = getServiceSecurely(localTrackedServiceReference);

        LOG.debug("{}: Returning service instance: {}", getName(), trackedService);

        Preconditions.checkNotNull(trackedService, "getService() returned null for %s", localTrackedServiceReference);

        return trackedService;
    }
}
