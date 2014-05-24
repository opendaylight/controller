/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType (XmlAccessType.NONE)

public class NeutronSecurityGroupRequest {
    /**
    * See OpenStack Network API v2.0 Reference for a
    * description of annotated attributes and operations
    */

    @XmlElement (name = "security_group")
    NeutronSecurityGroup singletonSecurityGroup;

    @XmlElement (name = "security_groups")
    List<NeutronSecurityGroup> bulkRequest;

    NeutronSecurityGroupRequest() {
    }

    NeutronSecurityGroupRequest(List<NeutronSecurityGroup> bulk) {
        bulkRequest = bulk;
        singletonSecurityGroup = null;
    }

    NeutronSecurityGroupRequest(NeutronSecurityGroup group) {
        singletonSecurityGroup = group;
    }

    public List<NeutronSecurityGroup> getBulk() {
        return bulkRequest;
    }

    public NeutronSecurityGroup getSingleton() {
        return singletonSecurityGroup;
    }

    public boolean isSingleton() {
        return (singletonSecurityGroup != null);
    }
}