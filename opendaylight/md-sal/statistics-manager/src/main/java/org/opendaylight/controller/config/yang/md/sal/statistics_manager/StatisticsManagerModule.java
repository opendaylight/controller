package org.opendaylight.controller.config.yang.md.sal.statistics_manager;

import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsManagerModule extends org.opendaylight.controller.config.yang.md.sal.statistics_manager.AbstractStatisticsManagerModule {
    private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerModule.class);

    private StatisticsManager statisticsManagerProvider;

    public StatisticsManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public StatisticsManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, StatisticsManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("StatisticsManagerActivator initialization.");
        statisticsManagerProvider = new StatisticsManagerImpl(getDataBrokerDependency(),
                getStatisticsManagerSettings().getMaxNodesForCollector());
        statisticsManagerProvider.start(getNotificationServiceDependency(), getRpcRegistryDependency(),
                getStatisticsManagerSettings().getMinRequestNetMonitorInterval());
        LOG.info("StatisticsManagerActivator started successfully.");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                try {
                    statisticsManagerProvider.close();
                }
                catch (final Exception e) {
                    LOG.error("Unexpected error by stopping StatisticsManagerActivator", e);
                }
                LOG.info("StatisticsManagerActivator stoped.");
            }
        };
    }

}
