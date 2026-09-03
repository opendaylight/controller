/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;
import java.util.UUID;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.ActorSystemInstance.Callbacks;

/**
 * Default base class for {@link ActorSystemInstance.Creator} implementations.
 */
@NonNullByDefault
public abstract class ActorSystemInstanceCreator implements ActorSystemInstance.Creator {
    /**
     * The {@link UUID} of this creator.
     */
    protected final UUID uuid = UUID.randomUUID();

    /**
     * {@return the class loader to use with created {@link ActorSystemInstance}s}
     */
    protected abstract ClassLoader classLoader();

    @Override
    public final ActorSystemInstance.WithShutdown createInstance(final String name, final Config config,
            final Callbacks callbacks) {
        return new DefaultActorSystemInstance(name, config, callbacks, classLoader());
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("uuid", uuid).add("classLoader", classLoader()).toString();
    }
}
