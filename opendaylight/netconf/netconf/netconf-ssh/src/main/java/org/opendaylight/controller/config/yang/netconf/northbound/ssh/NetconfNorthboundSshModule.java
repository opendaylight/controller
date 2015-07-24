package org.opendaylight.controller.config.yang.netconf.northbound.ssh;

import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.opendaylight.controller.netconf.api.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfNorthboundSshModule extends org.opendaylight.controller.config.yang.netconf.northbound.ssh.AbstractNetconfNorthboundSshModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfNorthboundSshModule.class);

    public NetconfNorthboundSshModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfNorthboundSshModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.netconf.northbound.ssh.NetconfNorthboundSshModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NetconfServerDispatcher dispatch = getDispatcherDependency();

        final LocalAddress localAddress = new LocalAddress(getPort().toString());
        final ChannelFuture localServer = dispatch.createLocalServer(localAddress);

        final SshProxyServer sshProxyServer = new SshProxyServer(Executors.newScheduledThreadPool(1), getWorkerThreadGroupDependency(), getEventExecutorDependency());

        final InetSocketAddress bindingAddress = getInetAddress();
        final SshProxyServerConfigurationBuilder sshProxyServerConfigurationBuilder = new SshProxyServerConfigurationBuilder();
        sshProxyServerConfigurationBuilder.setBindingAddress(bindingAddress);
        sshProxyServerConfigurationBuilder.setLocalAddress(localAddress);
        sshProxyServerConfigurationBuilder.setAuthenticator(new UserAuthenticator(getUsername(), getPassword()));
        sshProxyServerConfigurationBuilder.setIdleTimeout(Integer.MAX_VALUE);
        sshProxyServerConfigurationBuilder.setKeyPairProvider(new PEMGeneratorHostKeyProvider());

        localServer.addListener(new GenericFutureListener<ChannelFuture>() {

            @Override
            public void operationComplete(final ChannelFuture future) {
                if(future.isDone() && !future.isCancelled()) {
                    try {
                        sshProxyServer.bind(sshProxyServerConfigurationBuilder.createSshProxyServerConfiguration());
                        LOG.info("Netconf SSH endpoint started successfully at {}", bindingAddress);
                    } catch (final IOException e) {
                        throw new RuntimeException("Unable to start SSH netconf server", e);
                    }
                } else {
                    LOG.warn("Unable to start SSH netconf server at {}", bindingAddress, future.cause());
                    throw new RuntimeException("Unable to start SSH netconf server", future.cause());
                }
            }
        });

        return new NetconfServerCloseable(localServer, sshProxyServer);
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


    private static final class UserAuthenticator implements PasswordAuthenticator {

        private final String username;
        private final String password;

        public UserAuthenticator(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public boolean authenticate(final String username, final String password, final ServerSession session) {
            // FIXME use aaa stuff here instead
            return this.username.equals(username) && this.password.equals(password);
        }
    }
}
