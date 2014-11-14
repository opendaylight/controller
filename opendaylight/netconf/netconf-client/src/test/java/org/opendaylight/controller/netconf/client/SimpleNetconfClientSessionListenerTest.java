/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;

public class SimpleNetconfClientSessionListenerTest {

    private Channel channel;
    private ChannelFuture channelFuture;
    Set<String> caps;
    private NetconfHelloMessage helloMessage;
    private NetconfMessage message;
    private NetconfClientSessionListener sessionListener;
    private NetconfClientSession clientSession;

    @Before
    public void setUp() throws Exception {
        channel = mock(Channel.class);
        channelFuture = mock(ChannelFuture.class);
        doReturn(channelFuture).when(channel).writeAndFlush(anyObject());
        caps = Sets.newSet("a", "b");
        helloMessage = NetconfHelloMessage.createServerHello(caps, 10);
        message = new NetconfMessage(helloMessage.getDocument());
        sessionListener = mock(NetconfClientSessionListener.class);
        clientSession = new NetconfClientSession(sessionListener, channel, 20L, caps);
    }

    @Test
    public void testSessionDown() throws Exception {
        SimpleNetconfClientSessionListener simpleListener = new SimpleNetconfClientSessionListener();
        Future<NetconfMessage> promise = simpleListener.sendRequest(message);
        simpleListener.onSessionUp(clientSession);
        verify(channel, times(1)).writeAndFlush(anyObject());

        simpleListener.onSessionDown(clientSession, new Exception());
        assertFalse(promise.isSuccess());
    }

    @Test
    public void testSendRequest() throws Exception {
        SimpleNetconfClientSessionListener simpleListener = new SimpleNetconfClientSessionListener();
        Future<NetconfMessage> promise = simpleListener.sendRequest(message);
        simpleListener.onSessionUp(clientSession);
        verify(channel, times(1)).writeAndFlush(anyObject());

        simpleListener.sendRequest(message);
        assertFalse(promise.isSuccess());
    }

    @Test
    public void testOnMessage() throws Exception {
        SimpleNetconfClientSessionListener simpleListener = new SimpleNetconfClientSessionListener();
        Future<NetconfMessage> promise = simpleListener.sendRequest(message);
        simpleListener.onSessionUp(clientSession);
        verify(channel, times(1)).writeAndFlush(anyObject());

        simpleListener.onMessage(clientSession, message);
        assertTrue(promise.isSuccess());
    }
}
