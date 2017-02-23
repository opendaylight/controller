/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlappingSingletonService implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(FlappingSingletonService.class);

    private static final ServiceGroupIdentifier SERVICE_GROUP_IDENTIFIER =
            ServiceGroupIdentifier.create("flapping-singleton-service");

    private final ClusterSingletonServiceProvider singletonServiceProvider;

    private volatile int flapCount = 0;
    private AtomicBoolean active = new AtomicBoolean(true);

    private volatile ClusterSingletonServiceRegistration registration;

    private static ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public FlappingSingletonService(final ClusterSingletonServiceProvider singletonServiceProvider) {
        LOG.debug("Registering flapping-singleton-service.");

        this.singletonServiceProvider = singletonServiceProvider;
        registration = singletonServiceProvider.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.debug("Instantiating flapping-singleton-service.");

        // we need to execute close from a different thread then the
        // singleton callback or we deadlock the singleton service.
        EXECUTOR.submit(() -> {
            try {
                registration.close();
                registration = null;
            } catch (final Exception e) {
                LOG.warn("There was a problem closing flapping singleton service.", e);
                flapCount = -flapCount;
            }
        });
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.debug("Closing flapping-singleton-service, flapCount: {}", flapCount);

        flapCount++;
        if (active.get()) {
            // seems like without delay this can deadlock as well
            EXECUTOR.submit(() -> {
                LOG.debug("Running registration");
                registration =
                        singletonServiceProvider.registerClusterSingletonService(this);

            });
        }

        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }

    public int setInactive() {
        LOG.debug("Setting flapping-singleton-service to inactive, flap-count: {}", flapCount);

        active.set(false);
        return flapCount;
    }
}
