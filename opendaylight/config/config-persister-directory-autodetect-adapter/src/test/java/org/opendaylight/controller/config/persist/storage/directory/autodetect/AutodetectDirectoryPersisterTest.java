/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.autodetect;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;

import java.io.File;
import java.util.List;

public class AutodetectDirectoryPersisterTest {

    @Test
    public void testCombined() throws Exception {
        File resourcePath = FileTypeTest.getResourceAsFile("/combined/1controller.txt.config");

        AutodetectDirectoryPersister persister = new AutodetectDirectoryPersister(resourcePath.getParentFile());
        List<ConfigSnapshotHolder> configs = persister.loadLastConfigs();

        Assert.assertEquals(2, configs.size());
        String snapFromTxt = configs.get(0).getConfigSnapshot();
        org.junit.Assert.assertThat(snapFromTxt, JUnitMatchers.containsString("<config>txt</config>"));
        org.junit.Assert.assertThat(snapFromTxt, JUnitMatchers.containsString("<service>txt</service>"));

        String snapFromXml = configs.get(1).getConfigSnapshot();
        org.junit.Assert.assertThat(snapFromXml, JUnitMatchers.containsString("<config>xml</config>"));

        Assert.assertEquals(configs.get(0).getCapabilities(), configs.get(1).getCapabilities());
    }
}
