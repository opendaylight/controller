/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;
import java.util.ServiceLoader;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.ActorSystemInstance.WithShutdown;
import org.opendaylight.controller.pekko.support.dagger.ActorSystemCreator;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;

/**
 * Default implementation of {@link ActorSystemCreator}.
 */
@MetaInfServices
@NonNullByDefault
public final class DefaultActorSystemCreator implements ActorSystemCreator {
    private final PekkoAccessibleClassLoader classLoader;

    public DefaultActorSystemCreator() {
        classLoader = PekkoAccessibleClassLoader.of(
            ServiceLoader.load(PekkoAccessibleClasses.class).stream().map(ServiceLoader.Provider::get));
    }

    @Override
    public WithShutdown createInstance(final String name, final Config config, final Callbacks callbacks) {
        return new DefaultActorSystemInstance(name, config, callbacks, classLoader);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("classLoader", classLoader).toString();
    }
}
