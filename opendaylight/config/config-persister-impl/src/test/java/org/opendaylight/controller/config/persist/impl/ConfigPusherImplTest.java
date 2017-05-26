/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.facade.xml.ConfigExecution;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.osgi.YangStoreService;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.w3c.dom.Element;

public class ConfigPusherImplTest {

    @Mock
    private YangStoreService yangStoreService;
    @Mock
    private ConfigSnapshotHolder mockedConfigSnapshot;
    @Mock
    private Persister mockedAggregator;
    @Mock
    private ConfigRegistryClient configRegistryClient;
    @Mock
    private org.opendaylight.yangtools.yang.model.api.Module module;
    @Mock
    private ConfigSubsystemFacadeFactory facadeFactory;
    @Mock
    private ConfigSubsystemFacade facade;
    @Mock
    private MBeanServerConnection mBeanServer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn("content").when(yangStoreService).getModuleSource(any(ModuleIdentifier.class));
        doReturn("mocked snapshot").when(mockedConfigSnapshot).toString();
        doReturn("<mocked-snapshot/>").when(mockedConfigSnapshot).getConfigSnapshot();
        doReturn(Collections.<Module>emptySet()).when(yangStoreService).getModules();
        final Config mock = mock(Config.class);
        doReturn("mocked config").when(mock).toString();
        doReturn(facade).when(facadeFactory).createFacade(anyString());
        doReturn(Sets.newHashSet()).when(facadeFactory).getCurrentCapabilities();
        doReturn(mock).when(facade).getConfigMapping();
        doNothing().when(mBeanServer).addNotificationListener(any(ObjectName.class), any(NotificationListener.class), any(NotificationFilter.class), anyObject());
    }

    @Test
    public void testPersisterNotAllCapabilitiesProvided() throws Exception {
        doReturn(new TreeSet<>(Lists.newArrayList("required-cap"))).when(mockedConfigSnapshot).getCapabilities();

        final ConfigPusherImpl configPusher = new ConfigPusherImpl(facadeFactory, 0, 0);

        configPusher.pushConfigs(Collections.singletonList(mockedConfigSnapshot));
        try {
            configPusher.process(Lists.<AutoCloseable>newArrayList(), ManagementFactory.getPlatformMBeanServer(),
                    mockedAggregator, true);
        } catch(IllegalStateException e) {
            Throwable cause = Throwables.getRootCause(e);
            assertTrue(cause instanceof ConfigPusherImpl.NotEnoughCapabilitiesException);
            final Set<String> missingCaps = ((ConfigPusherImpl.NotEnoughCapabilitiesException) cause).getMissingCaps();
            assertEquals(missingCaps.size(), 1);
            assertEquals(missingCaps.iterator().next(), "required-cap");
            return;
        }

        fail();
    }

    @Test
    public void testPersisterSuccessfulPush() throws Exception {
        doReturn(new TreeSet<>(Lists.newArrayList("namespace?module=module&revision=2012-12-12"))).when(mockedConfigSnapshot).getCapabilities();
        final Capability cap = mock(Capability.class);
        doReturn("namespace?module=module&revision=2012-12-12").when(cap).getCapabilityUri();
        doReturn(Sets.newHashSet(cap)).when(facadeFactory).getCurrentCapabilities();
        final ConfigExecution cfgExec = mock(ConfigExecution.class);
        doReturn("cfg exec").when(cfgExec).toString();
        doReturn(cfgExec).when(facade).getConfigExecution(any(Config.class), any(Element.class));
        doNothing().when(facade).executeConfigExecution(any(ConfigExecution.class));
        doReturn(mock(CommitStatus.class)).when(facade).commitSilentTransaction();
        doReturn(Sets.newHashSet(module)).when(yangStoreService).getModules();

        final ConfigPusherImpl configPusher = new ConfigPusherImpl(facadeFactory, 0, 0);

        configPusher.pushConfigs(Collections.singletonList(mockedConfigSnapshot));
        configPusher.processSingle(Lists.<AutoCloseable>newArrayList(), mBeanServer, mockedAggregator, true);

        verify(facade).executeConfigExecution(cfgExec);
        verify(facade).commitSilentTransaction();
    }

    @Test
    public void testPersisterConflictingVersionException() throws Exception {
        doReturn(new TreeSet<>(Lists.newArrayList("namespace?module=module&revision=2012-12-12"))).when(mockedConfigSnapshot).getCapabilities();
        final Capability cap = mock(Capability.class);
        doReturn("namespace?module=module&revision=2012-12-12").when(cap).getCapabilityUri();
        doReturn(Sets.newHashSet(cap)).when(facadeFactory).getCurrentCapabilities();
        final ConfigExecution cfgExec = mock(ConfigExecution.class);
        doReturn("cfg exec").when(cfgExec).toString();
        doReturn(cfgExec).when(facade).getConfigExecution(any(Config.class), any(Element.class));
        doNothing().when(facade).executeConfigExecution(any(ConfigExecution.class));
        doThrow(ConflictingVersionException.class).when(facade).commitSilentTransaction();
        doReturn(Sets.newHashSet(module)).when(yangStoreService).getModules();

        final ConfigPusherImpl configPusher = new ConfigPusherImpl(facadeFactory, 0, 0);

        configPusher.pushConfigs(Collections.singletonList(mockedConfigSnapshot));
        try {
            configPusher.processSingle(Lists.<AutoCloseable>newArrayList(), mBeanServer, mockedAggregator, true);
        } catch (IllegalStateException e) {
            Throwable cause = Throwables.getRootCause(e);
            assertTrue(cause instanceof ConflictingVersionException);
            return;
        }

        fail();
    }

    @Test
    public void testSuccessConflictingVersionException() throws Exception {
        doReturn(new TreeSet<>(Lists.newArrayList("namespace?module=module&revision=2012-12-12"))).when(mockedConfigSnapshot).getCapabilities();
        final Capability cap = mock(Capability.class);
        doReturn("namespace?module=module&revision=2012-12-12").when(cap).getCapabilityUri();
        doReturn(Sets.newHashSet(cap)).when(facadeFactory).getCurrentCapabilities();
        final ConfigExecution cfgExec = mock(ConfigExecution.class);
        doReturn("cfg exec").when(cfgExec).toString();
        doReturn(cfgExec).when(facade).getConfigExecution(any(Config.class), any(Element.class));
        doNothing().when(facade).executeConfigExecution(any(ConfigExecution.class));

        doThrow(ConflictingVersionException.class)
        .doThrow(ConflictingVersionException.class)
        .doReturn(mock(CommitStatus.class)).when(facade).commitSilentTransaction();

        doReturn(Sets.newHashSet(module)).when(yangStoreService).getModules();

        final ConfigPusherImpl configPusher = new ConfigPusherImpl(facadeFactory, 5000, 5000);

        configPusher.pushConfigs(Collections.singletonList(mockedConfigSnapshot));
        configPusher.processSingle(Lists.<AutoCloseable>newArrayList(), mBeanServer, mockedAggregator, true);

        verify(facade, times(3)).executeConfigExecution(cfgExec);
        verify(facade, times(3)).commitSilentTransaction();
    }

}
