/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.collect.Lists;
import java.util.Collections;
import javax.management.Notification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.api.jmx.NetconfJMXNotification;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

public class ConfigPersisterNotificationListenerTest {

    @Mock
    private Persister mockPersister;
    private PersisterAggregator persisterAggregator;

    @Mock
    private NetconfJMXNotification unknownNetconfNotif;
    @Mock
    private CommitJMXNotification commitNetconfNotif;
    @Mock
    private Notification unknownNotif;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.doNothing().when(mockPersister).persistConfig(Matchers.any(ConfigSnapshotHolder.class));
        Mockito.doReturn("persister").when(mockPersister).toString();
        final PersisterAggregator.PersisterWithConfiguration withCfg = new PersisterAggregator.PersisterWithConfiguration(mockPersister, false);
        persisterAggregator = new PersisterAggregator(Lists.newArrayList(withCfg));

        Mockito.doReturn("netconfUnknownNotification").when(unknownNetconfNotif).toString();
        Mockito.doReturn("netconfCommitNotification").when(commitNetconfNotif).toString();

        Mockito.doReturn(XmlUtil.readXmlToElement("<config-snapshot/>")).when(commitNetconfNotif).getConfigSnapshot();
        Mockito.doReturn(Collections.emptySet()).when(commitNetconfNotif).getCapabilities();

    }

    @Test
    public void testNotificationListenerUnknownNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator);
        testeListener.handleNotification(unknownNotif, null);
        Mockito.verifyZeroInteractions(mockPersister);
    }

    @Test
    public void testNotificationListenerUnknownNetconfNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator);
        try {
            testeListener.handleNotification(unknownNetconfNotif, null);
            Assert.fail("Unknown netconf notification should fail");
        } catch (final IllegalStateException e) {
            Mockito.verifyZeroInteractions(mockPersister);
        }
    }

    @Test
    public void testNotificationListenerCommitNetconfNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator);
        testeListener.handleNotification(commitNetconfNotif, null);
        Mockito.verify(mockPersister).persistConfig(Matchers.any(ConfigSnapshotHolder.class));
    }
}
