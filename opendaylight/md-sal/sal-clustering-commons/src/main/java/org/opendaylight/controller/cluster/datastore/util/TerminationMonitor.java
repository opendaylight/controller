/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.util;

import org.apache.pekko.actor.Terminated;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.opendaylight.controller.cluster.common.actor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TerminationMonitor extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationMonitor.class);
    public static final String ADDRESS = "termination-monitor";

    TerminationMonitor() {
        LOG.debug("Created TerminationMonitor");
    }

    @Override
    public void onReceive(final Object message) {
        switch (message) {
            case Terminated terminated -> LOG.debug("Actor terminated : {}", terminated.actor());
            case Monitor monitor -> getContext().watch(monitor.getActorRef());
            case null, default -> {
                // no-op
            }
        }
    }
}
