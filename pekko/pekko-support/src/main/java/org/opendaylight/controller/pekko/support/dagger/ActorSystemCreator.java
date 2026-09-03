/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.dagger;

import com.google.common.annotations.Beta;
import com.typesafe.config.Config;
import org.apache.pekko.actor.Address;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;

/**
 * A factory component capable of creating {@link ActorSystemInstance}s.
 *
 * @since 14.0.0
 */
// FIXME: this really should be a .dagger thing
@Beta
@NonNullByDefault
public interface ActorSystemCreator {
    /**
     * Callbacks invoked on lifecycle events.
     */
    interface Callbacks {
        /**
         * Invoked when remoting was downed locally.
         */
        void onLocalDown();

        /**
         * Invoked when remoting was quarententined.
         *
         * @param quarantinedBy the address which caused the quarantine
         */
        void onRemoteQuarantined(Address quarantinedBy);
    }

    /**
     * Create a new {@link ActorSystemInstance}.
     *
     * @param name the actor system name
     * @param config the actor system configuration
     * @param callbacks lifecycle {@link Callbacks}
     * @return an {@link ActorSystemInstance} that needs to be shut down
     */
    ActorSystemInstance.WithShutdown createInstance(String name, Config config, Callbacks callbacks);

    /**
     * Create a new {@link ActorSystemInstance}.
     *
     * @param name the actor system name
     * @param configReader the {@link ConfigurationReader}
     * @param callbacks lifecycle {@link Callbacks}
     * @return an {@link ActorSystemInstance} that needs to be shut down
     */
    default ActorSystemInstance.WithShutdown createInstance(final String name, final ConfigurationReader configReader,
            final Callbacks callbacks) {
        return createInstance(name, configReader.read(), callbacks);
    }
}
