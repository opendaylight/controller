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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final ScheduledExecutorService EXECUTOR = FinalizableScheduledExecutorService.newSingleThread();

    private final ClusterSingletonServiceProvider singletonServiceProvider;
    private final AtomicBoolean active = new AtomicBoolean(true);

    private final AtomicLong flapCount = new AtomicLong();
    private volatile ClusterSingletonServiceRegistration registration;

    public FlappingSingletonService(final ClusterSingletonServiceProvider singletonServiceProvider) {
        LOG.debug("Registering flapping-singleton-service.");

        this.singletonServiceProvider = singletonServiceProvider;
        registration = singletonServiceProvider.registerClusterSingletonService(this);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void instantiateServiceInstance() {
        LOG.debug("Instantiating flapping-singleton-service.");

        // TODO direct registration/close seem to trigger a bug in singleton state transitions,
        // remove the whole executor shenanigans after it's fixed.
        EXECUTOR.submit(() -> {
            try {
                registration.close();
                registration = null;
            } catch (Exception e) {
                LOG.warn("There was a problem closing flapping singleton service.", e);
                setInactive();

                final long count = flapCount.get();
                flapCount.compareAndSet(count, -count);
            }
        });
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.debug("Closing flapping-singleton-service, flapCount: {}", flapCount);

        flapCount.incrementAndGet();
        if (active.get()) {
            // TODO direct registration/close seem to trigger a bug in singleton state transitions,
            // remove  whole executor shenanigans after it's fixed.
            // Needs to be delayed slightly otherwise it's triggered as well.
            EXECUTOR.schedule(() -> {
                LOG.debug("Running re-registration");
                try {
                    registration = singletonServiceProvider.registerClusterSingletonService(this);
                } catch (RuntimeException e) {
                    LOG.warn("There was a problem re-registering flapping singleton service.", e);
                    setInactive();

                    final long count = flapCount.get();
                    flapCount.compareAndSet(count, -count - 1);
                }

            }, 200, TimeUnit.MILLISECONDS);
        }

        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }

    public long setInactive() {
        LOG.debug("Setting flapping-singleton-service to inactive, flap-count: {}", flapCount);

        active.set(false);
        return flapCount.get();
    }
}
