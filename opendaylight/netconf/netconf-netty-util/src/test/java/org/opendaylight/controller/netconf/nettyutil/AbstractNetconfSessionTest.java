/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.nettyutil.handler.exi.NetconfStartExiMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.openexi.proc.common.EXIOptions;

public class AbstractNetconfSessionTest {

    @Mock
    private NetconfSessionListener<NetconfSession> listener;
    @Mock
    private Channel channel;
    @Mock
    private ChannelPipeline pipeline;
    private NetconfHelloMessage clientHello;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(listener).onMessage(any(NetconfSession.class), any(NetconfMessage.class));
        doNothing().when(listener).onSessionUp(any(NetconfSession.class));
        doNothing().when(listener).onSessionDown(any(NetconfSession.class), any(Exception.class));
        doNothing().when(listener).onSessionTerminated(any(NetconfSession.class), any(NetconfTerminationReason.class));

        doReturn(mock(ChannelFuture.class)).when(channel).writeAndFlush(any(NetconfMessage.class));
        doReturn(pipeline).when(channel).pipeline();
        doReturn("mockChannel").when(channel).toString();
        doReturn(mock(ChannelFuture.class)).when(channel).close();

        doReturn(null).when(pipeline).replace(anyString(), anyString(), any(ChannelHandler.class));

        clientHello = NetconfHelloMessage.createClientHello(Collections.<String>emptySet(), Optional.<NetconfHelloMessageAdditionalHeader>absent());
    }

    @Test
    public void testHandleMessage() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        testingNetconfSession.handleMessage(clientHello);
        verify(listener).onMessage(testingNetconfSession, clientHello);
    }

    @Test
    public void testSessionUp() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        testingNetconfSession.sessionUp();
        verify(listener).onSessionUp(testingNetconfSession);
        assertEquals(1L, testingNetconfSession.getSessionId());
    }

    @Test
    public void testClose() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        testingNetconfSession.sessionUp();
        testingNetconfSession.close();
        verify(channel).close();
        verify(listener).onSessionTerminated(any(NetconfSession.class), any(NetconfTerminationReason.class));
    }

    @Test
    public void testReplaceHandlers() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        final ChannelHandler mock = mock(ChannelHandler.class);
        doReturn("handler").when(mock).toString();

        testingNetconfSession.replaceMessageDecoder(mock);
        verify(pipeline).replace(AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, mock);
        testingNetconfSession.replaceMessageEncoder(mock);
        verify(pipeline).replace(AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, mock);
        testingNetconfSession.replaceMessageEncoderAfterNextMessage(mock);
        verifyNoMoreInteractions(pipeline);

        testingNetconfSession.sendMessage(clientHello);
        verify(pipeline, times(2)).replace(AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, mock);
    }

    @Test
    public void testStartExi() throws Exception {
        TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        testingNetconfSession = spy(testingNetconfSession);

        testingNetconfSession.startExiCommunication(NetconfStartExiMessage.create(new EXIOptions(), "4"));
        verify(testingNetconfSession).addExiHandlers(any(ByteToMessageDecoder.class), any(MessageToByteEncoder.class));
    }

    @Test
    public void testEndOfInput() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        testingNetconfSession.endOfInput();
        verifyZeroInteractions(listener);
        testingNetconfSession.sessionUp();
        testingNetconfSession.endOfInput();
        verify(listener).onSessionDown(any(NetconfSession.class), any(Exception.class));
    }

    @Test
    public void testSendMessage() throws Exception {
        final TestingNetconfSession testingNetconfSession = new TestingNetconfSession(listener, channel, 1L);
        final NetconfHelloMessage clientHello = NetconfHelloMessage.createClientHello(Collections.<String>emptySet(), Optional.<NetconfHelloMessageAdditionalHeader>absent());
        testingNetconfSession.sendMessage(clientHello);
        verify(channel).writeAndFlush(clientHello);
    }

    private static class TestingNetconfSession extends AbstractNetconfSession<NetconfSession, NetconfSessionListener<NetconfSession>> {

        protected TestingNetconfSession(final NetconfSessionListener<NetconfSession> sessionListener, final Channel channel, final long sessionId) {
            super(sessionListener, channel, sessionId);
        }

        @Override
        protected NetconfSession thisInstance() {
            return this;
        }

        @Override
        protected void addExiHandlers(final ByteToMessageDecoder decoder, final MessageToByteEncoder<NetconfMessage> encoder) {}

        @Override
        public void stopExiCommunication() {}
    }
}
