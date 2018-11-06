/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;

@Deprecated
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DOMMountPointAdapterTest {
    @Mock
    private DOMMountPoint delegate;

    private DOMMountPointAdapter adapter;

    @Before
    public void before() {
        doReturn(Optional.empty()).when(delegate).getService(any());
        adapter = new DOMMountPointAdapter(delegate);
    }

    @Test
    public void testDOMActionService() {
        assertFalse(adapter.getService(DOMActionService.class).isPresent());

        org.opendaylight.mdsal.dom.api.DOMActionService mdsal =
                mock(org.opendaylight.mdsal.dom.api.DOMActionService.class);

        doReturn(Optional.of(mdsal)).when(delegate).getService(org.opendaylight.mdsal.dom.api.DOMActionService.class);
        assertTrue(adapter.getService(DOMActionService.class).isPresent());
    }

    @Test
    public void testDOMDataBroker() {
        assertFalse(adapter.getService(DOMDataBroker.class).isPresent());

        org.opendaylight.mdsal.dom.api.DOMDataBroker mdsal = mock(org.opendaylight.mdsal.dom.api.DOMDataBroker.class);
        doReturn(ImmutableClassToInstanceMap.of()).when(mdsal).getExtensions();

        doReturn(Optional.of(mdsal)).when(delegate).getService(org.opendaylight.mdsal.dom.api.DOMDataBroker.class);
        assertTrue(adapter.getService(DOMDataBroker.class).isPresent());
    }

    @Test
    public void testDOMNotificationService() {
        assertFalse(adapter.getService(DOMNotificationService.class).isPresent());

        org.opendaylight.mdsal.dom.api.DOMNotificationService mdsal =
                mock(org.opendaylight.mdsal.dom.api.DOMNotificationService.class);

        doReturn(Optional.of(mdsal)).when(delegate).getService(
            org.opendaylight.mdsal.dom.api.DOMNotificationService.class);
        assertTrue(adapter.getService(DOMNotificationService.class).isPresent());
    }

    @Test
    public void testDOMRpcService() {
        assertFalse(adapter.getService(DOMRpcService.class).isPresent());

        org.opendaylight.mdsal.dom.api.DOMRpcService mdsal = mock(org.opendaylight.mdsal.dom.api.DOMRpcService.class);

        doReturn(Optional.of(mdsal)).when(delegate).getService(org.opendaylight.mdsal.dom.api.DOMRpcService.class);
        assertTrue(adapter.getService(DOMRpcService.class).isPresent());
    }
}
