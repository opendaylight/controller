/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;

public class NetconfMonitoringServiceImplTest {

    private NetconfMonitoringServiceImpl service;

    @Mock
    private NetconfOperationProvider operationProvider;
    @Mock
    private NetconfManagementSession managementSession;
    @Mock
    private NetconfOperationServiceSnapshot snapshot;
    @Mock
    private NetconfOperationService operationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = new NetconfMonitoringServiceImpl(operationProvider);
    }

    @Test
    public void testSessions() throws Exception {
        doReturn("sessToStr").when(managementSession).toString();
        service.onSessionUp(managementSession);
    }

    @Test(expected = RuntimeException.class)
    public void testGetSchemas() throws Exception {
        doThrow(RuntimeException.class).when(operationProvider).openSnapshot(anyString());
        service.getSchemas();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSchemas2() throws Exception {
        doThrow(Exception.class).when(operationProvider).openSnapshot(anyString());
        service.getSchemas();
    }

    @Test
    public void testGetSchemas3() throws Exception {
        doReturn("").when(managementSession).toString();
        Capability cap = mock(Capability.class);
        Set<Capability> caps = Sets.newHashSet(cap);
        Set<NetconfOperationService> services = Sets.newHashSet(operationService);
        doReturn(snapshot).when(operationProvider).openSnapshot(anyString());
        doReturn(services).when(snapshot).getServices();
        doReturn(caps).when(operationService).getCapabilities();
        Optional<String> opt = mock(Optional.class);
        doReturn(opt).when(cap).getCapabilitySchema();
        doReturn(true).when(opt).isPresent();
        doReturn(opt).when(cap).getModuleNamespace();
        doReturn("namespace").when(opt).get();
        Optional<String> optRev = Optional.of("rev");
        doReturn(optRev).when(cap).getRevision();
        doReturn(Optional.of("modName")).when(cap).getModuleName();
        doReturn(Lists.newArrayList("loc")).when(cap).getLocation();
        doNothing().when(snapshot).close();

        assertNotNull(service.getSchemas());
        verify(snapshot, times(1)).close();

        NetconfServerSessionListener sessionListener = mock(NetconfServerSessionListener.class);
        Channel channel = mock(Channel.class);
        doReturn("mockChannel").when(channel).toString();
        NetconfHelloMessageAdditionalHeader header = new NetconfHelloMessageAdditionalHeader("name", "addr", "2", "tcp", "id");
        NetconfServerSession sm = new NetconfServerSession(sessionListener, channel, 10, header);
        doNothing().when(sessionListener).onSessionUp(any(NetconfServerSession.class));
        sm.sessionUp();
        service.onSessionUp(sm);
        assertEquals(1, service.getSessions().getSession().size());

        assertEquals(Long.valueOf(10), service.getSessions().getSession().get(0).getSessionId());

        service.onSessionDown(sm);
        assertEquals(0, service.getSessions().getSession().size());
    }
}
