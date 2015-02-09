package org.opendaylight.controller.config.yang.config.netconf.northbound.impl;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.netconf.impl.CommitNotifier;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcherImpl;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;

public class NetconfServerDispatcherModule extends org.opendaylight.controller.config.yang.config.netconf.northbound.impl.AbstractNetconfServerDispatcherModule {
    public NetconfServerDispatcherModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfServerDispatcherModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.netconf.northbound.impl.NetconfServerDispatcherModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkCondition(getConnectionTimeoutMillis() > 0, "Invalid connection timeout", connectionTimeoutMillisJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final NetconfOperationServiceFactoryListenerImpl aggregatedOpProvider = getAggregatedOpProvider();
        final SessionMonitoringService monitoringService = startMonitoringService(aggregatedOpProvider);
        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                getTimerDependency(), aggregatedOpProvider, new SessionIdProvider(), getConnectionTimeoutMillis(), CommitNotifier.NoopCommitNotifier.getInstance(), monitoringService);
        final NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(
                serverNegotiatorFactory);

        return new NetconfServerDispatcherImpl(serverChannelInitializer, getBossThreadGroupDependency(), getWorkerThreadGroupDependency()) {

            @Override
            public void close() {
                // NOOP, close should not be present here, the deprecated method closes injected evet loop groups
            }
        };

    }

    private NetconfMonitoringServiceImpl startMonitoringService(final NetconfOperationServiceFactoryListenerImpl netconfOperationProvider) {
        return new NetconfMonitoringServiceImpl(netconfOperationProvider);
    }

    private NetconfOperationServiceFactoryListenerImpl getAggregatedOpProvider() {
        final NetconfOperationServiceFactoryListenerImpl netconfOperationProvider = new NetconfOperationServiceFactoryListenerImpl();
        for (final NetconfOperationServiceFactory netconfOperationServiceFactory : getMappersDependency()) {
            netconfOperationProvider.onAddNetconfOperationServiceFactory(netconfOperationServiceFactory);
        }
        return netconfOperationProvider;
    }


}
