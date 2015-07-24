/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Collections;
import javax.management.Notification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.jmx.notifications.CommitJMXNotification;
import org.opendaylight.controller.config.api.jmx.notifications.ConfigJMXNotification;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.facade.xml.Datastore;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;

public class ConfigPersisterNotificationListenerTest {

    @Mock
    private Persister mockPersister;
    private PersisterAggregator persisterAggregator;

    @Mock
    private ConfigJMXNotification unknownNetconfNotif;
    @Mock
    private CommitJMXNotification commitNetconfNotif;
    @Mock
    private Notification unknownNotif;
    @Mock
    private ConfigSubsystemFacadeFactory facadeFactory;
    @Mock
    private ConfigSubsystemFacade facade;
    @Mock
    private ConfigRegistryClient configRegistryClient;
    @Mock
    private Capability cap;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.doNothing().when(mockPersister).persistConfig(any(ConfigSnapshotHolder.class));
        doReturn("persister").when(mockPersister).toString();
        final PersisterAggregator.PersisterWithConfiguration withCfg = new PersisterAggregator.PersisterWithConfiguration(mockPersister, false);
        persisterAggregator = new PersisterAggregator(Lists.newArrayList(withCfg));

        doReturn("netconfUnknownNotification").when(unknownNetconfNotif).toString();
        doReturn("netconfCommitNotification").when(commitNetconfNotif).toString();

        doReturn("config client").when(configRegistryClient).toString();

        doReturn("cap").when(cap).getCapabilityUri();
        doReturn(facade).when(facadeFactory).createFacade(anyString());

        doReturn(Collections.singleton(cap)).when(facadeFactory).getCurrentCapabilities();
        doReturn(XmlUtil.readXmlToElement("<snapshot/>")).when(facade)
                .getConfiguration(any(Document.class), any(Datastore.class), any(Optional.class));
    }

    @Test
    public void testNotificationListenerUnknownNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator, facadeFactory);
        testeListener.handleNotification(unknownNotif, null);
        Mockito.verifyZeroInteractions(mockPersister);
    }

    @Test
    public void testNotificationListenerUnknownNetconfNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator, facadeFactory);
        try {
            testeListener.handleNotification(unknownNetconfNotif, null);
            Assert.fail("Unknown netconf notification should fail");
        } catch (final IllegalStateException e) {
            Mockito.verifyZeroInteractions(mockPersister);
        }
    }

    @Test
    public void testNotificationListenerCommitNetconfNotification() throws Exception {
        final ConfigPersisterNotificationListener testeListener = new ConfigPersisterNotificationListener(persisterAggregator, facadeFactory);
        testeListener.handleNotification(commitNetconfNotif, null);
        Mockito.verify(mockPersister).persistConfig(any(ConfigSnapshotHolder.class));
    }
}
