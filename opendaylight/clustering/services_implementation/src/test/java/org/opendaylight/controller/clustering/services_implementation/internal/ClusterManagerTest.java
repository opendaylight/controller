package org.opendaylight.controller.clustering.services_implementation.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.infinispan.CacheImpl;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;

public class ClusterManagerTest {

    @Test
    public void noManagerSetTest() throws CacheExistException,
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
    public void withManagerTest() throws CacheExistException,
            CacheConfigException {

        ClusterManager cm = new ClusterManager();
        CacheImpl<String, Integer> c1 = null;
        CacheImpl<String, Integer> c2 = null;

        cm.start();

        // Check no cache created yet
        assertFalse(cm.existCache("NonExistantContainerName",
                "NonExistantCacheName"));

        String cacheName = "Cache1";
        String containerName = "Container1";
        // Create cache with no cacheMode set, expecting it to fail
        HashSet<cacheMode> cacheModeSet = new HashSet<cacheMode>();
        Assert.assertNull(cm.createCache(containerName,cacheName, cacheModeSet));

        // Create first cache as transactional
        cacheModeSet.add(cacheMode.TRANSACTIONAL);
        try {
            c1 = (CacheImpl<String, Integer>) cm.createCache(containerName,
                    cacheName, cacheModeSet);
        } catch (CacheExistException | CacheConfigException cce) {
           fail("Failed to create cache " + cacheName);
        }

        // Try creating exact same cache again
        try {
            c1 = (CacheImpl<String, Integer>) cm.createCache(containerName,
                    cacheName, cacheModeSet);
        } catch (CacheExistException cee) {

        } catch (CacheConfigException cce) {
            fail("Creating cache failed with " + cce);
        }

        // Create second cache with both types of cacheMode, expecting it to
        // complain
        String cacheName2 = "Cache2";
        cacheModeSet.add(cacheMode.NON_TRANSACTIONAL);
        try {
            c2 = (CacheImpl<String, Integer>) cm.createCache(containerName,
                    cacheName2, cacheModeSet);
        } catch (CacheExistException cee) {
            fail("Failed to create cache " + cacheName2 + cee);
        } catch (CacheConfigException cce) {

        }

        // Create second cache NON_TRANSACTIONAL but with both ASYNC and SYNC,
        // expect to complain
        cacheModeSet.remove(cacheMode.TRANSACTIONAL);
        cacheModeSet.add(cacheMode.SYNC);
        cacheModeSet.add(cacheMode.ASYNC);
        try {
            c2 = (CacheImpl<String, Integer>) cm.createCache(containerName, cacheName2, cacheModeSet);
        } catch (CacheExistException cee) {
            fail("Attempted to create cache " + cacheName2 + " with illegal cache modes set " + cacheModeSet);
        } catch (CacheConfigException cce) {

        }

        // Create second cache properly this time, as non_transactional and
        // ASYNC
        cacheModeSet.remove(cacheMode.SYNC);
        try {
            c2 = (CacheImpl<String, Integer>) cm.createCache(containerName,
                    cacheName2, cacheModeSet);
        } catch (CacheExistException | CacheConfigException e) {
            fail("Failed to create cache " + cacheName + " though it was supposed to succeed." + e);
        }

        // Make sure correct caches exists
        Assert.assertTrue(cm.existCache(containerName, cacheName));
        c1 = (CacheImpl<String, Integer>) cm.getCache(containerName, cacheName);
        Assert.assertNotNull(c1);

        Assert.assertTrue(cm.existCache(containerName, cacheName2));
        c2 = (CacheImpl<String, Integer>) cm.getCache(containerName, cacheName2);
        Assert.assertNotNull(c2);

        Assert.assertNull(cm.getCache(containerName, "Cache3"));

        // Get CacheList
        HashSet<String> cacheList = (HashSet<String>) cm
                .getCacheList("Container2");
        Assert.assertEquals(0, cacheList.size());

        cacheList = (HashSet<String>) cm.getCacheList(containerName);
        Assert.assertEquals(2, cacheList.size());
        Assert.assertTrue(cacheList.contains(cacheName));
        Assert.assertTrue(cacheList.contains(cacheName2));

        // Get CacheProperties
        Assert.assertNull(cm.getCacheProperties(containerName, ""));
        Properties p = cm.getCacheProperties(containerName, cacheName);
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
        cm.destroyCache(containerName, cacheName);
        cm.destroyCache(containerName, "Cache3");
        Assert.assertFalse(cm.existCache(containerName, cacheName));
        Assert.assertTrue(cm.existCache(containerName, cacheName2));

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
