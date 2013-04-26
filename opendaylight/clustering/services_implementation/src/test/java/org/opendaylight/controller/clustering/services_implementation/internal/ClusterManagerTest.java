package org.opendaylight.controller.clustering.services_implementation.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.CacheImpl;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;

public class ClusterManagerTest {

    @Test
    public void NoManagerSetTest() throws CacheExistException,
            CacheConfigException {
        ClusterManager cm = new ClusterManager();
        CacheImpl<String, Integer> c1 = null;
        CacheImpl<String, Integer> c2 = null;
        Assert.assertNull(cm.createCache("Container", "Cache", null));
        Assert.assertNull(cm.getCacheProperties("Container", "Cache"));
        Assert.assertNull(cm.getCache("Container", "Cache"));
        Assert.assertFalse(cm.existCache("Container", "Cache"));
        Assert.assertNull(cm.getCacheList("Container"));
        Assert.assertTrue(cm.amIStandby());
        Assert.assertNull(cm.getActiveAddress());
        Assert.assertNull(cm.getMyAddress());
        Assert.assertNull(cm.getClusteredControllers());
    }

    @Test
    public void WithManagerTest() throws CacheExistException,
            CacheConfigException {

        ClusterManager cm = new ClusterManager();
        CacheImpl<String, Integer> c1 = null;
        CacheImpl<String, Integer> c2 = null;

        cm.start();

        // Check no cache created yet
        assertFalse(cm.existCache("NonExistantContainerName",
                "NonExistantCacheName"));

        // Create cache with no cacheMode set, expecting it to fail
        HashSet<cacheMode> cacheModeSet = new HashSet<cacheMode>();
        Assert.assertNull(cm.createCache("Container1", "Cache1", cacheModeSet));

        // Create first cache as transactional
        cacheModeSet.add(cacheMode.TRANSACTIONAL);
        try {
            c1 = (CacheImpl<String, Integer>) cm.createCache("Container1",
                    "Cache1", cacheModeSet);
        } catch (CacheExistException cee) {
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            Assert.assertTrue(false);
        }

        // Try creating exact same cache again
        try {
            c1 = (CacheImpl<String, Integer>) cm.createCache("Container1",
                    "Cache1", cacheModeSet);
        } catch (CacheExistException cee) {
            Assert.assertTrue(true);
        } catch (CacheConfigException cce) {
            Assert.assertTrue(false);
        }

        // Create second cache with both types of cacheMode, expecting it to
        // complain
        cacheModeSet.add(cacheMode.NON_TRANSACTIONAL);
        try {
            c2 = (CacheImpl<String, Integer>) cm.createCache("Container1",
                    "Cache2", cacheModeSet);
        } catch (CacheExistException cee) {
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            Assert.assertTrue(true);
        }

        // Create second cache properly this time, as non_transactional
        cacheModeSet.remove(cacheMode.TRANSACTIONAL);
        try {
            c2 = (CacheImpl<String, Integer>) cm.createCache("Container1",
                    "Cache2", cacheModeSet);
        } catch (CacheExistException cee) {
            Assert.assertTrue(false);
        } catch (CacheConfigException cce) {
            Assert.assertTrue(false);
        }

        // Make sure correct caches exists
        Assert.assertTrue(cm.existCache("Container1", "Cache1"));
        c1 = (CacheImpl<String, Integer>) cm.getCache("Container1", "Cache1");
        Assert.assertTrue(c1 != null);

        Assert.assertTrue(cm.existCache("Container1", "Cache2"));
        c2 = (CacheImpl<String, Integer>) cm.getCache("Container1", "Cache2");
        Assert.assertTrue(c2 != null);

        Assert.assertNull(cm.getCache("Container1", "Cache3"));

        // Get CacheList
        HashSet<String> cacheList = (HashSet<String>) cm
                .getCacheList("Container2");
        Assert.assertEquals(0, cacheList.size());

        cacheList = (HashSet<String>) cm.getCacheList("Container1");
        Assert.assertEquals(2, cacheList.size());
        Assert.assertTrue(cacheList.contains("Cache1"));
        Assert.assertTrue(cacheList.contains("Cache2"));

        // Get CacheProperties
        Assert.assertNull(cm.getCacheProperties("Container1", ""));
        Properties p = cm.getCacheProperties("Container1", "Cache1");
        Assert.assertEquals(3, p.size());
        Assert.assertNotNull(p
                .getProperty(IClusterServices.cacheProps.TRANSACTION_PROP
                        .toString()));
        Assert.assertNotNull(p
                .getProperty(IClusterServices.cacheProps.CLUSTERING_PROP
                        .toString()));
        Assert.assertNotNull(p
                .getProperty(IClusterServices.cacheProps.LOCKING_PROP
                        .toString()));

        // Destroy cache1 and make sure it's gone
        cm.destroyCache("Container1", "Cache1");
        cm.destroyCache("Container1", "Cache3");
        Assert.assertFalse(cm.existCache("Container1", "Cache1"));
        Assert.assertTrue(cm.existCache("Container1", "Cache2"));

        // Check amIStandBy()
        boolean standby = cm.amIStandby();
        Assert.assertFalse(standby);

        // Check addresses, which are all loopback
        InetAddress activeAddress = cm.getActiveAddress();
        Assert.assertEquals("/127.0.0.1", activeAddress.toString());
        InetAddress myAddress = cm.getMyAddress();
        Assert.assertEquals("/127.0.0.1", myAddress.toString());

        List<InetAddress> cc = cm.getClusteredControllers();
        Assert.assertEquals(0, cc.size());

        cm.stop();
    }

}
