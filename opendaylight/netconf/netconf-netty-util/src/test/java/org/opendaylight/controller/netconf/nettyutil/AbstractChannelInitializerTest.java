/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Promise;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfSession;

public class AbstractChannelInitializerTest {

    @Mock
    private Channel channel;
    @Mock
    private ChannelPipeline pipeline;
    @Mock
    private Promise<NetconfSession> sessionPromise;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(pipeline).when(channel).pipeline();
        doReturn(pipeline).when(pipeline).addLast(anyString(), any(ChannelHandler.class));
    }

    @Test
    public void testInit() throws Exception {
        final TestingInitializer testingInitializer = new TestingInitializer();
        testingInitializer.initialize(channel, sessionPromise);
        verify(pipeline, times(4)).addLast(anyString(), any(ChannelHandler.class));
    }

    private static final class TestingInitializer extends AbstractChannelInitializer<NetconfSession> {

        @Override
        protected void initializeSessionNegotiator(final Channel ch, final Promise<NetconfSession> promise) {
        }
    }

}