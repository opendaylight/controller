/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronRouterRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="router")
    NeutronRouter singletonRouter;

    @XmlElement(name="routers")
    List<NeutronRouter> bulkRequest;

    NeutronRouterRequest() {
    }

    NeutronRouterRequest(List<NeutronRouter> bulk) {
        bulkRequest = bulk;
        singletonRouter = null;
    }

    NeutronRouterRequest(NeutronRouter router) {
        singletonRouter = router;
    }

    public List<NeutronRouter> getBulk() {
        return bulkRequest;
    }

    public NeutronRouter getSingleton() {
        return singletonRouter;
    }

    public boolean isSingleton() {
        return (singletonRouter != null);
    }
}
