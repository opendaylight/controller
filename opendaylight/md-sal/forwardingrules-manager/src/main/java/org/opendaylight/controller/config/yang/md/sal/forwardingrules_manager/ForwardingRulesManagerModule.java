package org.opendaylight.controller.config.yang.md.sal.forwardingrules_manager;

import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.frm.impl.FRMConfig;
import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardingRulesManagerModule extends org.opendaylight.controller.config.yang.md.sal.forwardingrules_manager.AbstractForwardingRulesManagerModule {
    private final static Logger LOG = LoggerFactory.getLogger(ForwardingRulesManagerModule.class);

    private static final boolean CLEAN_ALIEN_FLOWS_ON_RECONCIL_DEFAULT = false;

    private ForwardingRulesManager forwardingrulessManagerProvider;

    public ForwardingRulesManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ForwardingRulesManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, ForwardingRulesManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("FRM module initialization.");
        FRMConfig config = createConfig();
        forwardingrulessManagerProvider = new ForwardingRulesManagerImpl(getDataBrokerDependency(),
                getRpcRegistryDependency(), getNotificationServiceDependency(), config);
        forwardingrulessManagerProvider.start();
        LOG.info("FRM module started successfully.");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                try {
                    forwardingrulessManagerProvider.close();
                } catch (final Exception e) {
                    LOG.error("Unexpected error by stopping FRM", e);
                }
                LOG.info("FRM module stoped.");
            }
        };
    }

    public FRMConfig createConfig() {
        FRMConfig.FrmConfigBuilder builder = FRMConfig.builder();
        if (getFrmSettings() != null && getFrmSettings().getCleanAlienFlowsOnReconciliation() != null) {
            builder.setCleanAlienFlowsOnReconcil(getFrmSettings().getCleanAlienFlowsOnReconciliation());
        } else {
            // Default to false if nothing is set
            builder.setCleanAlienFlowsOnReconcil(CLEAN_ALIEN_FLOWS_ON_RECONCIL_DEFAULT);
        }
        return builder.build();
    }

}
