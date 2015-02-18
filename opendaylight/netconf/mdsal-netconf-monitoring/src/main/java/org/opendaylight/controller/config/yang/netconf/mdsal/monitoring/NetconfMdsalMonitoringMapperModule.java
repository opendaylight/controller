package org.opendaylight.controller.config.yang.netconf.mdsal.monitoring;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.monitoring.GetSchema;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

public class NetconfMdsalMonitoringMapperModule extends org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.AbstractNetconfMdsalMonitoringMapperModule {
    public NetconfMdsalMonitoringMapperModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfMdsalMonitoringMapperModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.NetconfMdsalMonitoringMapperModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NetconfMonitoringService serverMonitoringDependency = getServerMonitoringDependency();

        final MonitoringToMdsalWriter monitoringToMdsalWriter = new MonitoringToMdsalWriter(serverMonitoringDependency);
        getBindingAwareBrokerDependency().registerProvider(monitoringToMdsalWriter);

        final MdSalMonitoringMapperFactory mdSalMonitoringMapperFactory = new MdSalMonitoringMapperFactory(new MdsalMonitoringMapper(serverMonitoringDependency)) {
            @Override
            public void close() {
                super.close();
                monitoringToMdsalWriter.close();
                getAggregatorDependency().onRemoveNetconfOperationServiceFactory(this);
            }
        };

        getAggregatorDependency().onAddNetconfOperationServiceFactory(mdSalMonitoringMapperFactory);
        return mdSalMonitoringMapperFactory;

    }

    // FIXME almost exactly same code as in netconf-monitoring, refactor
    private static class MdSalMonitoringMapperFactory implements NetconfOperationServiceFactory, AutoCloseable {

        private final NetconfOperationService operationService;

        private static final Set<Capability> CAPABILITIES = Sets.<Capability>newHashSet(new Capability() {

            @Override
            public String getCapabilityUri() {
                return MonitoringConstants.URI;
            }

            @Override
            public Optional<String> getModuleNamespace() {
                return Optional.of(MonitoringConstants.NAMESPACE);
            }

            @Override
            public Optional<String> getModuleName() {
                return Optional.of(MonitoringConstants.MODULE_NAME);
            }

            @Override
            public Optional<String> getRevision() {
                return Optional.of(MonitoringConstants.MODULE_REVISION);
            }

            @Override
            public Optional<String> getCapabilitySchema() {
                return Optional.absent();
            }

            @Override
            public Collection<String> getLocation() {
                return Collections.emptyList();
            }
        });

        private static final AutoCloseable AUTO_CLOSEABLE = new AutoCloseable() {
            @Override
            public void close() throws Exception {
                // NOOP
            }
        };

        private final List<CapabilityListener> listeners = new ArrayList<>();

        public MdSalMonitoringMapperFactory(final NetconfOperationService operationService) {
            this.operationService = operationService;
        }

        @Override
        public NetconfOperationService createService(final String netconfSessionIdForReporting) {
            return operationService;
        }

        @Override
        public Set<Capability> getCapabilities() {
            return CAPABILITIES;
        }

        @Override
        public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
            listener.onCapabilitiesAdded(getCapabilities());
            listeners.add(listener);
            return AUTO_CLOSEABLE;
        }

        @Override
        public void close() {
            for (final CapabilityListener listener : listeners) {
                listener.onCapabilitiesRemoved(getCapabilities());
            }
        }
    }


    private static class MdsalMonitoringMapper implements NetconfOperationService {

        private final NetconfMonitoringService serverMonitoringDependency;

        public MdsalMonitoringMapper(final NetconfMonitoringService serverMonitoringDependency) {
            this.serverMonitoringDependency = serverMonitoringDependency;
        }

        @Override
        public Set<NetconfOperation> getNetconfOperations() {
            return Collections.<NetconfOperation>singleton(new GetSchema(serverMonitoringDependency));
        }

        @Override
        public void close() {
            // NOOP
        }
    }
}
