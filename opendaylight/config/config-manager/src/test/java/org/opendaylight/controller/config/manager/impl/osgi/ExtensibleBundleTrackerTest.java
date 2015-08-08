/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ExtensibleBundleTrackerTest {

    @Mock
    private BundleContext bundleContext;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleEvent bundleEvent;

    @Mock
    private BundleTrackerCustomizer<Object> primaryTracker;
    @Mock
    private BundleTrackerCustomizer<?> additionalTracker;

    private ExtensibleBundleTracker<Object> extensibleBundleTracker;
    private Object primaryValue = new Object();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn("bundle").when(bundle).toString();
        doReturn("bundleEvent").when(bundleEvent).toString();

        doReturn(primaryValue).when(primaryTracker).addingBundle(bundle, bundleEvent);
        doNothing().when(primaryTracker).modifiedBundle(bundle, bundleEvent, primaryValue);
        doNothing().when(primaryTracker).removedBundle(bundle, bundleEvent, primaryValue);

        doReturn(new Object()).when(additionalTracker).addingBundle(bundle, bundleEvent);
        doNothing().when(additionalTracker).modifiedBundle(bundle, bundleEvent, null);
        doNothing().when(additionalTracker).removedBundle(bundle, bundleEvent, null);
        extensibleBundleTracker = new ExtensibleBundleTracker<>(bundleContext, primaryTracker, additionalTracker);
    }

    @Test
    public void testAddingBundle() throws Exception {
        assertEquals(primaryValue, extensibleBundleTracker.addingBundle(bundle, bundleEvent).get());
        InOrder inOrder = Mockito.inOrder(primaryTracker, additionalTracker);
        inOrder.verify(primaryTracker).addingBundle(bundle, bundleEvent);
        inOrder.verify(additionalTracker).addingBundle(bundle, bundleEvent);
    }

    @Test
    public void testRemovedBundle() throws Exception {
        extensibleBundleTracker.removedBundle(bundle, bundleEvent, Futures.immediateFuture(primaryValue));
        InOrder inOrder = Mockito.inOrder(primaryTracker, additionalTracker);
        inOrder.verify(primaryTracker).removedBundle(bundle, bundleEvent, primaryValue);
        inOrder.verify(additionalTracker).removedBundle(bundle, bundleEvent, null);
    }

    @Test
    public void testRemovedBundleWithEx() throws Exception {
        IllegalStateException throwable = new IllegalStateException();
        extensibleBundleTracker.removedBundle(bundle, bundleEvent, Futures.immediateFailedFuture(throwable));
        verifyZeroInteractions(primaryTracker);
        verifyZeroInteractions(additionalTracker);
    }
}
