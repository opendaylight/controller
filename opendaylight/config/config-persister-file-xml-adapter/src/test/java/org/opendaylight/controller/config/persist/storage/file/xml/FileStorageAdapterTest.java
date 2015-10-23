/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.file.xml;

import static junit.framework.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.test.PropertiesProviderTest;

public class FileStorageAdapterTest {

    private static int i;
    private File file;
    private static final String NON_EXISTENT_DIRECTORY = "./nonExistentDir/";
    private static final String NON_EXISTENT_FILE = "nonExistent.txt";
    private XmlFileStorageAdapter storage;

    @Before
    public void setUp() throws Exception {
        file = Files.createTempFile("testFilePersist", ".txt").toFile();
        file.deleteOnExit();
        if (!file.exists()) {
            return;
        }
        com.google.common.io.Files.write("", file, Charsets.UTF_8);
        i = 1;
        storage = new XmlFileStorageAdapter();
    }

    @After
    public void tearDown() throws Exception {
        storage.reset();
    }

    @Test
    public void testNewFile() throws Exception {
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",NON_EXISTENT_DIRECTORY+NON_EXISTENT_FILE);
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));
        storage.instantiate(pp);

        final ConfigSnapshotHolder holder = new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return createConfig();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return createCaps();
            }
        };
        storage.persistConfig(holder);

        storage.persistConfig(holder);

        assertEquals(storage.toString().replace("\\","/"),"XmlFileStorageAdapter [storage="+NON_EXISTENT_DIRECTORY+NON_EXISTENT_FILE+"]");
        delete(new File(NON_EXISTENT_DIRECTORY));
    }

    @Test
    public void testFileAdapter() throws Exception {
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",file.getPath());
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));
        storage.instantiate(pp);

        final ConfigSnapshotHolder holder = new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return createConfig();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return createCaps();
            }
        };
        storage.persistConfig(holder);

        storage.persistConfig(holder);

        assertEquals(29, com.google.common.io.Files.readLines(file, Charsets.UTF_8).size());
        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertXMLEqual("<config>2</config>", configSnapshotHolder.getConfigSnapshot());
        assertEquals(createCaps(), configSnapshotHolder.getCapabilities());

        storage = new XmlFileStorageAdapter();
        storage.setFileStorage(file);
        storage.setNumberOfBackups(Integer.MAX_VALUE);

        List<ConfigSnapshotHolder> last = storage.loadLastConfigs();
        assertEquals(createCaps(), last.get(0).getCapabilities());
    }

    private SortedSet<String> createCaps() {
        SortedSet<String> caps = new TreeSet<>();

        caps.add("cap1" + i);
        caps.add("cap2" + i);
        caps.add("urn:opendaylight:params:xml:ns:yang:controller:netty?module=netty&revision=2013-11-19" + i);
        caps.add("capaaaa as dasfasdf s2" + i);
        return caps;
    }

    @Test
    public void testFileAdapterOneBackup() throws Exception {
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",file.getPath());
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));
        storage.instantiate(pp);

        final ConfigSnapshotHolder holder = new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return createConfig();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return createCaps();
            }
        };
        storage.persistConfig(holder);

        storage.persistConfig(holder);

        assertEquals(29, com.google.common.io.Files.readLines(file, Charsets.UTF_8).size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertXMLEqual("<config>2</config>", configSnapshotHolder.getConfigSnapshot());
    }

    @Test
    public void testWithFeatures() throws Exception {
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",file.getPath());
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));
        storage.instantiate(pp);

        final ConfigSnapshotHolder holder = new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return createConfig();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return createCaps();
            }
        };
        final FeatureListProvider mock = mock(FeatureListProvider.class);

        doReturn(Sets.newHashSet("f1-11", "f2-22")).when(mock).listFeatures();
        storage.setFeaturesService(mock);
        storage.persistConfig(holder);

        assertEquals(20, com.google.common.io.Files.readLines(file, Charsets.UTF_8).size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertXMLEqual("<config>1</config>", configSnapshotHolder.getConfigSnapshot());
        assertEquals(Sets.newHashSet("f1-11", "f2-22"), storage.getPersistedFeatures());
    }

    @Test
    public void testNoFeaturesStored() throws Exception {
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",file.getPath());
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));
        storage.instantiate(pp);

        com.google.common.io.Files.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<persisted-snapshots>\n" +
                "   <snapshots>\n" +
                "      <snapshot>\n" +
                "         <required-capabilities>\n" +
                "            <capability>cap12</capability>\n" +
                "         </required-capabilities>\n" +
                "         <configuration>\n" +
                "            <config>1</config>\n" +
                "         </configuration>\n" +
                "      </snapshot>\n" +
                "   </snapshots>\n" +
                "</persisted-snapshots>", file, Charsets.UTF_8);

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertXMLEqual("<config>1</config>", configSnapshotHolder.getConfigSnapshot());
        assertTrue(storage.getPersistedFeatures().isEmpty());
    }

    @Test
    public void testFileAdapterOnlyTwoBackups() throws Exception {
        storage.setFileStorage(file);
        storage.setNumberOfBackups(2);
        final ConfigSnapshotHolder holder = new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return createConfig();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return createCaps();
            }
        };
        storage.persistConfig(holder);

        storage.persistConfig(holder);
        storage.persistConfig(holder);

        List<String> readLines = com.google.common.io.Files.readLines(file, Charsets.UTF_8);
        assertEquals(29, readLines.size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertXMLEqual("<config>3</config>", configSnapshotHolder.getConfigSnapshot());
        assertFalse(readLines.contains(holder.getConfigSnapshot()));
        assertTrue(storage.getPersistedFeatures().isEmpty());
    }

    @Test
    public void testNoLastConfig() throws Exception {
        File file = Files.createTempFile("testFilePersist", ".txt").toFile();
        file.deleteOnExit();
        if (!file.exists()) {
            return;
        }
        try (XmlFileStorageAdapter storage = new XmlFileStorageAdapter()) {
            storage.setFileStorage(file);

            List<ConfigSnapshotHolder> elementOptional = storage.loadLastConfigs();
            assertThat(elementOptional.size(), is(0));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNoProperties() throws Exception {
        try (XmlFileStorageAdapter storage = new XmlFileStorageAdapter()) {
            storage.loadLastConfigs();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNoProperties2() throws Exception {
        try (XmlFileStorageAdapter storage = new XmlFileStorageAdapter()) {
            storage.persistConfig(new ConfigSnapshotHolder() {
                @Override
                public String getConfigSnapshot() {
                    return mock(String.class);
                }

                @Override
                public SortedSet<String> getCapabilities() {
                    return new TreeSet<>();
                }
            });
        }
    }

    static String createConfig() {
        return "<config>" + i++ + "</config>";
    }

    private void delete(final File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        if (!f.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + f);
        }
    }
}
