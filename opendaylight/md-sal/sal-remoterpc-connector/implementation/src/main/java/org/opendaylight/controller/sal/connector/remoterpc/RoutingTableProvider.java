/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.remoterpc.impl.RoutingTableImpl;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class RoutingTableProvider implements AutoCloseable {

    @SuppressWarnings("rawtypes")
    final ServiceTracker<RoutingTable,RoutingTable> tracker;

    private RoutingTableImpl routingTableImpl = null;

    //final private RouteChangeListener routeChangeListener;
    
    
    public RoutingTableProvider(BundleContext ctx){//,RouteChangeListener rcl) {
        @SuppressWarnings("rawtypes")
        ServiceTracker<RoutingTable, RoutingTable> rawTracker = new ServiceTracker<>(ctx, RoutingTable.class, null);
        tracker = rawTracker;
        tracker.open();

        //routeChangeListener = rcl;
    }
    
    public Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> getRoutingTable() {
        @SuppressWarnings("unchecked")
        RoutingTable<RpcRouter.RouteIdentifier,String> tracked = tracker.getService();

        if(tracked instanceof RoutingTableImpl){
            if(routingTableImpl != tracked){
             routingTableImpl= (RoutingTableImpl)tracked;
             //routingTableImpl.setRouteChangeListener(routeChangeListener);
            }
        }

        return Optional.fromNullable(tracked);
    }

    @Override
    public void close() throws Exception {
        tracker.close();
    }
}
