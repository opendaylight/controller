/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.file;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.test.PropertiesProviderTest;
import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FileStorageAdapterTest {

    private static int i;
    private File file;

    @Before
    public void setUp() throws Exception {
        file = Files.createTempFile("testFilePersist", ".txt").toFile();
        if (!file.exists())
            return;
        com.google.common.io.Files.write("", file, Charsets.UTF_8);
        i = 1;
    }


    @Test
    public void testFileAdapterAsPersister() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("fileStorage",file.getPath());
        pp.addProperty("numberOfBackups",Integer.toString(Integer.MAX_VALUE));

        Persister configPersister = storage.instantiate(pp);
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
        configPersister.persistConfig(holder);

        configPersister.persistConfig(holder);

        Collection<String> readLines = Collections2.filter(com.google.common.io.Files.readLines(file, Charsets.UTF_8),
                new Predicate<String>() {

                    @Override
                    public boolean apply(String input) {
                        if (input.equals(""))
                            return false;
                        return true;
                    }
                });
        assertEquals(14, readLines.size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertEquals("<config>2</config>",
                configSnapshotHolder.getConfigSnapshot().replaceAll("\\s", ""));
        assertEquals(createCaps(), configSnapshotHolder.getCapabilities());
    }
    @Test
    public void testFileAdapter() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
        storage.setFileStorage(file);
        storage.setNumberOfBackups(Integer.MAX_VALUE);
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

        Collection<String> readLines = Collections2.filter(com.google.common.io.Files.readLines(file, Charsets.UTF_8),
                new Predicate<String>() {

                    @Override
                    public boolean apply(String input) {
                        if (input.equals(""))
                            return false;
                        return true;
                    }
                });
        assertEquals(14, readLines.size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertEquals("<config>2</config>",
                configSnapshotHolder.getConfigSnapshot().replaceAll("\\s", ""));
        assertEquals(createCaps(), configSnapshotHolder.getCapabilities());
    }

    private SortedSet<String> createCaps() {
        SortedSet<String> caps = new TreeSet<>();

        caps.add("cap1");
        caps.add("cap2");
        caps.add("capaaaa as dasfasdf s2");
        return caps;
    }

    @Test
    public void testFileAdapterOneBackup() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
        storage.setFileStorage(file);
        storage.setNumberOfBackups(1);
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

        Collection<String> readLines = Collections2.filter(com.google.common.io.Files.readLines(file, Charsets.UTF_8),
                new Predicate<String>() {

                    @Override
                    public boolean apply(String input) {
                        if (input.equals(""))
                            return false;
                        return true;
                    }
                });
        assertEquals(7, readLines.size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertEquals("<config>2</config>",
                configSnapshotHolder.getConfigSnapshot().replaceAll("\\s", ""));
    }

    @Test
    public void testFileAdapterOnlyTwoBackups() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
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

        Collection<String> readLines = Collections2.filter(com.google.common.io.Files.readLines(file, Charsets.UTF_8),
                new Predicate<String>() {

                    @Override
                    public boolean apply(String input) {
                        if (input.equals(""))
                            return false;
                        return true;
                    }
                });

        assertEquals(14, readLines.size());

        List<ConfigSnapshotHolder> lastConf = storage.loadLastConfigs();
        assertEquals(1, lastConf.size());
        ConfigSnapshotHolder configSnapshotHolder = lastConf.get(0);
        assertEquals("<config>3</config>",
                configSnapshotHolder.getConfigSnapshot().replaceAll("\\s", ""));
        assertFalse(readLines.contains(holder.getConfigSnapshot()));
    }

    @Test
    public void testNoLastConfig() throws Exception {
        File file = Files.createTempFile("testFilePersist", ".txt").toFile();
        if (!file.exists())
            return;
        FileStorageAdapter storage = new FileStorageAdapter();
        storage.setFileStorage(file);

        List<ConfigSnapshotHolder> elementOptional = storage.loadLastConfigs();
        assertThat(elementOptional.size(), is(0));
    }

    @Test(expected = NullPointerException.class)
    public void testNoProperties() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
        storage.loadLastConfigs();
    }

    @Test(expected = NullPointerException.class)
    public void testNoProperties2() throws Exception {
        FileStorageAdapter storage = new FileStorageAdapter();
        storage.persistConfig(new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return Mockito.mock(String.class);
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return new TreeSet<>();
            }
        } );
    }

    static String createConfig() {
        return "<config>" + i++ + "</config>";
    }

}
