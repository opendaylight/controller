/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client.conf;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;

public class NetconfClientConfiguration {

    private final NetconfClientProtocol clientProtocol;
    private final InetSocketAddress address;
    private final Long connectionTimeoutMillis;

    private final NetconfHelloMessageAdditionalHeader additionalHeader;
    private final NetconfClientSessionListener sessionListener;

    private final ReconnectStrategy reconnectStrategy;

    private final AuthenticationHandler authHandler;

    NetconfClientConfiguration(final NetconfClientProtocol protocol, final InetSocketAddress address, final Long connectionTimeoutMillis, final NetconfHelloMessageAdditionalHeader additionalHeader, final NetconfClientSessionListener sessionListener, final ReconnectStrategy reconnectStrategy, final AuthenticationHandler authHandler) {
        this.address = address;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.additionalHeader = additionalHeader;
        this.sessionListener = sessionListener;
        this.clientProtocol = protocol;
        this.reconnectStrategy = reconnectStrategy;
        this.authHandler = authHandler;
        validateConfiguration();
    }

    public final InetSocketAddress getAddress() {
        return address;
    }

    public final Long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public final Optional<NetconfHelloMessageAdditionalHeader> getAdditionalHeader() {
        return Optional.fromNullable(additionalHeader);
    }

    public final NetconfClientSessionListener getSessionListener() {
        return sessionListener;
    }

    public final ReconnectStrategy getReconnectStrategy() {
        return reconnectStrategy;
    }

    public final AuthenticationHandler getAuthHandler() {
        return authHandler;
    }

    public NetconfClientProtocol getProtocol() {
        return clientProtocol;
    }

    private void validateConfiguration() {
        Preconditions.checkNotNull(clientProtocol, " ");
        switch (clientProtocol) {
        case SSH:
            validateSshConfiguration();
            // Fall through intentional (ssh validation is a superset of tcp validation)
        case TCP:
            validateTcpConfiguration();
        }
    }

    protected void validateSshConfiguration() {
        Preconditions.checkNotNull(authHandler, "authHandler");
    }

    protected void validateTcpConfiguration() {
        Preconditions.checkNotNull(address, "address");
        Preconditions.checkNotNull(clientProtocol, "clientProtocol");
        Preconditions.checkNotNull(connectionTimeoutMillis, "connectionTimeoutMillis");
        Preconditions.checkNotNull(sessionListener, "sessionListener");
        Preconditions.checkNotNull(reconnectStrategy, "reconnectStrategy");
    }

    @Override
    public final String toString() {
        return buildToStringHelper().toString();
    }

    protected ToStringHelper buildToStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("address", address)
                .add("connectionTimeoutMillis", connectionTimeoutMillis)
                .add("additionalHeader", additionalHeader)
                .add("sessionListener", sessionListener)
                .add("reconnectStrategy", reconnectStrategy)
                .add("clientProtocol", clientProtocol)
                .add("authHandler", authHandler);
    }

    public static enum NetconfClientProtocol {
        TCP, SSH
    }
}