/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.api.monitoring;

import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;

public interface NetconfMonitoringService extends CapabilityListener, SessionListener {

    Sessions getSessions();

    Schemas getSchemas();

    String getSchemaForCapability(String moduleName, Optional<String> revision);

    Capabilities getCapabilities();

    /**
     * Allows push based state information transfer. After the listener is registered, current state is pushed to the listener.
     */
    AutoCloseable registerListener(MonitoringListener listener);

    interface MonitoringListener {

        // TODO more granular updates would make sense
        void onStateChanged(NetconfState state);
    }
}
