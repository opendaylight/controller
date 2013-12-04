/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods for CRUD of NB network objects
 *
 */

public interface INeutronNetworkCRUD extends INeutronCRUD<NeutronNetwork> {

    /**
     * Applications call this interface method to determine if a Network object
     * is use
     *
     * @param netUUID
     *            identifier of the network object
     *
     * @return boolean on whether the network is in use or not
     */

    public boolean networkInUse(String netUUID);
}
