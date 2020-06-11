/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import akka.actor.Props;
import akka.japi.Effect;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuarantinedMonitorActorPropsFactory {
    private static final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitorActorPropsFactory.class);

    private static final String DEFAULT_HANDLING_DISABLED_PROPERTY = "default_quarantine_event_handling_disabled";

    private QuarantinedMonitorActorPropsFactory() {

    }

    public static Props createProps(final BundleContext bundleContext) {
        Effect handling = () -> {
            // restart the entire karaf container
            LOG.warn("Restarting karaf container");
            System.setProperty("karaf.restart.jvm", "true");
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
        };

        try {
            final String property = System.getProperty(DEFAULT_HANDLING_DISABLED_PROPERTY);
            if (property != null && property.equalsIgnoreCase("true")) {
                handling = () -> {};
            }
        } catch (SecurityException secEx) {
            LOG.warn("Access denied to system property {}. "
                + "Therefore default handling will be used", DEFAULT_HANDLING_DISABLED_PROPERTY);
        }
        return QuarantinedMonitorActor.props(handling);
    }
}
