/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ExtenderYangTrackerCustomizerTest {


    private ExtenderYangTracker tested;
    @Mock
    private MbeParser parser;
    @Mock
    private YangStoreSnapshotImpl yangStoreSnapshot;
    @Mock
    private BundleContext bundleContext;

    private Map<String, Map.Entry<Module, String>> moduleMap = Maps.newHashMap();

    @Before
    public void setUp() throws YangStoreException {

        moduleMap.put("1", new Map.Entry<Module, String>() {
            @Override
            public Module getKey() {
                return mock(Module.class);
            }

            @Override
            public String getValue() {
                return "v";
            }

            @Override
            public String setValue(String value) {
                return "v";
            }
        });

        MockitoAnnotations.initMocks(this);
        doNothing().when(bundleContext).addBundleListener(any(BundleListener.class));
        doReturn(new Bundle[0]).when(bundleContext).getBundles();
        tested = new ExtenderYangTracker(parser, Optional.<Pattern>absent(), bundleContext);
        doReturn(yangStoreSnapshot).when(parser).parseYangFiles(
                anyCollectionOf(InputStream.class));
        doReturn(22).when(yangStoreSnapshot).countModuleMXBeanEntries();
        doReturn("mock yang store").when(yangStoreSnapshot).toString();
        doNothing().when(yangStoreSnapshot).close();
        doReturn(moduleMap).when(yangStoreSnapshot).getModuleMap();
        doReturn(Collections.emptyMap()).when(yangStoreSnapshot).getModuleMXBeanEntryMap();
    }

    @Test
    public void testCache() throws MalformedURLException, YangStoreException,
            InterruptedException {
        Bundle bundle = getMockedBundle(5, false);
        tested.addingBundle(bundle, null);
        bundle = getMockedBundle(2, false);
        tested.addingBundle(bundle, null);
        bundle = getMockedBundle(10, false);
        tested.addingBundle(bundle, null);
        YangStoreSnapshot returnedStore;

        returnedStore = tested.getYangStoreSnapshot();

        assertEquals(yangStoreSnapshot.getModuleMap(), returnedStore.getModuleMap());

        tested.removedBundle(bundle, null, null);
        tested.getYangStoreSnapshot();

        bundle = getMockedBundle(10, false);
        tested.addingBundle(bundle, null);

        for(int i = 0; i< 20; i++){
            tested.getYangStoreSnapshot();
        }

        verify(parser, times(5)).parseYangFiles(anyCollectionOf(InputStream.class));

        returnedStore = tested.getYangStoreSnapshot();

        verifyNoMoreInteractions(parser);
    }

    int bundleCounter = 1;

    private Bundle getMockedBundle(int sizeOfUrls, boolean system)
            throws MalformedURLException {
        Bundle mock = mock(Bundle.class);
        doReturn(32).when(mock).getState();//mock just for logging

        List<URL> urls = Lists.newArrayList();
        for (int i = 0; i < sizeOfUrls; i++) {
            urls.add(new URL("http://127.0." + bundleCounter++ + "." + i));
        }
        Enumeration<URL> abc = new TestEnumeration(urls);

        doReturn(abc).when(mock).findEntries("META-INF/yang", "*.yang", false);
        if (system)
            doReturn(0L).when(mock).getBundleId();
        else
            doReturn(1L).when(mock).getBundleId();

        doReturn("mockedBundle").when(mock).toString();
        doReturn("mockedBundle").when(mock).getSymbolicName();

        return mock;
    }

    private static final class TestEnumeration implements Enumeration<URL> {

        private final List<URL> urls;
        int currentPos = 0;

        public TestEnumeration(List<URL> urls) {
            this.urls = urls;
        }

        @Override
        public boolean hasMoreElements() {
            try {
                urls.get(currentPos);
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
            return true;
        }

        @Override
        public URL nextElement() {
            URL url = urls.get(currentPos++);
            return url;
        }

    }
}
