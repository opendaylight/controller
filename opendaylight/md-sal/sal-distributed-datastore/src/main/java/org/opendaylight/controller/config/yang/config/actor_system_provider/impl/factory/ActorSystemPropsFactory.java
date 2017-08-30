/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.actor_system_provider.impl.factory;

import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.opendaylight.controller.config.yang.config.actor_system_provider.impl.ActorSystemProviderImpl;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorSystemPropsFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemProviderImpl.class);

    public static Props createProps(final BundleContext bundleContext) {
        return QuarantinedMonitorActor.props(() -> {
            // restart the entire karaf container
            LOG.warn("Restarting karaf container");
            System.setProperty("karaf.restart.jvm", "true");
            bundleContext.getBundle().stop();
        });
    }
}
