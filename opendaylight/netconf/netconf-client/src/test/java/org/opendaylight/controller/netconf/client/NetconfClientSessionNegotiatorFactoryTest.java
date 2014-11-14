/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;

public class NetconfClientSessionNegotiatorFactoryTest {
    @Test
    public void testGetSessionNegotiator() throws Exception {
        NetconfClientSessionListener sessionListener = mock(NetconfClientSessionListener.class);
        Timer timer = new HashedWheelTimer();
        SessionListenerFactory<NetconfClientSessionListener> listenerFactory = mock(SessionListenerFactory.class);
        doReturn(sessionListener).when(listenerFactory).getSessionListener();

        Channel channel = mock(Channel.class);
        Promise<NetconfClientSession> promise = mock(Promise.class);
        NetconfClientSessionNegotiatorFactory negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer,
                Optional.<NetconfHelloMessageAdditionalHeader>absent(), 200L);

        SessionNegotiator<?> sessionNegotiator = negotiatorFactory.getSessionNegotiator(listenerFactory, channel, promise);
        assertNotNull(sessionNegotiator);
    }
}
