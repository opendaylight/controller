package org.opendaylight.controller.config.yang.netconf.northbound.tcp;

import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.opendaylight.controller.netconf.api.NetconfServerDispatcher;

public class NetconfNorthboundTcpModule extends org.opendaylight.controller.config.yang.netconf.northbound.tcp.AbstractNetconfNorthboundTcpModule {
    public NetconfNorthboundTcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfNorthboundTcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.netconf.northbound.tcp.NetconfNorthboundTcpModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NetconfServerDispatcher dispatch = getDispatcherDependency();
        final ChannelFuture tcpServer = dispatch.createServer(getInetAddress());
        return new NetconfServerCloseable(tcpServer);
    }

    private InetSocketAddress getInetAddress() {
        try {
            final InetAddress inetAd = InetAddress.getByName(getBindingAddress().getIpv4Address() == null ? getBindingAddress().getIpv6Address().getValue() : getBindingAddress().getIpv4Address().getValue());
            return new InetSocketAddress(inetAd, getPort().getValue());
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unable to bind netconf endpoint to address " + getBindingAddress(), e);
        }
    }

    private static final class NetconfServerCloseable implements AutoCloseable {
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
