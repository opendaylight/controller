/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml.model;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Set;

@XmlRootElement(name = MonitoringConstants.NETCONF_MONITORING_XML_ROOT_ELEMENT)
public final class NetconfMonitoring {

    private Collection<MonitoringSession> sessions;

    public NetconfMonitoring(NetconfMonitoringService netconfMonitor) {
        this.sessions = transformSessions(netconfMonitor.getManagementSessions());
    }

    public NetconfMonitoring() {
    }

    private Collection<MonitoringSession> transformSessions(Set<NetconfManagementSession> sessions) {
        return Collections2.transform(sessions, new Function<NetconfManagementSession, MonitoringSession>() {
            @Nullable
            @Override
            public MonitoringSession apply(@Nullable NetconfManagementSession input) {
                return new MonitoringSession(input);
            }
        });
    }

    @XmlElementWrapper(name="sessions")
    @XmlElement(name="session")
    public Collection<MonitoringSession> getSessions() {
        return sessions;
    }
}
