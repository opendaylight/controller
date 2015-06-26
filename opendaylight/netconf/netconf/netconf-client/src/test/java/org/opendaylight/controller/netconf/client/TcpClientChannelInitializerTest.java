/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Promise;
import org.junit.Test;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;

public class TcpClientChannelInitializerTest {
    @Test
    public void testInitializeSessionNegotiator() throws Exception {
        NetconfClientSessionNegotiatorFactory factory = mock(NetconfClientSessionNegotiatorFactory.class);
        SessionNegotiator<?> sessionNegotiator = mock(SessionNegotiator.class);
        doReturn("").when(sessionNegotiator).toString();
        doReturn(sessionNegotiator).when(factory).getSessionNegotiator(any(SessionListenerFactory.class), any(Channel.class), any(Promise.class));
        NetconfClientSessionListener listener = mock(NetconfClientSessionListener.class);
        TcpClientChannelInitializer initializer = new TcpClientChannelInitializer(factory, listener);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        doReturn(pipeline).when(pipeline).addAfter(anyString(), anyString(), any(ChannelHandler.class));
        Channel channel = mock(Channel.class);
        doReturn(pipeline).when(channel).pipeline();
        doReturn("").when(channel).toString();

        Promise<NetconfClientSession> promise = mock(Promise.class);
        doReturn("").when(promise).toString();

        initializer.initializeSessionNegotiator(channel, promise);
        verify(pipeline, times(1)).addAfter(anyString(), anyString(), any(ChannelHandler.class));
    }
}
