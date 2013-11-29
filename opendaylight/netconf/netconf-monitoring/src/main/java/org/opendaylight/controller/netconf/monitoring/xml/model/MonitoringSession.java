/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml.model;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfSsh;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

final class MonitoringSession {

    @XmlTransient
    private Session managementSession;

    public MonitoringSession(Session managementSession) {
        this.managementSession = managementSession;
    }

    public MonitoringSession() {
    }

    public void setManagementSession(Session managementSession) {
        this.managementSession = managementSession;
    }

    @XmlElement(name = "session-id")
    public long getId() {
        return managementSession.getSessionId();
    }

    @XmlElement(name = "source-host")
    public String getSourceHost() {
        return managementSession.getSourceHost().getDomainName().getValue();
    }

    @XmlElement(name = "login-time")
    public String getLoginTime() {
        return managementSession.getLoginTime().getValue();
    }

    @XmlElement(name = "in-bad-rpcs")
    public Long getInBadRpcs() {
        return managementSession.getInBadRpcs().getValue();
    }

    @XmlElement(name = "in-rpcs")
    public Long getInRpcs() {
        return managementSession.getInRpcs().getValue();
    }

    @XmlElement(name = "out-notifications")
    public Long getOutNotifications() {
        return managementSession.getOutNotifications().getValue();
    }

    @XmlElement(name = "out-rpc-errors")
    public Long getOutRpcErrors() {
        return managementSession.getOutRpcErrors().getValue();
    }

    @XmlElement(name = "transport")
    public String getTransport() {
        Preconditions.checkState(managementSession.getTransport() == NetconfSsh.class);
        return "netconf-ssh";
    }

    @XmlElement(name = "username")
    public String getUsername() {
        return managementSession.getUsername();
    }
}
