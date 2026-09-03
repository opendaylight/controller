/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.google.common.base.VerifyException;
import java.util.Dictionary;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

@NonNullByDefault
@Component(factory = OSGiActorSystemInstance.FACTORY_NAME, service = ActorSystemInstance.class)
public final class OSGiActorSystemInstance extends AbstractActorSystemInstance {
    // OSGi DS Component Factory name
    static final String FACTORY_NAME = "org.opendaylight.controller.pekko.support.impl.OSGiActorSystemInstance";

    private static final Logger LOG = LoggerFactory.getLogger(OSGiActorSystemInstance.class);
    private static final String PROP_CALLBACKS = ".callbacks";
    private static final String PROP_LOADER = ".loader";
    private static final String PROP_READER = ".reader";
    private static final String PROP_UUID = "uuid";

    @Activate
    public OSGiActorSystemInstance(final Map<String, ?> properties) {
        super("opendaylight-cluster-data",
            prop(properties, PROP_READER, ConfigurationReader.class).read(),
            prop(properties, PROP_CALLBACKS, OSGiCallbacks.class),
            prop(properties, PROP_LOADER, PekkoAccessibleClassLoader.class));
    }

    private static <T> T prop(final Map<String, ?> map, final String key, final Class<T> type) {
        final var ret = map.get(key);
        if (ret == null) {
            throw new VerifyException("missing " + key);
        }
        return type.cast(ret);
    }

    static Dictionary<String, ?> props(final ConfigurationReader reader, final PekkoAccessibleClassLoader classLoader,
            final OSGiCallbacks callbacks) {
        return FrameworkUtil.asDictionary(Map.of(
            PROP_CALLBACKS, callbacks,
            PROP_LOADER, classLoader,
            PROP_READER, reader,
            PROP_UUID, UUID.randomUUID()));
    }

    @Override
    @Deactivate
    public void close() throws TimeoutException, InterruptedException {
        final var name = name();
        try {
            shutdownAndWait(Duration.Inf());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Actor system '{}' shutdown not completed yet: wait interrupted", name, e);
            throw e;
        } catch (TimeoutException e) {
            LOG.warn("Actor system '{}' shutdown not completed yet: wait timed out", name, e);
            throw e;
        }
    }
}
