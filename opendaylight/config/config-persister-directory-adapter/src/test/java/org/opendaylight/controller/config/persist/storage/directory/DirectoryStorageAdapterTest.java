/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.directory;

import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DirectoryStorageAdapterTest {
    DirectoryPersister tested;
    SortedSet<String> expectedCapabilities;
    String expectedSnapshot;

    @Before
    public void setUp() throws Exception {
        expectedCapabilities = new TreeSet<>(IOUtils.readLines(getClass().getResourceAsStream("/expectedCapabilities.txt")));
        expectedSnapshot = IOUtils.toString(getClass().getResourceAsStream("/expectedSnapshot.xml"));
    }

    @Test
    public void testEmptyDirectory() throws Exception {
        File folder = new File("target/emptyFolder");
        folder.mkdir();
        tested = new DirectoryPersister((folder));
        assertEquals(Optional.<ConfigSnapshotHolder>absent(), tested.loadLastConfig());

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
        tested = new DirectoryPersister((folder));
        assertExpected();
    }

    private void assertExpected() throws IOException {
        Optional<ConfigSnapshotHolder> maybeResult = tested.loadLastConfig();
        assertTrue(maybeResult.isPresent());
        ConfigSnapshotHolder result = maybeResult.get();
        assertEquals(expectedCapabilities, result.getCapabilities());
        assertEquals(expectedSnapshot, result.getConfigSnapshot());
    }

    @Test
    public void testTwoFiles() throws Exception {
        File folder = getFolder("twoFiles");
        tested = new DirectoryPersister((folder));
        assertExpected();
    }

}
