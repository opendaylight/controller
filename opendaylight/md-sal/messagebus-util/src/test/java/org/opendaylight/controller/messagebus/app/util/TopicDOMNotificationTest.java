/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class TopicDOMNotificationTest {

    private static final String containerNodeBodyMockToString = "containerNodeBodyMock";
    ContainerNode containerNodeBodyMock;
    TopicDOMNotification topicDOMNotification;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        containerNodeBodyMock = mock(ContainerNode.class);
        doReturn(containerNodeBodyMockToString).when(containerNodeBodyMock).toString();
        topicDOMNotification = new TopicDOMNotification(containerNodeBodyMock);
    }

    @Test
    public void constructorTest() {
        assertNotNull("Instance has not been created correctly.", topicDOMNotification);
    }

    @Test
    public void getTypeTest() {
        SchemaPath TOPIC_NOTIFICATION_ID = SchemaPath.create(true, TopicNotification.QNAME);
        assertEquals("Type has not been created correctly.", TOPIC_NOTIFICATION_ID, topicDOMNotification.getType());
    }

    @Test
    public void getBodyTest() {
        assertEquals("String has not been created correctly.", containerNodeBodyMock, topicDOMNotification.getBody());
    }

    @Test
    public void getToStringTest() {
        String bodyString = "TopicDOMNotification [body=" + containerNodeBodyMockToString + "]";
        assertEquals("String has not been created correctly.", bodyString, topicDOMNotification.toString());
    }
}