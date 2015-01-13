/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client.conf;

import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import org.apache.mina.core.session.IoSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

public class NetconfReversedClientConfiguration extends NetconfClientConfiguration {

    private final IoSession tcpSession;

    public NetconfReversedClientConfiguration(final Long connectionTimeoutMillis, final NetconfHelloMessageAdditionalHeader additionalHeader, final NetconfClientSessionListener sessionListener, final AuthenticationHandler authHandler, final IoSession tcpSession) {
        // FIXME the address has to be unused
        super(NetconfClientProtocol.SSH, InetSocketAddress.createUnresolved("localhost", 830), connectionTimeoutMillis, additionalHeader, sessionListener, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 10000), authHandler);
        this.tcpSession = tcpSession;
    }

    public IoSession getTcpSession() {
        return tcpSession;
    }
}