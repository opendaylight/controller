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
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter.PropertiesProvider;
import org.opendaylight.controller.config.persist.storage.file.FileStorageAdapter;
import org.opendaylight.controller.netconf.persist.impl.osgi.ConfigPersisterActivator;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersisterImplTest {
    @Mock
    TestingPropertiesProvider propertiesProvider;

    class TestingPropertiesProvider implements PropertiesProvider {
        @Override
        public String getFullKeyForReporting(String key) {
            return "prefix" + key;
        }

        @Override
        public String getProperty(String key) {
            throw new UnsupportedOperationException("should be mocked");
        }
    }

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        doCallRealMethod().when(propertiesProvider).getFullKeyForReporting(anyString());
    }

    @Test
    public void testFromProperties() throws Exception {
        doReturn(MockAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);

        PersisterImpl persisterImpl = PersisterImpl.createFromProperties(propertiesProvider);
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

        doReturn(FileStorageAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);

        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                propertiesProvider).getProperty(FileStorageAdapter.FILE_STORAGE_PROP);
        doReturn("propertiesProvider").when(propertiesProvider).toString();
        doReturn(null).when(propertiesProvider).getProperty("numberOfBackups");

        PersisterImpl persisterImpl = PersisterImpl.createFromProperties(propertiesProvider);
        assertTrue(persisterImpl.getStorage() instanceof FileStorageAdapter);
    }

    @Test
    public void testFromProperties3() throws Exception {

        doReturn(FileStorageAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);
        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                propertiesProvider).getProperty(FileStorageAdapter.FILE_STORAGE_PROP);
        doReturn("propertiesProvider").when(propertiesProvider).toString();
        doReturn("0").when(propertiesProvider).getProperty("numberOfBackups");
        try {
            PersisterImpl.createFromProperties(propertiesProvider);
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
        public void setProperties(PropertiesProvider propertiesProvider) {
            props++;
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

        }

    }

}
