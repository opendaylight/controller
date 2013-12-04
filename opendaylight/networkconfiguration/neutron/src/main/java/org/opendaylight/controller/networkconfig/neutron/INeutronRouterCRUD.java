/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods for CRUD of NB Router objects
 *
 */

public interface INeutronRouterCRUD extends INeutronCRUD<NeutronRouter> {

    /**
     * Applications call this interface method to check if a router is in use
     *
     * @param uuid
     *            identifier of the Router object
     * @return boolean on whether the router is in use or not
     */

    public boolean routerInUse(String routerUUID);
}
