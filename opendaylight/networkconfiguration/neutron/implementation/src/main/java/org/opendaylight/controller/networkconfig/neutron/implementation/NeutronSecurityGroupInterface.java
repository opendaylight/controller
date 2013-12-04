package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSecurityGroupInterface extends AbstractNeutronInterface<NeutronSecurityGroup>
                                           implements INeutronSecurityGroupCRUD {

    protected static final Logger logger = LoggerFactory.getLogger(NeutronSecurityGroupInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronSecurityGroups";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean securityGroupInUse(String uuid) {
        NeutronSecurityGroup target = db.get(uuid);
        return target != null && target.getPorts().size() > 0;
    }
}
