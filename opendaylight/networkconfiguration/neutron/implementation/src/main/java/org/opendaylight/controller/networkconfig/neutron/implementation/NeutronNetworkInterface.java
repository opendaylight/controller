/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronNetworkInterface extends AbstractNeutronInterface<NeutronNetwork>
                                     implements INeutronNetworkCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronNetworkInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronNetworks";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean networkInUse(String netUUID) {
        NeutronNetwork target = db.get(netUUID);
        return target != null && target.getPortsOnNetwork().size() > 0;
    }
}
