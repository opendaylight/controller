package org.opendaylight.controller.config.yang.netconf.northbound.ssh;

import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.opendaylight.controller.netconf.impl.CommitNotifier;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;

public class NetconfNorthboundSshModule extends org.opendaylight.controller.config.yang.netconf.northbound.ssh.AbstractNetconfNorthboundSshModule {
    public NetconfNorthboundSshModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfNorthboundSshModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.netconf.northbound.ssh.NetconfNorthboundSshModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NetconfOperationServiceFactoryListenerImpl aggregatedOpProvider = getAggregatedOpProvider();
        final SessionMonitoringService monitoringService = startMonitoringService(aggregatedOpProvider);

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(), aggregatedOpProvider, new SessionIdProvider(), 10000L, CommitNotifier.NoopCommitNotifier.getInstance(), monitoringService);

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        final NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        final NetconfServerDispatcher dispatch = new NetconfServerDispatcher(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

        final LocalAddress localAddress = new LocalAddress(getPort().toString());
        final ChannelFuture localServer = dispatch.createLocalServer(localAddress);

        final SshProxyServer sshProxyServer = new SshProxyServer(Executors.newScheduledThreadPool(1), eventLoopGroup, eventLoopGroup);

        final InetSocketAddress bindingAddress = new InetSocketAddress(getPort());
        final SshProxyServerConfigurationBuilder sshProxyServerConfigurationBuilder = new SshProxyServerConfigurationBuilder();
        sshProxyServerConfigurationBuilder.setBindingAddress(bindingAddress);
        sshProxyServerConfigurationBuilder.setLocalAddress(localAddress);
        sshProxyServerConfigurationBuilder.setAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(final String username, final String password, final ServerSession session) {
                // FIXME
                return true;
            }
        });
        sshProxyServerConfigurationBuilder.setIdleTimeout(Integer.MAX_VALUE);
        sshProxyServerConfigurationBuilder.setKeyPairProvider(new PEMGeneratorHostKeyProvider());

        localServer.addListener(new GenericFutureListener<ChannelFuture>() {

            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if(future.isDone() && !future.isCancelled()) {
                    sshProxyServer.bind(sshProxyServerConfigurationBuilder.createSshProxyServerConfiguration());
                }
            }
        });

        return new NetconfServerCloseable(localServer, sshProxyServer);
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
        private final SshProxyServer sshProxyServer;

        public NetconfServerCloseable(final ChannelFuture localServer, final SshProxyServer sshProxyServer) {
            this.localServer = localServer;
            this.sshProxyServer = sshProxyServer;
        }

        @Override
        public void close() throws Exception {
            sshProxyServer.close();

            if(localServer.isDone()) {
                localServer.channel().close();
            } else {
                localServer.cancel(true);
            }
        }
    }


}
