package org.opendaylight.tutorial.mibs;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.MibsTutorialService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MIBTutorialProvider extends AbstractBindingAwareProvider {
    static final Logger LOG  = LoggerFactory.getLogger(MIBTutorialProvider.class);

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {

        SnmpService snmpService = providerContext.getRpcService(SnmpService.class);

        MIBTutorialImpl impl = new MIBTutorialImpl(snmpService);
        providerContext.addRpcImplementation(MibsTutorialService.class, impl);
        LOG.info("MIB tutorial service Initialized");
    }

}
