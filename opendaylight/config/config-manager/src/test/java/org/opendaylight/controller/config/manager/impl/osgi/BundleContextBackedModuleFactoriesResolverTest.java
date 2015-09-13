/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleContextBackedModuleFactoriesResolverTest {

    @Mock
    private ModuleFactoryBundleTracker mockBundleTracker;

    @Mock
    private BundleContext mockBundleContext1;

    @Mock
    private BundleContext mockBundleContext2;

    @Mock
    private Bundle mockBundle1;

    @Mock
    private Bundle mockBundle2;

    private BundleContextBackedModuleFactoriesResolver resolver;
    private ModuleFactory f1;
    private ModuleFactory f2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(mockBundleContext1).when(mockBundle1).getBundleContext();
        doReturn(mockBundleContext2).when(mockBundle2).getBundleContext();

        f1 = getMockFactory("f1");
        f2 = getMockFactory("f2");

        resolver = new BundleContextBackedModuleFactoriesResolver();
        resolver.setModuleFactoryBundleTracker(mockBundleTracker);
    }

    private ModuleFactory getMockFactory(final String name) {
        ModuleFactory mock = mock(ModuleFactory.class);
        doReturn(name).when(mock).toString();
        doReturn(name).when(mock).getImplementationName();
        return mock;
    }

    @Test
    public void testGetAllFactories() throws Exception {
        doReturn(Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle1),
                new AbstractMap.SimpleImmutableEntry<>(f2, mockBundle2))).
                        when(mockBundleTracker).getModuleFactoryEntries();

        Map<String, Map.Entry<ModuleFactory, BundleContext>> allFactories = resolver.getAllFactories();
        assertEquals(2, allFactories.size());
        assertTrue(allFactories.containsKey(f1.getImplementationName()));
        assertEquals(f1, allFactories.get(f1.getImplementationName()).getKey());
        assertEquals(mockBundleContext1, allFactories.get(f1.getImplementationName()).getValue());
        assertTrue(allFactories.containsKey(f2.getImplementationName()));
        assertEquals(f2, allFactories.get(f2.getImplementationName()).getKey());
        assertEquals(mockBundleContext2, allFactories.get(f2.getImplementationName()).getValue());
    }

    @Test
    public void testDuplicateFactories() throws Exception {
        doReturn(Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle1),
                new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle2))).
                        when(mockBundleTracker).getModuleFactoryEntries();

        try {
            resolver.getAllFactories();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(f1.getImplementationName()));
            assertThat(e.getMessage(), containsString("unique"));
            return;
        }

        fail("Should fail with duplicate factory name");
    }

    @Test(expected = IllegalStateException.class)
    public void testNullFactoryName() throws Exception {
        doReturn(Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle1))).
                when(mockBundleTracker).getModuleFactoryEntries();

        doReturn(null).when(f1).getImplementationName();
        resolver.getAllFactories();
    }

    @Test(expected = IllegalStateException.class)
    public void testNullBundleContext() throws Exception {
        doReturn(null).when(mockBundle1).getBundleContext();
        doReturn(Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle1))).
                when(mockBundleTracker).getModuleFactoryEntries();

        resolver.setBundleContextTimeout(100);
        resolver.getAllFactories();
    }

    @Test
    public void testBundleContextInitiallyNull() throws Exception {
        final AtomicReference<BundleContext> bundleContext = new AtomicReference<>();
        Answer<BundleContext> answer = new Answer<BundleContext>() {
            @Override
            public BundleContext answer(InvocationOnMock invocation) throws Throwable {
                return bundleContext.get();
            }
        };

        doAnswer(answer).when(mockBundle1).getBundleContext();
        doReturn(Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(f1, mockBundle1))).
                when(mockBundleTracker).getModuleFactoryEntries();

        final AtomicReference<Map<String, Map.Entry<ModuleFactory, BundleContext>>> allFactories = new AtomicReference<>();
        final AtomicReference<Exception> caughtEx = new AtomicReference<>();
        final CountDownLatch doneLatch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                try {
                    allFactories.set(resolver.getAllFactories());
                } catch (Exception e) {
                    caughtEx.set(e);
                } finally {
                    doneLatch.countDown();
                }
            }
        }.start();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        bundleContext.set(mockBundleContext1);

        assertEquals(true, doneLatch.await(5, TimeUnit.SECONDS));
        if(caughtEx.get() != null) {
            throw caughtEx.get();
        }

        assertEquals(1, allFactories.get().size());
        assertTrue(allFactories.get().containsKey(f1.getImplementationName()));
    }
}
