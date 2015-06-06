/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListener;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

public class DefaultCloseSessionTest {

    @Test
    public void testDefaultCloseSession() throws Exception {
        AutoCloseable res = mock(AutoCloseable.class);
        doNothing().when(res).close();
        DefaultCloseSession close = new DefaultCloseSession("", res);
        Document doc = XmlUtil.newDocument();
        XmlElement elem = XmlElement.fromDomElement(XmlUtil.readXmlToElement("<elem/>"));
        final Channel channel = mock(Channel.class);
        doReturn("channel").when(channel).toString();
        doReturn(mock(ChannelFuture.class)).when(channel).close();

        final ChannelFuture sendFuture = mock(ChannelFuture.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ((GenericFutureListener) invocation.getArguments()[0]).operationComplete(sendFuture);
                return null;
            }
        }).when(sendFuture).addListener(any(GenericFutureListener.class));
        doReturn(sendFuture).when(channel).writeAndFlush(anyObject());
        final NetconfServerSessionListener listener = mock(NetconfServerSessionListener.class);
        doNothing().when(listener).onSessionTerminated(any(NetconfServerSession.class), any(NetconfTerminationReason.class));
        final NetconfServerSession session =
                new NetconfServerSession(listener, channel, 1L,
                        NetconfHelloMessageAdditionalHeader.fromString("[netconf;10.12.0.102:48528;ssh;;;;;;]"));
        close.setNetconfSession(session);
        close.handleWithNoSubsequentOperations(doc, elem);
        // Fake close response to trigger delayed close
        session.sendMessage(new NetconfMessage(XmlUtil.readXmlToDocument("<rpc-reply message-id=\"101\"\n" +
                "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<ok/>\n" +
                "</rpc-reply>")));
        verify(channel).close();
        verify(listener).onSessionTerminated(any(NetconfServerSession.class), any(NetconfTerminationReason.class));
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testDefaultCloseSession2() throws Exception {
        AutoCloseable res = mock(AutoCloseable.class);
        doThrow(NetconfDocumentedException.class).when(res).close();
        DefaultCloseSession session = new DefaultCloseSession("", res);
        Document doc = XmlUtil.newDocument();
        XmlElement elem = XmlElement.fromDomElement(XmlUtil.readXmlToElement("<elem/>"));
        session.handleWithNoSubsequentOperations(doc, elem);
    }
}
