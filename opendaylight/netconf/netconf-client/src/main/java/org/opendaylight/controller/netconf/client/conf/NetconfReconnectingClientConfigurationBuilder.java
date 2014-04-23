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
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

public class NetconfReconnectingClientConfigurationBuilder extends NetconfClientConfigurationBuilder {

    private ReconnectStrategyFactory connectStrategyFactory;

    private NetconfReconnectingClientConfigurationBuilder() {
    }

    public static NetconfReconnectingClientConfigurationBuilder create() {
        return new NetconfReconnectingClientConfigurationBuilder();
    }


    public NetconfReconnectingClientConfigurationBuilder withConnectStrategyFactory(final ReconnectStrategyFactory connectStrategyFactory) {
        this.connectStrategyFactory = connectStrategyFactory;
        return this;
    }

    @Override
    public NetconfReconnectingClientConfiguration build() {
        return new NetconfReconnectingClientConfiguration(getProtocol(), getAddress(), getConnectionTimeoutMillis(), getAdditionalHeader(), getSessionListener(), getReconnectStrategy(), connectStrategyFactory, getAuthHandler());
    }

    // Override setter methods to return subtype

    @Override
    public NetconfReconnectingClientConfigurationBuilder withAddress(final InetSocketAddress address) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withAddress(address);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withConnectionTimeoutMillis(final long connectionTimeoutMillis) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withConnectionTimeoutMillis(connectionTimeoutMillis);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withAdditionalHeader(final NetconfHelloMessageAdditionalHeader additionalHeader) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withAdditionalHeader(additionalHeader);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withSessionListener(final NetconfClientSessionListener sessionListener) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withSessionListener(sessionListener);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withReconnectStrategy(final ReconnectStrategy reconnectStrategy) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withReconnectStrategy(reconnectStrategy);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withAuthHandler(final AuthenticationHandler authHandler) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withAuthHandler(authHandler);
    }

    @Override
    public NetconfReconnectingClientConfigurationBuilder withProtocol(NetconfClientConfiguration.NetconfClientProtocol clientProtocol) {
        return (NetconfReconnectingClientConfigurationBuilder) super.withProtocol(clientProtocol);
    }
}
