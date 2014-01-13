/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.directory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.test.PropertiesProviderTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DirectoryStorageAdapterTest {
    Persister tested;

    @Test
    public void testEmptyDirectory() throws Exception {
        File folder = new File("target/emptyFolder");
        folder.mkdir();

        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",folder.getPath());
        DirectoryStorageAdapter dsa = new DirectoryStorageAdapter();
        tested = dsa.instantiate(pp);
        assertEquals(Collections.<ConfigSnapshotHolder>emptyList(), tested.loadLastConfigs());

        try {
            tested.persistConfig(new ConfigSnapshotHolder() {
                @Override
                public String getConfigSnapshot() {
                    throw new RuntimeException();
                }

                @Override
                public SortedSet<String> getCapabilities() {
                    throw new RuntimeException();
                }
            });
            fail();
        } catch (UnsupportedOperationException e) {

        }
    }

    private File getFolder(String folderName) {
        File result = new File(("src/test/resources/" +
                folderName).replace("/", File.separator));
        assertTrue(result + " is not a directory", result.isDirectory());
        return result;
    }

    @Test
    public void testOneFile() throws Exception {
        File folder = getFolder("oneFile");
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",folder.getPath());
        DirectoryStorageAdapter dsa = new DirectoryStorageAdapter();
        tested = dsa.instantiate(pp);

        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(1, results.size());
        ConfigSnapshotHolder result = results.get(0);
        assertSnapshot(result, "oneFileExpected");
    }


    @Test
    public void testTwoFiles() throws Exception {
        File folder = getFolder("twoFiles");
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",folder.getPath());
        DirectoryStorageAdapter dsa = new DirectoryStorageAdapter();
        tested = dsa.instantiate(pp);

        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(2, results.size());
        assertSnapshot(results.get(0), "twoFilesExpected1");
        assertSnapshot(results.get(1), "twoFilesExpected2");
    }

    private void assertSnapshot(ConfigSnapshotHolder result, String directory) throws Exception {
        SortedSet<String> expectedCapabilities = new TreeSet<>(IOUtils.readLines(getClass().getResourceAsStream("/" + directory + "/expectedCapabilities.txt")));
        String expectedSnapshot = IOUtils.toString(getClass().getResourceAsStream("/" + directory + "/expectedSnapshot.xml"));
        assertEquals(expectedCapabilities, result.getCapabilities());
        assertEquals(expectedSnapshot, result.getConfigSnapshot());
    }

}
