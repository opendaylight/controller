/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.impl.BindingDataBrokerAdapter;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingDataBrokerAdapterTest {
    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Mock
    private org.opendaylight.mdsal.binding.api.DataBroker delegateDataBroker;

    @Mock
    private ListenerRegistration<org.opendaylight.mdsal.binding.api.DataTreeChangeListener<?>> listenerRegistration;

    @Mock
    private ClusteredDataTreeChangeListener<Top> clusteredDataTreeChangeListener;

    private BindingDataBrokerAdapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        adapter = new BindingDataBrokerAdapter(delegateDataBroker);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testClusteredDataTreeChangeListenerRegisteration() {
        doReturn(listenerRegistration).when(delegateDataBroker).registerDataTreeChangeListener(any(), any());

        ListenerRegistration<ClusteredDataTreeChangeListener<Top>> bindingListenerReg =
                adapter.registerDataTreeChangeListener(
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, TOP_PATH),
                        clusteredDataTreeChangeListener);

        ArgumentCaptor<org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener> delegateListener =
                ArgumentCaptor.forClass(org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener.class);
        verify(delegateDataBroker).registerDataTreeChangeListener(
                eq(org.opendaylight.mdsal.binding.api.DataTreeIdentifier.create(
                        org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL, TOP_PATH)),
                delegateListener.capture());

        bindingListenerReg.close();

        verify(listenerRegistration).close();
    }
}
