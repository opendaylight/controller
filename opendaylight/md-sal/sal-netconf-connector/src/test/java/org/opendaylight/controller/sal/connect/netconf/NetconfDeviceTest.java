/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

import io.netty.util.concurrent.GlobalEventExecutor;

public class NetconfDeviceTest {

    public static final NetconfClientConfiguration CONFIGURATION = NetconfClientConfigurationBuilder.create()
            .withAddress(InetSocketAddress.createUnresolved("localhost", 1111))
            .withConnectionTimeoutMillis(1000)
            .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.TCP)
            .withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 1000))
            .withSessionListener(new SimpleNetconfClientSessionListener()).build();

    @Test
    public void testNetconfDevice() throws Exception {


    }


}
