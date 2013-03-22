
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   TestClusteringStub.java
 *
 * @brief  Unit tests for the stub implementation of clustering,
 * needed only to run the integration tests
 *
 * Unit tests for the stub implementation of clustering,
 * needed only to run the integration tests
 */
package org.opendaylight.controller.clustering.stub.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.stub.internal.ClusterGlobalManager;

public class TestClusteringStub {
    @Test
    public void testStub1() {
        IClusterGlobalServices c = null;
        ClusterGlobalManager cm = null;
        try {
            cm = new ClusterGlobalManager();
            c = (IClusterGlobalServices) cm;
        } catch (UnknownHostException un) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }

        // Make sure the stub cluster manager is allocated
        Assert.assertTrue(cm != null);
        Assert.assertTrue(c != null);

        // ========================================
        // Now start testing the several aspects of it.
        // ========================================

        // Allocate few caches
        ConcurrentMap<String, Integer> c1 = null;
        ConcurrentMap<String, Integer> c2 = null;
        ConcurrentMap<String, Integer> c3 = null;
        try {
            c1 = (ConcurrentMap<String, Integer>) c.createCache("c1", null);
        } catch (CacheExistException cee) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }

        // Put some data to it
        c1.put("FOO", 1);
        c1.put("BAZ", 2);
        c1.put("BAR", 3);

        try {
            c1 = (ConcurrentMap<String, Integer>) c.createCache("c1", null);
        } catch (CacheExistException cee) {
            // This exception should be raised because the cache
            // already exists
            Assert.assertTrue(true);
        } catch (CacheConfigException cce) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }

        // Make sure this cache is retrieved
        c1 = (ConcurrentMap<String, Integer>) c.getCache("c1");
        Assert.assertTrue(c1 != null);

        // Now make sure the data exists
        Integer res = null;
        res = c1.get("FOO");
        Assert.assertTrue(res != null);
        res = c1.get("BAR");
        Assert.assertTrue(res != null);
        res = c1.get("BAZ");
        Assert.assertTrue(res != null);

        // Now create yet another two caches
        try {
            c2 = (ConcurrentMap<String, Integer>) c.createCache("c2", null);
            c3 = (ConcurrentMap<String, Integer>) c.createCache("c3", null);
        } catch (CacheExistException cee) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }

        // Make sure the caches exist
        Assert.assertTrue(c2 != null);
        Assert.assertTrue(c3 != null);

        // Put some fake data
        c2.put("FOO", 11);
        c2.put("BAZ", 22);
        c2.put("BAR", 33);

        c3.put("FOOBAR", 110);

        // Test for cache existance
        Assert.assertTrue(c.existCache("c1"));
        Assert.assertTrue(c.existCache("c2"));
        Assert.assertTrue(c.existCache("c3"));

        // Get the Cache List
        Set<String> caches = c.getCacheList();
        Assert.assertTrue(caches != null);

        // Check if the cachelist is correct
        System.out.println("cache size:" + caches.size());
        Assert.assertTrue(caches.size() == 3);
        Assert.assertTrue(caches.contains("c1"));
        Assert.assertTrue(caches.contains("c2"));
        Assert.assertTrue(caches.contains("c3"));

        // Check that the utility API for the cluster are working too
        Assert.assertTrue(c.getCoordinatorAddress() != null);
        Assert.assertTrue(c.getClusteredControllers() != null);
        // This a one man-show
        Assert.assertTrue(c.getClusteredControllers().size() == 1);
        Assert.assertTrue(c.getMyAddress() != null);
        // Make sure i'm the coordinator
        Assert.assertTrue(c.amICoordinator());

        // Now destroy some caches make sure they are gone
        c.destroyCache("c1");
        Assert.assertTrue(!c.existCache("c1"));
        caches = c.getCacheList();
        Assert.assertTrue(caches.size() == 2);

        // Now recreate the cache, make sure a different one is
        // retrieved, which should be empty
        try {
            c1 = (ConcurrentMap<String, Integer>) c.createCache("c1", null);
        } catch (CacheExistException cee) {
            // This exception should be raised because the cache
            // already exists
            Assert.assertTrue(true);
        } catch (CacheConfigException cce) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }
        c1 = (ConcurrentMap<String, Integer>) c.getCache("c1");
        Assert.assertTrue(c1 != null);
        Assert.assertTrue(c1.keySet().size() == 0);
        caches = c.getCacheList();
        Assert.assertTrue(caches.size() == 3);

        // Now destroy the cache manager and make sure things are
        // clean
        cm.destroy();
        caches = c.getCacheList();
        Assert.assertTrue(caches.size() == 0);

        // Now to re-create two caches and make sure they exists, but
        // are different than in previous life
        try {
            c2 = (ConcurrentMap<String, Integer>) c.createCache("c2", null);
            c3 = (ConcurrentMap<String, Integer>) c.createCache("c3", null);
        } catch (CacheExistException cee) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            // Don't expect this assertion, so if happens signal a
            // failure in the testcase
            Assert.assertTrue(false);
        }
        Assert.assertTrue(c2 != null);
        Assert.assertTrue(c3 != null);
        caches = c.getCacheList();
        Assert.assertTrue(caches.size() == 2);
        Assert.assertTrue(c2.keySet().size() == 0);
        Assert.assertTrue(c3.keySet().size() == 0);
    }
}
