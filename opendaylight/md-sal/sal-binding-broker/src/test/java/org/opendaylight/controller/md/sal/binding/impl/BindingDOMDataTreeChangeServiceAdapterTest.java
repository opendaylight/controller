/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import java.util.Collection;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Unit tests for BindingDOMDataTreeChangeServiceAdapter.
 *
 * @author Thomas Pantelis
 */
public class BindingDOMDataTreeChangeServiceAdapterTest {
    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Mock
    private DOMDataTreeChangeService mockDOMService;

    @Mock
    private GeneratedClassLoadingStrategy classLoadingStrategy;

    @Mock
    private BindingNormalizedNodeCodecRegistry codecRegistry;

    @Mock
    private YangInstanceIdentifier mockYangID;

    @SuppressWarnings("rawtypes")
    @Mock
    private ListenerRegistration mockDOMReg;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mockYangID).when(codecRegistry).toYangInstanceIdentifier(TOP_PATH);
    }

    @Test
    public void testRegisterDataTreeChangeListener() {
        BindingToNormalizedNodeCodec codec = new BindingToNormalizedNodeCodec(classLoadingStrategy, codecRegistry);

        DataTreeChangeService service = BindingDOMDataTreeChangeServiceAdapter.create(codec, mockDOMService);

        doReturn(mockDOMReg).when(mockDOMService).registerDataTreeChangeListener(domDataTreeIdentifier(mockYangID),
                any(DOMDataTreeChangeListener.class));
        DataTreeIdentifier<Top> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, TOP_PATH);
        TestClusteredDataTreeChangeListener mockClusteredListener = new TestClusteredDataTreeChangeListener();
        service.registerDataTreeChangeListener(treeId , mockClusteredListener);

        verify(mockDOMService).registerDataTreeChangeListener(domDataTreeIdentifier(mockYangID),
                isA(ClusteredDOMDataTreeChangeListener.class));

        reset(mockDOMService);
        doReturn(mockDOMReg).when(mockDOMService).registerDataTreeChangeListener(domDataTreeIdentifier(mockYangID),
                any(DOMDataTreeChangeListener.class));
        TestDataTreeChangeListener mockNonClusteredListener = new TestDataTreeChangeListener();
        service.registerDataTreeChangeListener(treeId , mockNonClusteredListener);

        verify(mockDOMService).registerDataTreeChangeListener(domDataTreeIdentifier(mockYangID),
                not(isA(ClusteredDOMDataTreeChangeListener.class)));
    }

    static DOMDataTreeIdentifier domDataTreeIdentifier(final YangInstanceIdentifier yangID) {
        return Matchers.argThat(new ArgumentMatcher<DOMDataTreeIdentifier>() {
            @Override
            public boolean matches(Object argument) {
                DOMDataTreeIdentifier treeId = (DOMDataTreeIdentifier) argument;
                return treeId.getDatastoreType() == LogicalDatastoreType.CONFIGURATION &&
                        yangID.equals(treeId.getRootIdentifier());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangID));
            }
        });
    }

    private static class TestClusteredDataTreeChangeListener implements ClusteredDataTreeChangeListener<Top> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Top>> changes) {
        }
    }

    private static class TestDataTreeChangeListener implements DataTreeChangeListener<Top> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Top>> changes) {
        }
    }
}
