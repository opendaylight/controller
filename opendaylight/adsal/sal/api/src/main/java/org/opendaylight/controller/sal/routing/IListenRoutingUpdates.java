
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.routing;

/**
 * Interface that will be implemented by the modules that wants to
 * know events published by the routing engine
 *
 */

public interface IListenRoutingUpdates {
    /**
     * Method invoked when the recalculation of the all shortest path
     * tree is done
     *
     */
    public void recalculateDone();

}
