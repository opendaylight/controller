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
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement(name = MonitoringConstants.NETCONF_MONITORING_XML_ROOT_ELEMENT)
public final class NetconfState {

    private Schemas schemas;
    private Sessions sessions;

    public NetconfState(NetconfMonitoringService monitoringService) {
        this.sessions = monitoringService.getSessions();
        this.schemas = monitoringService.getSchemas();
    }

    public NetconfState() {
    }



    @XmlElementWrapper(name="schemas")
    @XmlElement(name="schema")
    public Collection<MonitoringSchema> getSchemas() {
        return Collections2.transform(schemas.getSchema(), new Function<Schema, MonitoringSchema>() {
            @Nullable
            @Override
            public MonitoringSchema apply(@Nullable Schema input) {
                return new MonitoringSchema(input);
            }
        });
    }

    @XmlElementWrapper(name="sessions")
    @XmlElement(name="session")
    public Collection<MonitoringSession> getSessions() {
        return Collections2.transform(sessions.getSession(), new Function<Session, MonitoringSession>() {
            @Nullable
            @Override
            public MonitoringSession apply(@Nullable Session input) {
                return new MonitoringSession(input);
            }
        });
    }
}
