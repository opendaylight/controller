/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronRouterInterface extends AbstractNeutronInterface<NeutronRouter>
                                    implements INeutronRouterCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronRouterInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronRouters";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean routerInUse(String routerUUID) {
        NeutronRouter target = db.get(routerUUID);
        return target != null && target.getInterfaces().size() > 0;
    }
}
