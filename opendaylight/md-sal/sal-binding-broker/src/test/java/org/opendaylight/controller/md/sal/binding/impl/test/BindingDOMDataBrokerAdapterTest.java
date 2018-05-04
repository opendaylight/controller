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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class BindingDOMDataBrokerAdapterTest {

    @Mock
    DOMDataBroker dataBroker;

    @Mock
    GeneratedClassLoadingStrategy classLoadingStrategy;

    @Mock
    BindingNormalizedNodeCodecRegistry codecRegistry;

    @Mock
    DOMDataTreeChangeService dataTreeChangeService;

    @Mock
    ListenerRegistration<DOMDataTreeChangeListener> listenerRegistration;

    @Mock
    ClusteredDataTreeChangeListener<Top> clusteredDataTreeChangeListener;

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClusteredDataTreeChangeListenerRegisteration() {

        doReturn(YangInstanceIdentifier.of(Top.QNAME)).when(codecRegistry).toYangInstanceIdentifier(TOP_PATH);

        doReturn(listenerRegistration).when(dataTreeChangeService).registerDataTreeChangeListener(any(), any());

        doReturn(ImmutableMap.of(DOMDataTreeChangeService.class, dataTreeChangeService))
            .when(dataBroker).getSupportedExtensions();

        final BindingToNormalizedNodeCodec codec =
                new BindingToNormalizedNodeCodec(this.classLoadingStrategy, this.codecRegistry);

        try (BindingDOMDataBrokerAdapter bindingDOMDataBrokerAdapter = new BindingDOMDataBrokerAdapter(this.dataBroker,
                codec)) {

            ListenerRegistration<ClusteredDataTreeChangeListener<Top>> bindingListenerReg =
                bindingDOMDataBrokerAdapter.registerDataTreeChangeListener(
                    new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, TOP_PATH),
                    clusteredDataTreeChangeListener);

            verify(dataTreeChangeService).registerDataTreeChangeListener(
                eq(new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(Top.QNAME))),
                any(ClusteredDOMDataTreeChangeListener.class));

            bindingListenerReg.close();

            verify(listenerRegistration).close();
        }
    }
}
