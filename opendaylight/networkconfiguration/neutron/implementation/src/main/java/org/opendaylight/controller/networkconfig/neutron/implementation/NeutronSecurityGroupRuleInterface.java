package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSecurityGroupRuleInterface
        extends AbstractNeutronInterface<NeutronSecurityGroupRule>
        implements INeutronSecurityGroupRuleCRUD {
    protected static final Logger logger = LoggerFactory.getLogger(NeutronSecurityGroupRuleInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronSecurityGroupRules";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean add(NeutronSecurityGroupRule input) {
        if (!super.add(input)) {
            return false;
        }

        INeutronSecurityGroupCRUD secGroupCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        secGroupCRUD.get(input.getSecGroupUUID()).addRule(input);
        return true;
    }

    @Override
    public boolean remove(String uuid) {
        if (!exists(uuid)) {
            return false;
        }
        NeutronSecurityGroupRule sgGroupRule = get(uuid);
        db.remove(uuid);

        INeutronSecurityGroupCRUD secGroupCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        secGroupCRUD.get(sgGroupRule.getSecGroupUUID()).removeRule(sgGroupRule);
        return true;
    }
}
