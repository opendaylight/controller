package org.opendaylight.controller.config.yang.netconf.northbound.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.opendaylight.controller.netconf.impl.CommitNotifier;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;

public class NetconfNorthboundImplModule extends org.opendaylight.controller.config.yang.netconf.northbound.impl.AbstractNetconfNorthboundImplModule {
    public NetconfNorthboundImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfNorthboundImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.netconf.northbound.impl.NetconfNorthboundImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NetconfOperationServiceFactoryListenerImpl aggregaterOpProvider = getAggregatedOpProvider();
        final SessionMonitoringService monitoringService = startMonitoringService(aggregaterOpProvider);

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(), aggregaterOpProvider, new SessionIdProvider(), 10000L, CommitNotifier.NoopCommitNotifier.getInstance(), monitoringService);

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        final NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        final NetconfServerDispatcher dispatch = new NetconfServerDispatcher(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

        final LocalAddress address = NetconfConfigUtil.getNetconfLocalAddress();

        final ChannelFuture localServer = dispatch.createLocalServer(new LocalAddress(getPort().toString()));
        return new NetconfServerCloseable(localServer);
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

    private static class NetconfServerCloseable implements AutoCloseable {
        private final ChannelFuture localServer;

        public NetconfServerCloseable(final ChannelFuture localServer) {
            this.localServer = localServer;
        }

        @Override
        public void close() throws Exception {
            if(localServer.isDone()) {
                localServer.channel().close();
            } else {
                localServer.cancel(true);
            }
        }
    }
}
