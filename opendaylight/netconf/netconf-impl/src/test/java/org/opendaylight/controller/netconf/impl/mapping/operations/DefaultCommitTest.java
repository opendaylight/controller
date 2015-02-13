/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCommitTest {

    private NetconfOperationChainedExecution operation;
    private Document requestMessage;
    private NetconfOperationRouter router;
    private DefaultCommitNotificationProducer notifier;
    private NetconfMonitoringService cap;
    private DefaultCommit commit;

    @Before
    public void setUp() throws Exception {
        operation = mock(NetconfOperationChainedExecution.class);
        doReturn(XmlUtil.newDocument()).when(operation).execute(any(Document.class));
        router = mock(NetconfOperationRouter.class);
        doReturn(false).when(operation).isExecutionTermination();
        notifier = mock(DefaultCommitNotificationProducer.class);
        doNothing().when(notifier).sendCommitNotification(anyString(), any(Element.class), anySetOf(String.class));
        cap = mock(NetconfMonitoringService.class);
        doReturn(Sets.newHashSet()).when(cap).getCapabilities();
        Document rpcData = XmlFileLoader.xmlFileToDocument("netconfMessages/editConfig_expectedResult.xml");
        doReturn(rpcData).when(router).onNetconfMessage(any(Document.class), any(NetconfServerSession.class));
        commit = new DefaultCommit(notifier, cap, "", router);
    }

    @Test
    public void testHandleWithNotification() throws Exception {
        requestMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/commit.xml");
        commit.handle(requestMessage, operation);
        verify(operation, times(1)).execute(requestMessage);
        verify(notifier, times(1)).sendCommitNotification(anyString(), any(Element.class), anySetOf(String.class));
    }

    @Test
    public void testHandleWithoutNotification() throws Exception {
        requestMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/commit.xml");
        Element elem = requestMessage.getDocumentElement();
        elem.setAttribute("notify", "false");
        commit.handle(requestMessage, operation);
        verify(operation, times(1)).execute(requestMessage);
        verify(notifier, never()).sendCommitNotification(anyString(), any(Element.class), anySetOf(String.class));
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testHandle() throws Exception {
        Document rpcData = XmlFileLoader.xmlFileToDocument("netconfMessages/get.xml");
        doReturn(rpcData).when(router).onNetconfMessage(any(Document.class), any(NetconfServerSession.class));
        requestMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/commit.xml");
        commit.handle(requestMessage, operation);
    }
}
