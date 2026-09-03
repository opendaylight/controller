/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.Address;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.pekko.support.dagger.ActorSystemCreator;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callbacks that stop the framework and instruct Karaf to restart.
 */
@Component(service = OSGiCallbacks.class)
public final class OSGiCallbacks implements ActorSystemCreator.Callbacks {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiCallbacks.class);

    @NonNullByDefault
    private final ConfigurationReader reader;
    @NonNullByDefault
    private final PekkoAccessibleClassLoader classLoader;
    @NonNullByDefault
    private final ComponentFactory<OSGiActorSystemInstance> instanceFactory;

    private @Nullable ComponentInstance<OSGiActorSystemInstance> instance;

    @Activate
    @NonNullByDefault
    public OSGiCallbacks(
            @Reference final ConfigurationReader reader, @Reference final PekkoAccessibleClassLoader classLoader,
            @Reference(target = "(component.factory=" + OSGiActorSystemInstance.FACTORY_NAME + ")")
                final ComponentFactory<OSGiActorSystemInstance> instanceFactory) {
        this.reader = requireNonNull(reader);
        this.classLoader = requireNonNull(classLoader);
        this.instanceFactory = requireNonNull(instanceFactory);
        LOG.info("Actor System control starting");
        enableInstance();
        LOG.info("Actor System control started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Actor System control stopping");
        disableInstance();
        LOG.info("Actor System control stopped");
    }

    private void enableInstance() {
        if (instance == null) {
            instance = instanceFactory.newInstance(OSGiActorSystemInstance.props(reader, classLoader, this));
            LOG.info("Actor System instance enabled");
        } else {
            LOG.debug("Instance already enabled");
        }
    }

    private void disableInstance() {
        final var local = instance;
        if (local != null) {
            instance = null;
            local.dispose();
            LOG.info("Actor System instance disabled");
        } else {
            LOG.debug("Instance already disabled");
        }
    }

    @Override
    public void onLocalDown() {
        restart("local down");
    }

    @Override
    public void onRemoteQuarantined(final Address quarantinedBy) {
        restart("quarantined by " + quarantinedBy);
    }

    @NonNullByDefault
    private void restart(final String cause) {
        LOG.warn("Restarting Actor System instance due to {}", cause);
        disableInstance();
        enableInstance();
    }
}
