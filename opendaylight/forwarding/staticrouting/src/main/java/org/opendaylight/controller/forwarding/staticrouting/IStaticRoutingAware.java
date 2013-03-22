
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;

/**
 * Interface that will be implemented by the modules that want to
 * know when a Static Route is added or deleted.
 *
 */
public interface IStaticRoutingAware {

    /**
     * This method  is called when a StaticRoute has added or deleted.
     * @param s: StaticRoute
     * @param added: boolean true if the static route is added,
     */
    public void staticRouteUpdate(StaticRoute s, boolean added);
}
