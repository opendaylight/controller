/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.ServiceLoader;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.spi.ActorSystemInstanceCreator;
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
@NonNullByDefault
@MetaInfServices(ActorSystemInstance.Creator.class)
@Component(service = ActorSystemInstance.Creator.class)
public final class DefaultActorSystemInstanceCreator extends ActorSystemInstanceCreator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultActorSystemInstanceCreator.class);

    private final List<PekkoAccessibleClasses> accessibleClasses;
    private final ClassLoader classLoader;

    private DefaultActorSystemInstanceCreator(final ClassLoader classLoader,
            final List<PekkoAccessibleClasses> accessibleClasses) {
        // FIXME: construct from accessibleClasses only?
        this.classLoader = requireNonNull(classLoader);
        this.accessibleClasses = List.copyOf(accessibleClasses);
    }

    public DefaultActorSystemInstanceCreator() {
        this(DefaultActorSystemInstanceCreator.class.getClassLoader(),
            ServiceLoader.load(PekkoAccessibleClasses.class).stream().map(ServiceLoader.Provider::get).toList());
    }

    @Activate
    public DefaultActorSystemInstanceCreator(
            final @Reference(
                cardinality = ReferenceCardinality.AT_LEAST_ONE,
                policyOption = ReferencePolicyOption.GREEDY) List<PekkoAccessibleClasses> accessibleClasses) {
//        LOG.info("ActorSystemInstanceCreator-{} started", instanceNum);
        // FIXME: delegating class loader
        throw new UnsupportedOperationException();
    }

    @Deactivate
    void deactivate() {
        LOG.info("ActorSystemInstanceCreator {} stopped", uuid);
    }

    @Override
    protected ClassLoader classLoader() {
        return classLoader;
    }
}
