/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.typesafe.config.Config;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.dagger.ActorSystemCreator;

/**
 * Abstract base class for {@link ActorSystemInstance} implementations.
 */
public final class DefaultActorSystemInstance extends AbstractActorSystemInstance {
    /**
     * Default constructor.
     *
     * @param name the actor system name
     * @param config the actor system configuration
     * @param callbacks lifecycle {@link Callbacks}
     * @param classLoader actor system class loader
     */
    @NonNullByDefault
    DefaultActorSystemInstance(final String name, final Config rawConfig, final ActorSystemCreator.Callbacks callbacks,
            final PekkoAccessibleClassLoader classLoader) {
        super(name, rawConfig, callbacks, classLoader);
    }

    @Override
    public void close() throws TimeoutException, InterruptedException {
        shutdownAndWait(Duration.ofMinutes(1));
    }
}
