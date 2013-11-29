/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Preconditions;
import io.netty.util.internal.ConcurrentSet;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

public class NetconfMonitoringServiceImpl implements NetconfMonitoringService, SessionMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(NetconfMonitoringServiceImpl.class);

    private final Set<NetconfManagementSession> sessions = new ConcurrentSet<>();

    @Override
    public void onSessionUp(NetconfManagementSession session) {
        logger.debug("Session {} up", session);
        Preconditions.checkState(sessions.contains(session) == false, "Session %s was already added", session);
        sessions.add(session);
    }

    @Override
    public void onSessionDown(NetconfManagementSession session) {
        logger.debug("Session {} down", session);
        Preconditions.checkState(sessions.contains(session) == true, "Session %s not present", session);
        sessions.remove(session);
    }

    @Override
    public Set<NetconfManagementSession> getManagementSessions() {
        return Collections.unmodifiableSet(sessions);
    }
}
