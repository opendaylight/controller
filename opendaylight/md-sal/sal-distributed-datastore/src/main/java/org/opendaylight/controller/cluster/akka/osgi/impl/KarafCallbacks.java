/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.Address;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance.Callbacks;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
record KarafCallbacks(BundleContext bundleContext) implements Callbacks {
    private static final Logger LOG = LoggerFactory.getLogger(KarafCallbacks.class);

    KarafCallbacks {
        requireNonNull(bundleContext);
    }

    @Override
    public void onLocalDown() {
        restartKaraf();
    }

    @Override
    public void onRemoteQuarantined(final Address quarantinedBy) {
        restartKaraf();
    }

    private void restartKaraf() {
        // restart the entire karaf container
        LOG.warn("Restarting karaf container");
        System.setProperty("karaf.restart.jvm", "true");
        System.setProperty("karaf.restart", "true");
        try {
            bundleContext.getBundle(0).stop();
        } catch (BundleException | IllegalStateException | SecurityException e) {
            LOG.error("Failed to stop framework bundle", e);
        }
    }
}