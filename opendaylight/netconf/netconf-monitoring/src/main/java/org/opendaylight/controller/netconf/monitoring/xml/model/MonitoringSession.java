/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Joiner;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.Session1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.opendaylight.yangtools.yang.common.QName;

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
        try {
            QName qName = (QName) managementSession.getTransport().getField("QNAME").get(null);
            // Add extension prefix if transport type is from extension yang module
            if (qName.getNamespace().toString().equals(MonitoringConstants.EXTENSION_NAMESPACE)) {
                return Joiner.on(':').join(MonitoringConstants.EXTENSION_NAMESPACE_PREFIX, qName.getLocalName());
            } else {
                return qName.getLocalName();
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unknown transport type " + managementSession.getTransport(), e);
        }
    }

    @XmlElement(name= "session-identifier", namespace = MonitoringConstants.EXTENSION_NAMESPACE)
    public String getSessionType() {
        return managementSession.getAugmentation(Session1.class).getSessionIdentifier();
    }

    @XmlElement(name = "username")
    public String getUsername() {
        return managementSession.getUsername();
    }
}
