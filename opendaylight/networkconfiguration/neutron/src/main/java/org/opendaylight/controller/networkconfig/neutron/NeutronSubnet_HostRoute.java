/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronSubnet_HostRoute {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="destination")
    String destination;

    @XmlElement(name="nexthop")
    String nextHop;

    public NeutronSubnet_HostRoute() { }
}
