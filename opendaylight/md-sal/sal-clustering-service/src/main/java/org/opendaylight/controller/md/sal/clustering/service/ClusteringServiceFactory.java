/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.clustering.service;

import org.opendaylight.controller.md.sal.clustering.service.listener.LastRoleChangeListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

public class ClusteringServiceFactory {
    private static final ClusteringServiceFactory factory = new ClusteringServiceFactory();

    private ClusteringServiceFactory() {
    }

    private volatile ClusteringService cs;

    public static ClusteringServiceFactory getFactory() {
        return factory;
    }

    public ClusteringService getClusteringServiceInstance(
            NotificationProviderService notificationProviderService, LastRoleChangeListener lastRoleChangeListener) {
        if (cs == null) {
            synchronized (this) {
                if (cs == null) {
                    cs = new ClusteringServiceImpl(notificationProviderService, lastRoleChangeListener);
                }
            }
        }
        return cs;
    }
}
