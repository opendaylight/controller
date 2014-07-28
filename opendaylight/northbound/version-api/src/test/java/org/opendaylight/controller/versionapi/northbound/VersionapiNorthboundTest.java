package org.opendaylight.controller.versionapi.northbound;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
import java.util.ArrayList;
import junit.framework.Assert;
import org.junit.Test;

public class VersionapiNorthboundTest {

    @Test
    public void testGetVersion() throws Exception {
        ConfigUpdater cu = new ConfigUpdater();
        Version result = cu.getVersion();
        Assert.assertEquals(null, result);
    }

    @Test
    public void testGetVersions() throws Exception {
        ConfigUpdater cu = new ConfigUpdater();
        ArrayList<Version> result = cu.getVersions();
        ArrayList<Version> vs = new ArrayList<Version>();
        vs.add(null);
        Assert.assertEquals(vs, result);
    }
/*
    @Test
    public void testGetVersionApi() throws Exception {
        VersionapiNorthbound v = new VersionapiNorthbound();
        ArrayList<Version> result = v.getVersion();
        ArrayList<Version> expected = new ArrayList<Version>();
        expected.add(null);
        Assert.assertEquals(expected, result);
    }
*/
}