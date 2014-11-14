/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.w3c.dom.Document;

public class SendErrorExceptionUtilTest {

    NetconfSession netconfSession;
    ChannelFuture channelFuture;
    Channel channel;
    private NetconfDocumentedException exception;

    @Before
    public void setUp() throws Exception {
        netconfSession = mock(NetconfSession.class);
        channelFuture = mock(ChannelFuture.class);
        channel = mock(Channel.class);
        doReturn(channelFuture).when(netconfSession).sendMessage(any(NetconfMessage.class));
        doReturn(channelFuture).when(channelFuture).addListener(any(GenericFutureListener.class));
        doReturn(channelFuture).when(channel).writeAndFlush(any(NetconfMessage.class));
        exception = new NetconfDocumentedException("err");
    }

    @Test
    public void testSendErrorMessage1() throws Exception {
        SendErrorExceptionUtil.sendErrorMessage(netconfSession, exception);
        verify(channelFuture, times(1)).addListener(any(GenericFutureListener.class));
        verify(netconfSession, times(1)).sendMessage(any(NetconfMessage.class));
    }

    @Test
    public void testSendErrorMessage2() throws Exception {
        SendErrorExceptionUtil.sendErrorMessage(channel, exception);
        verify(channelFuture, times(1)).addListener(any(GenericFutureListener.class));
    }

    @Test
    public void testSendErrorMessage3() throws Exception {
        Document helloMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/rpc.xml");
        SendErrorExceptionUtil.sendErrorMessage(netconfSession, exception, new NetconfMessage(helloMessage));
        verify(channelFuture, times(1)).addListener(any(GenericFutureListener.class));
    }
}
