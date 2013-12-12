/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.base.Optional;

public class RoutingTableProvider implements AutoCloseable {

    @SuppressWarnings("rawtypes")
    final ServiceTracker<RoutingTable,RoutingTable> tracker;
    
    
    public RoutingTableProvider(BundleContext ctx) {
        @SuppressWarnings("rawtypes")
        ServiceTracker<RoutingTable, RoutingTable> rawTracker = new ServiceTracker<>(ctx, RoutingTable.class, null);
        tracker = rawTracker;
        tracker.open();
    }
    
    public Optional<RoutingTable<String, String>> getRoutingTable() {
        @SuppressWarnings("unchecked")
        RoutingTable<String,String> tracked = tracker.getService();
        return Optional.fromNullable(tracked);
    }

    @Override
    public void close() throws Exception {
        tracker.close();
    }
}
