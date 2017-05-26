/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.directory.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import org.junit.Test;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.test.PropertiesProviderTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class DirectoryStorageAdapterTest {
    private static final Logger LOG = LoggerFactory.getLogger(DirectoryStorageAdapterTest.class);
    Persister tested;

    private Persister instantiatePersisterFromAdapter(final File file, final Optional<String> extensions){
        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty(XmlDirectoryStorageAdapter.DIRECTORY_STORAGE_PROP,file.getPath());
        if(extensions.isPresent()) {
            pp.addProperty(XmlDirectoryStorageAdapter.INCLUDE_EXT_PROP, extensions.get());
        }

        XmlDirectoryStorageAdapter dsa = new XmlDirectoryStorageAdapter();
        return dsa.instantiate(pp);
    }

    private Persister instantiatePersisterFromAdapter(final File file){
        return instantiatePersisterFromAdapter(file, Optional.<String>absent());
    }

    @Test
    public void testEmptyDirectory() throws Exception {
        File folder = new File("target/emptyFolder");
        folder.mkdir();

        tested = instantiatePersisterFromAdapter(folder);
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

    private File getFolder(final String folderName) {
        File result = new File(("src/test/resources/" +
                folderName).replace("/", File.separator));
        assertTrue(result + " is not a directory", result.isDirectory());
        return result;
    }

    @Test
    public void testOneFile() throws Exception {
        File folder = getFolder("oneFile");
        tested = instantiatePersisterFromAdapter(folder, Optional.of("xml"));

        LOG.info("Testing : {}", tested);
        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(1, results.size());
        ConfigSnapshotHolder result = results.get(0);
        assertResult(result, "<config>1</config>", "cap1&rev", "cap2", "capa a");
    }

    @Test
    public void testOneFileWrongExtension() throws Exception {
        File folder = getFolder("oneFile");
        tested = instantiatePersisterFromAdapter(folder, Optional.of("aa, bb"));
        LOG.info("Testing : {}", tested);
    }

    private void assertResult(final ConfigSnapshotHolder result, final String s, final String... caps) throws SAXException, IOException {
        assertXMLEqual(s, result.getConfigSnapshot());
        int i = 0;
        for (String capFromSnapshot : result.getCapabilities()) {
            assertEquals(capFromSnapshot, caps[i++]);
        }
    }

    @Test
    public void testTwoFilesAllExtensions() throws Exception {
        File folder = getFolder("twoFiles");
        tested = instantiatePersisterFromAdapter(folder);
        LOG.info("Testing : {}", tested);
        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(2, results.size());

        assertResult(results.get(0), "<config>1</config>", "cap1-a", "cap2-a", "capa a-a");
        assertResult(results.get(1), "<config>2</config>", "cap1-b", "cap2-b", "capa a-b");
    }

    @Test
    public void testTwoFilesTwoExtensions() throws Exception {
        File folder = getFolder("twoFiles");
        tested = instantiatePersisterFromAdapter(folder, Optional.of("xml, xml2"));
        LOG.info("Testing : {}", tested);
        assertEquals(2, tested.loadLastConfigs().size());
    }

    @Test
    public void testTwoFilesOnlyOneExtension() throws Exception {
        File folder = getFolder("twoFiles");
        tested = instantiatePersisterFromAdapter(folder, Optional.of("xml"));
        LOG.info("Testing : ", tested);
        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(1, results.size());

        assertResult(results.get(0), "<config>1</config>", "cap1-a", "cap2-a", "capa a-a");
    }

    @Test
    public void testTwoFilesOneInvalid() throws Exception {
        File folder = getFolder("twoFiles_corrupt");
        tested = instantiatePersisterFromAdapter(folder, Optional.of("xml"));
        LOG.info("Testing : {}", tested);
        List<ConfigSnapshotHolder> results = tested.loadLastConfigs();
        assertEquals(1, results.size());

        assertResult(results.get(0), "<config>1</config>", "cap1-a", "cap2-a", "capa a-a");
    }

}
