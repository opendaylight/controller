
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentMap;
/**
 * 
 * This interface provides APIs to configure and manage static routes.
 *
 */
import org.opendaylight.controller.sal.utils.Status;

public interface IForwardingStaticRouting {

    /**
     * Retrieves the StaticRoute that has the longest prefix matching the ipAddress.
     * @param ipAddress (InetAddress) the IP address
     * @return StaticRoute
     */
    public StaticRoute getBestMatchStaticRoute(InetAddress ipAddress);

    /**
     * Returns all the StaticRouteConfig
     * @return all the StaticRouteConfig
     */
    public ConcurrentMap<String, StaticRouteConfig> getStaticRouteConfigs();

    /**
     * Adds a StaticRouteConfig
     * @param config: the StaticRouteConfig to be added
     * @return a text string indicating the result of the operation..
     * If the operation is successful, the return string will be "SUCCESS"
     */
    public Status addStaticRoute(StaticRouteConfig config);

    /**
     * Removes  the named StaticRouteConfig
     * @param name: the name of the StaticRouteConfig to be removed
     * @return a text string indicating the result of the operation.
     * If the operation is successful, the return string will be "SUCCESS"
     */
    public Status removeStaticRoute(String name);

    /**
     * Saves the config
     * @return a text string indicating the result of the operation.
     * If the operation is successful, the return string will be "Success"
     */
    Status saveConfig();
}
