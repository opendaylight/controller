/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.junit.Test;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class DefaultStopExiTest {
    @Test
    public void testHandleWithNoSubsequentOperations() throws Exception {
        DefaultStopExi exi = new DefaultStopExi("");
        Document doc = XmlUtil.newDocument();
        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        doReturn(pipeline).when(channel).pipeline();
        ChannelHandler channelHandler = mock(ChannelHandler.class);
        doReturn(channelHandler).when(pipeline).replace(anyString(), anyString(), any(ChannelHandler.class));

        NetconfServerSession serverSession = new NetconfServerSession(null, channel, 2L, null);
        exi.setNetconfSession(serverSession);

        assertNotNull(exi.handleWithNoSubsequentOperations(doc, XmlElement.fromDomElement(XmlUtil.readXmlToElement("<elem/>"))));
        verify(pipeline, times(1)).replace(anyString(), anyString(), any(ChannelHandler.class));
    }
}
