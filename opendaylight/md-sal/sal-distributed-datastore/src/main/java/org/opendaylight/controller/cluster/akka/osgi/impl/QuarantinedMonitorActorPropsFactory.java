/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import org.apache.pekko.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuarantinedMonitorActorPropsFactory {
    private static final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitorActorPropsFactory.class);

    private static final String DEFAULT_HANDLING_DISABLED =
        "akka.disable-default-actor-system-quarantined-event-handling";

    private QuarantinedMonitorActorPropsFactory() {

    }

    public static Props createProps(final BundleContext bundleContext, final Config akkaConfig) {
        try {
            if (akkaConfig.getBoolean(DEFAULT_HANDLING_DISABLED)) {
                LOG.info("{} was set, default handling is disabled", DEFAULT_HANDLING_DISABLED);
                return QuarantinedMonitorActor.props(() -> { });
            }
        } catch (ConfigException configEx) {
            LOG.info("Pekko config doesn't contain property {}. Therefore default handling will be used",
                DEFAULT_HANDLING_DISABLED);
        }
        return QuarantinedMonitorActor.props(() -> {
            // restart the entire karaf container
            LOG.warn("Restarting karaf container");
            System.setProperty("karaf.restart.jvm", "true");
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
        });
    }
}
