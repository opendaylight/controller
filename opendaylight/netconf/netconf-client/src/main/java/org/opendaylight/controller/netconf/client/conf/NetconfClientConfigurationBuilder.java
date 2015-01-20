/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client.conf;

import java.net.InetSocketAddress;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;

public class NetconfClientConfigurationBuilder {

    public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 5000;
    public static final NetconfClientConfiguration.NetconfClientProtocol DEFAULT_CLIENT_PROTOCOL = NetconfClientConfiguration.NetconfClientProtocol.TCP;

    private InetSocketAddress address;
    private long connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;
    private NetconfHelloMessageAdditionalHeader additionalHeader;
    private NetconfClientSessionListener sessionListener;
    private ReconnectStrategy reconnectStrategy;
    private AuthenticationHandler authHandler;
    private NetconfClientConfiguration.NetconfClientProtocol clientProtocol = DEFAULT_CLIENT_PROTOCOL;

    protected NetconfClientConfigurationBuilder() {
    }

    public static NetconfClientConfigurationBuilder create() {
        return new NetconfClientConfigurationBuilder();
    }

    public NetconfClientConfigurationBuilder withAddress(final InetSocketAddress address) {
        this.address = address;
        return this;
    }

    public NetconfClientConfigurationBuilder withConnectionTimeoutMillis(final long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        return this;
    }

    public NetconfClientConfigurationBuilder withProtocol(final NetconfClientConfiguration.NetconfClientProtocol clientProtocol) {
        this.clientProtocol = clientProtocol;
        return this;
    }

    public NetconfClientConfigurationBuilder withAdditionalHeader(final NetconfHelloMessageAdditionalHeader additionalHeader) {
        this.additionalHeader = additionalHeader;
        return this;
    }

    public NetconfClientConfigurationBuilder withSessionListener(final NetconfClientSessionListener sessionListener) {
        this.sessionListener = sessionListener;
        return this;
    }

    public NetconfClientConfigurationBuilder withReconnectStrategy(final ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

    public NetconfClientConfigurationBuilder withAuthHandler(final AuthenticationHandler authHandler) {
        this.authHandler = authHandler;
        return this;
    }

    public final InetSocketAddress getAddress() {
        return address;
    }

    final long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    final NetconfHelloMessageAdditionalHeader getAdditionalHeader() {
        return additionalHeader;
    }

    final NetconfClientSessionListener getSessionListener() {
        return sessionListener;
    }

    final ReconnectStrategy getReconnectStrategy() {
        return reconnectStrategy;
    }

    final AuthenticationHandler getAuthHandler() {
        return authHandler;
    }

    final NetconfClientConfiguration.NetconfClientProtocol getProtocol() {
        return clientProtocol;
    }

    public NetconfClientConfiguration build() {
        return new NetconfClientConfiguration(clientProtocol, address, connectionTimeoutMillis, additionalHeader, sessionListener, reconnectStrategy, authHandler);
    }
}
