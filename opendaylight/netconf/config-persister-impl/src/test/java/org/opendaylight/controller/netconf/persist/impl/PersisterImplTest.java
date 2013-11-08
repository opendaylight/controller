/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.opendaylight.controller.config.persist.storage.file.FileStorageAdapter;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersisterImplTest {
    @Mock
    BundleContext mockedContext;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFromProperties() throws Exception {
        doReturn(MockAdapter.class.getName()).when(mockedContext).getProperty(
                PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);

        PersisterImpl persisterImpl = PersisterImpl.createFromProperties(mockedContext).get();
        persisterImpl.persistConfig(null);
        persisterImpl.loadLastConfig();
        persisterImpl.persistConfig(null);
        persisterImpl.loadLastConfig();

        assertEquals(2, MockAdapter.persist);
        assertEquals(2, MockAdapter.load);
        assertEquals(1, MockAdapter.props);
    }

    @Test
    public void testFromProperties2() throws Exception {
        mockedContext = mock(BundleContext.class);
        doReturn(FileStorageAdapter.class.getName()).when(mockedContext).getProperty(
                PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);
        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                mockedContext).getProperty(FileStorageAdapter.FILE_STORAGE_PROP);
        doReturn("mockedContext").when(mockedContext).toString();
        doReturn(null).when(mockedContext).getProperty("numberOfBackups");

        PersisterImpl persisterImpl = PersisterImpl.createFromProperties(mockedContext).get();
        assertTrue(persisterImpl.getStorage() instanceof FileStorageAdapter);
    }

    @Test
    public void testFromProperties3() throws Exception {
        mockedContext = mock(BundleContext.class);
        doReturn(FileStorageAdapter.class.getName()).when(mockedContext).getProperty(
                PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);
        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                mockedContext).getProperty(FileStorageAdapter.FILE_STORAGE_PROP);
        doReturn("mockedContext").when(mockedContext).toString();
        doReturn("0").when(mockedContext).getProperty("numberOfBackups");
        try {
            PersisterImpl.createFromProperties(mockedContext).get();
            fail();
        } catch (RuntimeException e) {
            assertThat(
                    e.getMessage(),
                    containsString("numberOfBackups property should be either set to positive value, or ommited. Can not be set to 0."));
        }
    }

    @Test
    public void test() throws Exception {
        StorageAdapter storage = mock(StorageAdapter.class);
        doReturn(null).when(storage).loadLastConfig();
        doNothing().when(storage).persistConfig(any(Persister.ConfigSnapshotHolder.class));
        PersisterImpl persister = new PersisterImpl(storage);
        persister.loadLastConfig();
        persister.persistConfig(null);

        verify(storage).loadLastConfig();
        verify(storage).persistConfig(any(Persister.ConfigSnapshotHolder.class));
    }

    public static class MockAdapter implements StorageAdapter {

        static int persist = 0;

        @Override
        public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
            persist++;
        }

        static int load = 0;

        @Override
        public Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException {
            load++;
            return null;// ?
        }

        static int props = 0;

        @Override
        public void setProperties(BundleContext configProvider) {
            props++;
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

        }

    }

}
