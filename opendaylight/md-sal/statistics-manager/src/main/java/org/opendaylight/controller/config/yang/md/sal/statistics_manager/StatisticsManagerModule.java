package org.opendaylight.controller.config.yang.md.sal.statistics_manager;

import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerConfig;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsManagerModule extends org.opendaylight.controller.config.yang.md.sal.statistics_manager.AbstractStatisticsManagerModule {
    private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerModule.class);

    private final static int MAX_NODES_FOR_COLLECTOR_DEFAULT = 16;
    private final static int MIN_REQUEST_NET_MONITOR_INTERVAL_DEFAULT = 3000;

    private StatisticsManager statisticsManagerProvider;

    public StatisticsManagerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public StatisticsManagerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final StatisticsManagerModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("StatisticsManager module initialization.");
        final StatisticsManagerConfig config = createConfig();
        statisticsManagerProvider = new StatisticsManagerImpl(getDataBrokerDependency(), config);
        statisticsManagerProvider.start(getNotificationServiceDependency(), getRpcRegistryDependency());
        LOG.info("StatisticsManager started successfully.");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                try {
                    statisticsManagerProvider.close();
                }
                catch (final Exception e) {
                    LOG.error("Unexpected error by stopping StatisticsManager module", e);
                }
                LOG.info("StatisticsManager module stopped.");
            }
        };
    }

    public StatisticsManagerConfig createConfig() {
        final StatisticsManagerConfig.StatisticsManagerConfigBuilder builder = StatisticsManagerConfig.builder();
        if (getStatisticsManagerSettings() != null && getStatisticsManagerSettings().getMaxNodesForCollector() != null) {
            builder.setMaxNodesForCollector(getStatisticsManagerSettings().getMaxNodesForCollector());
        } else {
            LOG.warn("Load the xml ConfigSubsystem input value fail! MaxNodesForCollector value is set to {} ",
                    MAX_NODES_FOR_COLLECTOR_DEFAULT);
            builder.setMaxNodesForCollector(MAX_NODES_FOR_COLLECTOR_DEFAULT);
        }
        if (getStatisticsManagerSettings() != null &&
                getStatisticsManagerSettings().getMinRequestNetMonitorInterval() != null) {
            builder.setMinRequestNetMonitorInterval(getStatisticsManagerSettings().getMinRequestNetMonitorInterval());
        } else {
            LOG.warn("Load the xml CofnigSubsystem input value fail! MinRequestNetMonitorInterval value is set to {} ",
                    MIN_REQUEST_NET_MONITOR_INTERVAL_DEFAULT);
            builder.setMinRequestNetMonitorInterval(MIN_REQUEST_NET_MONITOR_INTERVAL_DEFAULT);
        }
        return builder.build();
    }

}
