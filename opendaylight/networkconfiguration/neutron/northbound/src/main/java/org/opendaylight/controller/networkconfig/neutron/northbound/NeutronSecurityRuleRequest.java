/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronSecurityRuleRequest {
    /**
     * See OpenStack Network API v2.0 Reference for a
     * description of annotated attributes and operations
     */

    @XmlElement(name="security_group_rule")
    NeutronSecurityRule singletonSecurityRule;

    @XmlElement(name="security_group_rules")
    List<NeutronSecurityRule> bulkRequest;

    NeutronSecurityRuleRequest() {
    }

    NeutronSecurityRuleRequest(List<NeutronSecurityRule> bulk) {
        bulkRequest = bulk;
        singletonSecurityRule = null;
    }

    NeutronSecurityRuleRequest(NeutronSecurityRule rule) {
        singletonSecurityRule = rule;
    }

    public NeutronSecurityRule getSingleton() {
        return singletonSecurityRule;
    }

    public boolean isSingleton() {
        return (singletonSecurityRule != null);
    }
    public List<NeutronSecurityRule> getBulk() {
        return bulkRequest;
    }

}