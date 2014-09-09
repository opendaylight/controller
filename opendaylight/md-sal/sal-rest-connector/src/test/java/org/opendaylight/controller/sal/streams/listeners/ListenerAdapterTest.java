/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.streams.listeners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.AsyncEventBus;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ListenerAdapterTest {

    private static URI NS;
    private static Date DATE;
    private static final String PATH_NAME1 = "listener path1";
    private static final String STREAM_NAME1 = "listener path1";
    private static YangInstanceIdentifier PATH1;
    private static ListenerAdapter listener;
    private static AnswerImpl<?> answer;

    @BeforeClass
    public static void initialization() {
        try {
            NS = new URI("ns:listener:adapter");
            DATE = new SimpleDateFormat("yyyy-MM-dd").parse("2014-07-25");
        } catch (URISyntaxException e) {
            fail("URI for wasn't created.");
        } catch (ParseException e) {
            fail("Date for wasn't created.");
        }

        PATH1 = YangInstanceIdentifier.builder().node(QName.create(NS, DATE, PATH_NAME1)).toInstance();
        Notificator.removeAllListeners();
        try {
            ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext("/listener-adapter"));
        } catch (FileNotFoundException e) {
            fail("Schema context with listener-adapter-test.yang module wasn't loaded.");
        }
        listener = Notificator.createListener(PATH1, STREAM_NAME1, new AsyncEventBus(new CurrentThreadExecutor()));

        final Channel mockedChannel = mock(Channel.class);
        when(mockedChannel.isActive()).thenReturn(true);
        answer = new ListenerAdapterTest.AnswerImpl<ChannelFuture>();
        when(mockedChannel.writeAndFlush(any(TextWebSocketFrame.class))).thenAnswer(answer);
        listener.addSubscriber(mockedChannel);

    }

    // 2.2.4. event notificatin - http://tools.ietf.org/html/draft-bierman-netconf-restconf-02
    // model: sal-remote
    // ns: "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote"
    // notification:
    // notification data-changed-notification {
    // description "Data change notification.";
    // list data-change-event {
    // key path;
    // leaf path {
    // type instance-identifier;
    // }
    // leaf store {
    // type enumeration {
    // enum config;
    // enum operation;
    // }
    // }
    // leaf operation {
    // type enumeration {
    // enum created;
    // enum updated;
    // enum deleted;
    // }
    // }
    // anyxml data{
    // description "DataObject ";
    // }
    // }
    // }

    @Test
    public void dataRemoveNotificationTest() {
        final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockedChange = mock(AsyncDataChangeEvent.class);
        when(mockedChange.getRemovedPaths())
                .thenReturn(Collections.singleton(provideInstanceIdentifier("cont", "lf5")));
        listener.onDataChanged(mockedChange);
        final TextWebSocketFrame notification = answer.getTextWebSocketFrame();
        assertNotNull(notification);
        Document xmlDocument = toDocument(notification);
        verifyNotification(xmlDocument, false);
    }

    private void verifyNotification(Document xmlDocument, boolean b) {
        Element namespaceRootElement = xmlDocument.getDocumentElement();
        boolean namespaceRootElementOk = false;

        if (namespaceRootElement.getNodeName().equals("notification")) {
            if (namespaceRootElement.getAttribute("xmlns").equals("urn:ietf:params:xml:ns:netconf:notification:1.0")) {
                namespaceRootElementOk = true;
            }
        }
        assertTrue(namespaceRootElementOk);
        // verifyEventTime(namespaceRootElement);
        verifyDataChangedNotification(namespaceRootElement);
    }

    private void verifyDataChangedNotification(Element namespaceRootElement) {

    }

    // http://tools.ietf.org/html/rfc5277#page-9 2.2.1

    public boolean verifyInternetDateTimeFormat(final String dateTime) {
        try {
            Date parse = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateTime);

            String dateRegex = "^[\\d]{4}-[\\d]{2}-[\\d]{2}";
            // String timeRegex = "[\\d]{2}:[\\d]{2}:[\\d]{2}";

            String timeHoursRegex = "([0-1][0-9]|[2][0-3])";
            String timeMinutesRegex = "([0-5][0-9])";
            String timeSecondsRegex = timeMinutesRegex;
            String timeMilisecondsRegex = "(|[.][0-9]{1,})";
            String timeRegex = timeHoursRegex + ":" + timeMinutesRegex + ":" + timeSecondsRegex + timeMilisecondsRegex;

            String timeZoneRegex = "([Z]|([+|-]([0][0-9]|[1][0-2]):[0][0]))$";

            String fullRegex = dateRegex + "[T]" + timeRegex + timeZoneRegex;
            return dateTime.matches(fullRegex);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    @Ignore
    @Test
    public void dateVerification() {
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31+00:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31-01:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31Z"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31+12:00"));
        System.out.println(verifyInternetDateTimeFormat("2013-02-29T09:27:31.343-01:00"));

        System.out.println(verifyInternetDateTimeFormat("+03:"));
        System.out.println(verifyInternetDateTimeFormat("+02:00"));
        System.out.println(verifyInternetDateTimeFormat("-02:01"));
        System.out.println(verifyInternetDateTimeFormat("2013-02-29T09:27:31,343-01:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31+Z"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31+13:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31-13:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31-01:14"));
        System.out.println(verifyInternetDateTimeFormat("2014-07-28T09:27:31-1:14"));
        System.out.println(verifyInternetDateTimeFormat("2014-7-28T09:27:31-01:00"));
        System.out.println(verifyInternetDateTimeFormat("2014-06-31T09:27:31-01:00"));
        System.out.println(verifyInternetDateTimeFormat("2013-02-29T09:65:31-01:00"));
    }

    @Test
    public void dataUpdateNotificationTest() {
        final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockedChange = mock(AsyncDataChangeEvent.class);
        when(mockedChange.getUpdatedData()).thenReturn(provideMap("cont", "lf3"));
        when(mockedChange.getCreatedData()).thenReturn(
                Collections.<YangInstanceIdentifier, NormalizedNode<?, ?>> emptyMap());

        listener.onDataChanged(mockedChange);
        final TextWebSocketFrame notification = answer.getTextWebSocketFrame();
        assertNotNull(notification);
        Document xmlDocument = toDocument(notification);
        verifyNotification(xmlDocument, true);
    }

    @Test
    public void dataCreateNotificationTest() {
        final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockedChange = mock(AsyncDataChangeEvent.class);
        when(mockedChange.getCreatedData()).thenReturn(provideMap("cont", "lf1"));

        listener.onDataChanged(mockedChange);
        final TextWebSocketFrame notification = answer.getTextWebSocketFrame();
        assertNotNull(notification);
        Document xmlDocument = toDocument(notification);
        verifyNotification(xmlDocument, true);
    }

    private Document toDocument(final TextWebSocketFrame notification) {
        try {
            return loadXMLFrom(notification.text());
        } catch (SAXException | IOException | URISyntaxException e) {
            fail("Conversion of notification to xml Document format wasn't successful");
        }
        return null;
    }

    private NormalizedNode<?, ?> provideNormalizedNode(final String... names) {
        if (names.length > 1) {
            DataContainerChild<?, ?> node = Builders.leafBuilder()
                    .withNodeIdentifier(new NodeIdentifier(QName.create(NS, DATE, names[names.length - 1])))
                    .withValue(names[names.length - 1] + " value").build();
            for (int i = names.length - 1; i >= 0; i--) {
                node = Builders.containerBuilder()
                        .withNodeIdentifier(new NodeIdentifier(QName.create(NS, DATE, names[i]))).withChild(node)
                        .build();

            }
            return node;
        }
        throw new IllegalArgumentException(names + " contains less then 2 elements");
    }

    private YangInstanceIdentifier provideInstanceIdentifier(String... iiNames) {
        InstanceIdentifierBuilder iiBuilder = YangInstanceIdentifier.builder();
        for (int i = 0; i < iiNames.length - 1; i++) {
            iiBuilder.node(QName.create(NS, DATE, iiNames[i]));
        }
        return iiBuilder.toInstance();
    }

    private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> provideMap(String... names) {
        return Collections.<YangInstanceIdentifier, NormalizedNode<?, ?>> singletonMap(
                provideInstanceIdentifier(names), provideNormalizedNode(names));
    }

    private Document loadXMLFrom(String xmlString) throws SAXException, IOException, URISyntaxException {
        // xmlString = TestUtils.loadTextFile("/listener-adapter/xml/xml.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
        }
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }

    private static class AnswerImpl<T> implements Answer<T> {
        TextWebSocketFrame textWebSocketFrame;

        @Override
        public T answer(InvocationOnMock invocation) throws Throwable {
            textWebSocketFrame = (TextWebSocketFrame) invocation.getArguments()[0];
            return null;
        }

        public TextWebSocketFrame getTextWebSocketFrame() {
            return textWebSocketFrame;
        }

    }
}
