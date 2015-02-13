/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;

/**
 * Factory that must be registered in OSGi service registry in order to be used
 * by netconf-impl. Responsible for creating per-session instances of
 * {@link NetconfOperationService}.
 */
public interface NetconfOperationServiceFactory {

    /**
     * Get capabilities supported by current operation service.
     */
    Set<Capability> getCapabilities();

    /**
     * Supported capabilities may change over time, registering a listener allows for push based information retrieval about current notifications
     */
    AutoCloseable registerCapabilityListener(CapabilityListener listener);

    NetconfOperationService createService(String netconfSessionIdForReporting);

}
