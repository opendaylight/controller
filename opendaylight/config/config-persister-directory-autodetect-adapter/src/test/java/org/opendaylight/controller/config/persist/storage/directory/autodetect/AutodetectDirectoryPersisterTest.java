/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.autodetect;

import java.io.File;
import java.io.IOException;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.test.PropertiesProviderTest;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AutodetectDirectoryPersisterTest {

    @Test
    public void testCombined() throws Exception {
        File resourcePath = FileTypeTest.getResourceAsFile("/combined/1controller.txt.config");
        File parentFile = resourcePath.getParentFile();

        AutodetectDirectoryStorageAdapter adapter = new AutodetectDirectoryStorageAdapter();

        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",parentFile.getPath());
        AutodetectDirectoryPersister persister = (AutodetectDirectoryPersister) adapter.instantiate(pp);
        List<ConfigSnapshotHolder> configs = persister.loadLastConfigs();

        Assert.assertEquals(2, configs.size());
        String snapFromTxt = configs.get(0).getConfigSnapshot();
        org.junit.Assert.assertThat(snapFromTxt, JUnitMatchers.containsString("<config>txt</config>"));
        org.junit.Assert.assertThat(snapFromTxt, JUnitMatchers.containsString("<service>txt</service>"));

        String snapFromXml = configs.get(1).getConfigSnapshot();
        org.junit.Assert.assertThat(snapFromXml, JUnitMatchers.containsString("<config>xml</config>"));

        Assert.assertEquals(configs.get(0).getCapabilities(), configs.get(1).getCapabilities());
    }

    @Test
    public void testInvalidXml() throws Exception {
        File resourcePath = FileTypeTest.getResourceAsFile("/bad_controller.xml.config");
        File parentFile = resourcePath.getParentFile();

        AutodetectDirectoryStorageAdapter adapter = new AutodetectDirectoryStorageAdapter();

        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",parentFile.getPath());
        AutodetectDirectoryPersister persister = (AutodetectDirectoryPersister) adapter.instantiate(pp);
        try {
            List<ConfigSnapshotHolder> configs = persister.loadLastConfigs();
            fail("An exception of type " + IllegalStateException.class + " was expected");
        } catch (IllegalStateException ise){
            String message = ise.getMessage();
            assertThat(message, JUnitMatchers.containsString("Unable to restore configuration snapshot from "));
        }

    }

    @Test
    public void testPersistConfig() throws Exception {
        File resourcePath = FileTypeTest.getResourceAsFile("/combined/1controller.txt.config");
        File parentFile = resourcePath.getParentFile();

        AutodetectDirectoryStorageAdapter adapter = new AutodetectDirectoryStorageAdapter();

        PropertiesProviderTest pp = new PropertiesProviderTest();
        pp.addProperty("directoryStorage",parentFile.getPath());
        AutodetectDirectoryPersister persister = (AutodetectDirectoryPersister) adapter.instantiate(pp);
        List<ConfigSnapshotHolder> configs = null;
        try {
            configs = persister.loadLastConfigs();
        } catch (IOException e) {
            fail("An exception of type " + UnsupportedOperationException.class + " was expected");
        }
        Assert.assertEquals(2, configs.size());
        try {
            persister.persistConfig(configs.get(0));
        } catch (UnsupportedOperationException uoe){
            String message = uoe.getMessage();
            assertThat(message,JUnitMatchers.containsString("This adapter is read only. Please set readonly=true on class"));
        }
    }

}
