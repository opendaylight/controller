/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.routing.dijkstra_implementation.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.ServiceRegistration;

public class DijkstraImplementationCLI {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "getRoute" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(DijkstraImplementationCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Retrieves a Route between two Nodes in the discovered Topology DB")
    public void getRoute(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("String representation of the Source Node, this need to be consumable from Node.fromString()") String srcNode,
            @Descriptor("String representation of the Destination Node") String dstNode) {
        final IRouting r = (IRouting) ServiceHelper.getInstance(IRouting.class, container, this);

        if (r == null) {
            System.out.println("Cannot find the routing instance on container:" + container);
            return;
        }

        final Node src = Node.fromString(srcNode);
        final Node dst = Node.fromString(dstNode);
        final Path p = r.getRoute(src, dst);
        if (p != null) {
            System.out.println("Route between srcNode:" + src + " and dstNode:" + dst + " = " + p);
        } else {
            System.out.println("There is no route between srcNode:" + src + " and dstNode:" + dst);
        }
    }
}
