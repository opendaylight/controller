/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.util;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Before;
import org.junit.Test;

public class DeserializerExceptionHandlerTest {

    private DeserializerExceptionHandler handler;
    private ChannelFuture channelFuture;
    private ChannelHandlerContext context;
    private Channel channel;

    @Before
    public void setUp() throws Exception {
        handler = new DeserializerExceptionHandler();
        context = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        doReturn(channel).when(context).channel();
        channelFuture = mock(ChannelFuture.class);
        doReturn(channelFuture).when(channelFuture).addListener(any(GenericFutureListener.class));
        doReturn(channelFuture).when(channel).writeAndFlush(anyObject());
    }

    @Test
    public void testExceptionCaught() throws Exception {
        handler.exceptionCaught(context, new Exception());
        verify(context, times(1)).channel();
    }
}
