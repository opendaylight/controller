/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.DataObject;
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
    YangInstanceIdentifier yangInstanceIdentifier;


    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier
        .create(Top.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClusteredDataChangeListernerRegisteration() {

        BindingToNormalizedNodeCodec codec = new BindingToNormalizedNodeCodec(classLoadingStrategy, codecRegistry);

        try (BindingDOMDataBrokerAdapter bindingDOMDataBrokerAdapter = new BindingDOMDataBrokerAdapter(dataBroker,
                codec)) {
            Mockito.when(codecRegistry.toYangInstanceIdentifier(TOP_PATH)).thenReturn(yangInstanceIdentifier);

            ArgumentCaptor<ClusteredDOMDataChangeListener> clusteredDOMListener = ArgumentCaptor
                    .forClass(ClusteredDOMDataChangeListener.class);
            ArgumentCaptor<LogicalDatastoreType> logicalDatastoreType = ArgumentCaptor
                    .forClass(LogicalDatastoreType.class);
            ArgumentCaptor<AsyncDataBroker.DataChangeScope> dataChangeScope = ArgumentCaptor
                    .forClass(AsyncDataBroker.DataChangeScope.class);
            ArgumentCaptor<YangInstanceIdentifier> yangInstanceIdentifier = ArgumentCaptor
                    .forClass(YangInstanceIdentifier.class);

            TestListener listener = new TestListener();

            bindingDOMDataBrokerAdapter.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, TOP_PATH,
                    listener, AsyncDataBroker.DataChangeScope.BASE);
            Mockito.verify(dataBroker).registerDataChangeListener(logicalDatastoreType.capture(),
                    yangInstanceIdentifier.capture(), clusteredDOMListener.capture(), dataChangeScope.capture());
        }

    }

    private class TestListener implements ClusteredDataChangeListener {

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        }
    }
}
