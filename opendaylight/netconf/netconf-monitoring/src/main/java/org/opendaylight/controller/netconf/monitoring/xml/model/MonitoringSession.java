/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml.model;

import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class MonitoringSession {

    @XmlTransient
    private NetconfManagementSession managementSession;

    public MonitoringSession(NetconfManagementSession managementSession) {
        this.managementSession = managementSession;
    }

    public MonitoringSession() {
    }

    public void setManagementSession(NetconfManagementSession managementSession) {
        this.managementSession = managementSession;
    }

    @XmlElement(name = "id")
    public long getId() {
        return managementSession.getId();
    }

    @XmlElement(name = "source-host")
    public String getSourceHost() {
        return managementSession.getSourceHost().getDomainName().getValue();
    }

    @XmlElement(name = "login-time")
    public String getLoginTime() {
        return managementSession.getLoginTime().getValue();
    }
}
