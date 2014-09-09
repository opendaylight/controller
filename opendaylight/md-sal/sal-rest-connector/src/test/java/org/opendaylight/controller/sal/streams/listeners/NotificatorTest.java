/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.streams.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.AsyncEventBus;
import io.netty.channel.Channel;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NotificatorTest {

    private static URI NS;
    private static Date DATE;

    private static final String STREAM_NAME1 = "listener path1";
    private static final String STREAM_NAME2 = "listener path2";
    private static final String STREAM_NAME3 = "listener path3";
    private static final String STREAM_NAME_WRONG = "listener path wrong";

    private static YangInstanceIdentifier PATH1;
    private static YangInstanceIdentifier PATH2;
    private static YangInstanceIdentifier PATH3;

    private ListenerAdapter listener;

    @BeforeClass
    public static void initialization() {

        try {
            NS = new URI("ns");
            DATE = new SimpleDateFormat("YYYY-MM-dd").parse("2014-07-23");
            PATH1 = YangInstanceIdentifier.builder().node(QName.create(NS, DATE, STREAM_NAME1)).toInstance();
            PATH2 = YangInstanceIdentifier.builder().node(QName.create(NS, DATE, STREAM_NAME2)).toInstance();
            PATH3 = YangInstanceIdentifier.builder().node(QName.create(NS, DATE, STREAM_NAME3)).toInstance();

        } catch (URISyntaxException | ParseException e) {
            fail("Exception occured in " + NotificatorTest.class + " during test initialization. " + e.getMessage());
        }

    }

    /**
     * creating listener with instance of CurrentThreadExecutor should ensure that operation (like posting events to
     * event bus in listener) are executed sequentially and not in several threads
     */
    @Before
    public void initializationBeforeTest() {
        Notificator.removeAllListeners();
        listener = Notificator.createListener(PATH1, STREAM_NAME1, new AsyncEventBus(new CurrentThreadExecutor()));
        Notificator.createListener(PATH2, STREAM_NAME2, new AsyncEventBus(new CurrentThreadExecutor()));
        Notificator.createListener(PATH3, STREAM_NAME3, new AsyncEventBus(new CurrentThreadExecutor()));
    }

    @Test
    public void getExistingListenerTest() {
        assertEquals("Listener for " + STREAM_NAME1 + " should be present", listener,
                Notificator.getListenerFor(STREAM_NAME1));
    }

    @Test
    public void getNotExistingListenerTest() {
        assertNull("Listener for stream " + STREAM_NAME_WRONG + " shouldn't exist.",
                Notificator.getListenerFor(STREAM_NAME_WRONG));
    }

    @Test
    public void checkListenerExistenceTest() {
        assertTrue("Listener for " + STREAM_NAME1 + " wasn't found.", Notificator.existListenerFor(STREAM_NAME1));
    }

    @Test
    public void notificatorStreamNamesTest() {
        final Set<String> expectedStreamNames = new HashSet<>();
        expectedStreamNames.add(STREAM_NAME1);
        expectedStreamNames.add(STREAM_NAME2);
        expectedStreamNames.add(STREAM_NAME3);

        final Set<String> actualStreamNames = Notificator.getStreamNames();
        assertEquals(expectedStreamNames, actualStreamNames);
    }

    @Test
    public void removeExistingListenerTest() {
        assertNotNull(Notificator.getListenerFor(STREAM_NAME1));
        Notificator.removeListenerIfNoSubscriberExists(listener);
        assertNull(Notificator.getListenerFor(STREAM_NAME1));
    }

    @Test
    public void deleteNotExistingListenerTest() {
        assertNull(Notificator.getListenerFor(STREAM_NAME_WRONG));
        Notificator.removeListenerIfNoSubscriberExists(listener);
        assertNull(Notificator.getListenerFor(STREAM_NAME_WRONG));
    }

    @Test
    public void streamNameFromUriTest() {
        String streamName = Notificator.createStreamNameFromUri("/foo/");
        assertEquals("foo", streamName);
        streamName = Notificator.createStreamNameFromUri("/foo");
        assertEquals("foo", streamName);
        streamName = Notificator.createStreamNameFromUri("foo/");
        assertEquals("foo", streamName);
        streamName = Notificator.createStreamNameFromUri("/");
        assertEquals("", streamName);
        streamName = Notificator.createStreamNameFromUri("//");
        assertEquals("", streamName);
        streamName = Notificator.createStreamNameFromUri("///");
        assertEquals("/", streamName);
        streamName = Notificator.createStreamNameFromUri(null);
        assertNull(streamName);
    }

    @Test
    public void removeExistingListenerWithoutSubscriberTest() {
        ListenerAdapter listener = Notificator.getListenerFor(STREAM_NAME3);
        assertNotNull(listener);
        final Channel mockedChannel = mock(Channel.class);
        when(mockedChannel.isActive()).thenReturn(true);

        listener.addSubscriber(mockedChannel);

        Notificator.removeListenerIfNoSubscriberExists(listener);
        // wasn't removed
        assertNotNull(Notificator.getListenerFor(STREAM_NAME3));

        listener.removeSubscriber(mockedChannel);

        Notificator.removeListenerIfNoSubscriberExists(listener);
        assertNull(Notificator.getListenerFor(STREAM_NAME3));
    }

}
