
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting.northbound;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * StaticRoute represents the static route data that is returned as a response to
 * the NorthBound GET request.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class StaticRoute {
    /**
     * The name of the static route.
     */
    @XmlElement
    private String name;

    /**
     * The prefix for the route.
     * Format: A.B.C.D/MM  Where A.B.C.D is the Default Gateway IP (L3) or ARP Querier IP (L2)
     */
    @XmlElement
    private String prefix;

    /**
     * NextHop IP-Address (or) datapath ID/port list: xx:xx:xx:xx:xx:xx:xx:xx/a,b,c-m,r-t,y
     */
    @XmlElement
    private String nextHop;

    public StaticRoute() {
    }

    public StaticRoute(String name, String prefix, String nextHop) {
        super();
        this.name = name;
        this.prefix = prefix;
        this.nextHop = nextHop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNextHop() {
        return nextHop;
    }

    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }
}
