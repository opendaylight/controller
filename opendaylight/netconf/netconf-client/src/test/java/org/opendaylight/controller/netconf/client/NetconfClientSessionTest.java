/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfEXICodec;
import org.openexi.proc.common.EXIOptions;

public class NetconfClientSessionTest {

    @Mock
    ChannelHandler channelHandler;

    @Mock
    Channel channel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNetconfClientSession() throws Exception {
        NetconfClientSessionListener sessionListener = mock(NetconfClientSessionListener.class);
        long sessId = 20L;
        Collection<String> caps = Lists.newArrayList("cap1", "cap2");

        NetconfEXICodec codec = new NetconfEXICodec(new EXIOptions());
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        Mockito.doReturn(pipeline).when(channel).pipeline();
        Mockito.doReturn(channelHandler).when(pipeline).replace(anyString(), anyString(), any(ChannelHandler.class));
        Mockito.doReturn("").when(channelHandler).toString();

        NetconfClientSession session = new NetconfClientSession(sessionListener, channel, sessId, caps);
        session.addExiHandlers(codec);
        session.stopExiCommunication();

        assertEquals(caps, session.getServerCapabilities());
        assertEquals(session, session.thisInstance());

        Mockito.verify(pipeline, Mockito.times(4)).replace(anyString(), anyString(), Mockito.any(ChannelHandler.class));
    }
}
