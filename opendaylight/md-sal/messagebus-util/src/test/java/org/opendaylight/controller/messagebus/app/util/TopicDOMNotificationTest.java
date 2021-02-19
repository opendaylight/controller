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
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@Deprecated(forRemoval = true)
public class TopicDOMNotificationTest {

    private static final String CONTAINER_NODE_BODY_MOCK_TO_STRING = "containerNodeBodyMock";
    ContainerNode containerNodeBodyMock;
    TopicDOMNotification topicDOMNotification;

    @BeforeClass
    public static void initTestClass() {
    }

    @Before
    public void setUp() {
        containerNodeBodyMock = mock(ContainerNode.class);
        doReturn(CONTAINER_NODE_BODY_MOCK_TO_STRING).when(containerNodeBodyMock).toString();
        topicDOMNotification = new TopicDOMNotification(containerNodeBodyMock);
    }

    @Test
    public void constructorTest() {
        assertNotNull("Instance has not been created correctly.", topicDOMNotification);
    }

    @Test
    public void getTypeTest() {
        assertEquals("Type has not been created correctly.", Absolute.of(TopicNotification.QNAME),
            topicDOMNotification.getType());
    }

    @Test
    public void getBodyTest() {
        assertEquals("String has not been created correctly.", containerNodeBodyMock, topicDOMNotification.getBody());
    }

    @Test
    public void getToStringTest() {
        String bodyString = "TopicDOMNotification [body=" + CONTAINER_NODE_BODY_MOCK_TO_STRING + "]";
        assertEquals("String has not been created correctly.", bodyString, topicDOMNotification.toString());
    }
}
