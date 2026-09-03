/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.typesafe.config.Config;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.ActorSystemInstance.Callbacks;
import org.opendaylight.controller.pekko.support.ActorSystemInstance.WithShutdown;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ActorSystemInstance.Creator}.
 */
@Component
@MetaInfServices
@NonNullByDefault
public final class ActorSystemInstanceCreator implements ActorSystemInstance.Creator {
    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemInstanceCreator.class);

    private final PekkoAccessibleClassLoader classLoader;

    private ActorSystemInstanceCreator(final Stream<PekkoAccessibleClasses> accessibleClasses) {
        classLoader = new PekkoAccessibleClassLoader(accessibleClasses);
    }

    public ActorSystemInstanceCreator() {
        this(ServiceLoader.load(PekkoAccessibleClasses.class).stream().map(ServiceLoader.Provider::get));
    }

    @Activate
    public ActorSystemInstanceCreator(
            final @Reference(
                cardinality = ReferenceCardinality.AT_LEAST_ONE,
                policyOption = ReferencePolicyOption.GREEDY) List<PekkoAccessibleClasses> accessibleClasses) {
        this(accessibleClasses.stream());
        LOG.info("ActorSystemInstanceCreator {} started", classLoader.getName());
    }

    @Deactivate
    void deactivate() {
        LOG.info("ActorSystemInstanceCreator {} stopped", classLoader.getName());
    }

    @Override
    public WithShutdown createInstance(final String name, final Config config, final Callbacks callbacks) {
        return new DefaultActorSystemInstance(name, config, callbacks, classLoader);
    }
}
