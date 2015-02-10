/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications.impl.ops;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationListener;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.controller.netconf.notifications.NotificationListenerRegistration;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.w3c.dom.Element;

public class CreateSubscriptionTest {

    private static final String CREATE_SUBSCRIPTION_XML = "<create-subscription\n" +
            "xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\" xmlns:netconf=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "<stream>TESTSTREAM</stream>" +
            "</create-subscription>";

    @Mock
    private NetconfNotificationRegistry notificationRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(true).when(notificationRegistry).isStreamAvailable(any(StreamNameType.class));
        doReturn(mock(NotificationListenerRegistration.class)).when(notificationRegistry).registerNotificationListener(any(StreamNameType.class), any(NetconfNotificationListener.class));
    }

    @Test
    public void testHandleWithNoSubsequentOperations() throws Exception {
        final CreateSubscription createSubscription = new CreateSubscription("id", notificationRegistry);
        createSubscription.setSession(mock(NetconfSession.class));

        final Element e = XmlUtil.readXmlToElement(CREATE_SUBSCRIPTION_XML);

        final XmlElement operationElement = XmlElement.fromDomElement(e);
        final Element element = createSubscription.handleWithNoSubsequentOperations(XmlUtil.newDocument(), operationElement);

        Assert.assertThat(XmlUtil.toString(element), CoreMatchers.containsString("ok"));
    }
}