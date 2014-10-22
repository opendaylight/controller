/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import org.junit.Assert;
import org.junit.Test;

public class DataStoreUtilsTest {

    @Test
    public void testDoPathsCoExist() {
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist(null, ""));
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist("", null));
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist("", ""));

        String path1 = "akka://test/user/$a";
        String path2 = "akka://test/user/$b";
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist(path1, path2));

        path1 = "akka.tcp://system@127.0.0.1:2550/";
        path2 = "akka.tcp://system@127.0.0.1:2550/";
        Assert.assertTrue(DataStoreUtils.getInstance().doPathsCoExist(path1, path2));

        path1 = "akka.tcp://system@127.0.0.1:2550/";
        path2 = "akka.tcp://system@128.0.0.1:2550/";
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist(path1, path2));

        path1 = "akka.tcp://system@127.0.0.1:2550/";
        path2 = "akka.tcp://system@127.0.0.1:2551/";
        Assert.assertFalse(DataStoreUtils.getInstance().doPathsCoExist(path1, path2));

    }
}
