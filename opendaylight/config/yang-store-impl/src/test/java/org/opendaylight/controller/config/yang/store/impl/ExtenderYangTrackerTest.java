/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import org.opendaylight.controller.config.yang.store.impl.ExtenderYangTracker;
import org.opendaylight.controller.config.yang.store.impl.MbeParser;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import com.google.common.collect.Lists;

public class ExtenderYangTrackerTest {

	@Mock
	private BundleContext context;
	private ExtenderYangTracker tested;
	@Mock
	private MbeParser parser;
	@Mock
	private YangStoreSnapshot yangStoreSnapshot;

	@Before
	public void setUp() throws YangStoreException {
		MockitoAnnotations.initMocks(this);
		doReturn("context").when(context).toString();
		tested = new ExtenderYangTracker(context, parser);
		doReturn(yangStoreSnapshot).when(parser).parseYangFiles(anyCollectionOf(InputStream.class));
		doReturn(22).when(yangStoreSnapshot).countModuleMXBeanEntries();
		doReturn("mock yang store").when(yangStoreSnapshot).toString();
	}

	@Test
	public void testCache() throws MalformedURLException, YangStoreException, InterruptedException {
		Bundle bundle = getMockedBundle(5, false);
		tested.addingBundle(bundle, null);
		bundle = getMockedBundle(2, false);
		tested.addingBundle(bundle, null);
		bundle = getMockedBundle(10, false);
		tested.addingBundle(bundle, null);
        YangStoreSnapshot returnedStore;

        returnedStore = tested.getYangStoreSnapshot();

        assertEquals(yangStoreSnapshot, returnedStore);

		tested.removedBundle(bundle, null, null);
        tested.getYangStoreSnapshot();

		bundle = getMockedBundle(10, false);
		tested.addingBundle(bundle, null);

		tested.getYangStoreSnapshot();

		verify(parser, times(3)).parseYangFiles(anyCollectionOf(InputStream.class));

        returnedStore = tested.getYangStoreSnapshot();

		verifyNoMoreInteractions(parser);
		assertEquals(yangStoreSnapshot, returnedStore);
	}


	int bundleCounter = 1;

	private Bundle getMockedBundle(int sizeOfUrls, boolean system) throws MalformedURLException {
		Bundle mock = mock(Bundle.class);

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
