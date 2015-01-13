/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.home;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.stream.StreamIoHandler;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.ClientSession;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.common.SshdSocketAddress;
import org.opendaylight.controller.config.yang.md.sal.connector.netconf.NetconfConnectorModule;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfReversedClientConfiguration;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfStateSchemas;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalFacade;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfCallHome extends StreamIoHandler implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfCallHome.class);

    private static final String DEFAULT_BINDING_ADDRESS = "0.0.0.0";
    private final NioSocketAcceptor acceptor;
    private final Map<Long, AutoCloseable> openedSessions = Maps.newHashMap();

    private NetconfCallHome() {
        acceptor = new NioSocketAcceptor();
    }

    @Override
    protected void processStreamIo(final IoSession ioSession, final InputStream in, final OutputStream out) {


        final RemoteDeviceId id = new RemoteDeviceId("called-home");

        final ExecutorService globalProcessingExecutor = Executors.newSingleThreadExecutor();

        final Broker domBroker = null;
        final BindingAwareBroker bindingBroker = null;

        final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade
                = new NetconfDeviceSalFacade(id, domBroker, bindingBroker, null, globalProcessingExecutor);

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO =
                new NetconfDevice.SchemaResourcesDTO(schemaRegistry, schemaContextFactory, new NetconfStateSchemas.NetconfStateSchemasResolverImpl());

        final NetconfDevice device =
                new NetconfDevice(schemaResourcesDTO, id, salFacade, globalProcessingExecutor, new NetconfMessageTransformer());

        final NetconfDeviceCommunicator listener = new NetconfDeviceCommunicator(id, device);

        final NetconfReversedClientConfiguration clientConfig = getClientConfig(listener, ioSession);

        final NetconfClientDispatcher dispatcher = null;
        listener.initializeRemoteConnection(dispatcher, clientConfig);

        final NetconfConnectorModule.NetconfConnectorCloseable netconfConnectorCloseable = new NetconfConnectorModule.NetconfConnectorCloseable(listener, salFacade);
        synchronized (openedSessions) {
            // TODO check
            openedSessions.put(ioSession.getId(), netconfConnectorCloseable);
        }
    }



    private NetconfReversedClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener, IoSession tcpSession) {
        return new NetconfReversedClientConfiguration(1000L, new NetconfHelloMessageAdditionalHeader("a", "127.0.0.1", "830", "ssh", "abc"), listener, new AuthenticationHandler() {
            @Override
            public String getUsername() {
                return "q";
            }

            @Override
            public AuthFuture authenticate(final ClientSession session) throws IOException {
                session.addPasswordIdentity("abcd");
                return session.auth();
            }
        }, tcpSession);
    }

    public static NetconfCallHome init(final int port) {
        final NetconfCallHome handler = new NetconfCallHome();
        handler.acceptor.setHandler(handler);
        try {
            handler.acceptor.bind(new SshdSocketAddress(DEFAULT_BINDING_ADDRESS, port));
        } catch (final IOException e) {
            // FIXME
            e.printStackTrace();
        }
        return handler;
    }

    @Override
    public void close() throws Exception {
        synchronized (openedSessions) {
            for (final AutoCloseable openedSession : openedSessions.values()) {
                openedSession.close();
            }
        }
        acceptor.unbind();
    }

    @Override
    public void sessionClosed(final IoSession ioSession) throws Exception {
        LOG.debug("Session from {} with id: {} is closing", ioSession.getRemoteAddress(), ioSession.getId());
        synchronized (openedSessions) {
            // TODO check
            openedSessions.get(ioSession.getId()).close();
        }
    }

}
