package org.opendaylight.controller.config.yang.md.sal.statistics_manager;

import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.impl.SMOperationalStatusService;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerConfig;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.StatisticsManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsManagerModule extends org.opendaylight.controller.config.yang.md.sal.statistics_manager.AbstractStatisticsManagerModule {
    private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerModule.class);

    private final static int MAX_NODES_FOR_COLLECTOR_DEFAULT = 16;
    private final static int MIN_REQUEST_NET_MONITOR_INTERVAL_DEFAULT = 30000;

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
        LOG.info("StatisticsManager module initialization.");
        StatisticsManagerConfig config = createConfig();
        statisticsManagerProvider = new StatisticsManagerImpl(getDataBrokerDependency(), config);
        statisticsManagerProvider.start(getNotificationServiceDependency(), getRpcRegistryDependency());
        SMOperationalStatusService smOperationalStatusService = new SMOperationalStatusService(statisticsManagerProvider);
        getRpcRegistryDependency().addRpcImplementation(StatisticsManagerService.class, smOperationalStatusService);
        LOG.info("StatisticsManager started successfully.");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                try {
                    statisticsManagerProvider.close();
                } catch (final Exception e) {
                    LOG.error("Unexpected error by stopping StatisticsManager module", e);
                }
                LOG.info("StatisticsManager module stopped.");
            }
        };
    }

    public StatisticsManagerConfig createConfig() {
        StatisticsManagerConfig.StatisticsManagerConfigBuilder builder = StatisticsManagerConfig.builder();
        if (getStatisticsManagerSettings() != null && getStatisticsManagerSettings().getMaxNodesForCollector() != null) {
            builder.setMaxNodesForCollector(getStatisticsManagerSettings().getMaxNodesForCollector());
        } else {
            builder.setMaxNodesForCollector(MAX_NODES_FOR_COLLECTOR_DEFAULT);
        }
        if (getStatisticsManagerSettings() != null &&
                getStatisticsManagerSettings().getMinRequestNetMonitorInterval() != null) {
            builder.setMinRequestNetMonitorInterval(getStatisticsManagerSettings().getMinRequestNetMonitorInterval());
        } else {
            builder.setMinRequestNetMonitorInterval(MIN_REQUEST_NET_MONITOR_INTERVAL_DEFAULT);
        }
        return builder.build();
    }

}
