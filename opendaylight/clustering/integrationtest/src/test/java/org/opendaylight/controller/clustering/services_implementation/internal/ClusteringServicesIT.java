package org.opendaylight.controller.clustering.services_implementation.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;
import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.UpdateType;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

@RunWith(PaxExam.class)
public class ClusteringServicesIT {
    private Logger log = LoggerFactory
        .getLogger(ClusteringServicesIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private IClusterServices clusterServices = null;
    private IClusterContainerServices clusterDefaultServices = null;
    private IClusterGlobalServices clusterGlobalServices = null;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
            //
            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir()
                + "/src/test/resources/logback.xml"),
            // To start OSGi console for inspection remotely
            systemProperty("osgi.console").value("2401"),
            // Set the systemPackages (used by clustering)
            systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
            // List framework bundles
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.console").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.util").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.osgi.services").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.ds").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.command").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.runtime").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.shell").versionAsInProject(),
            // List logger bundles
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
            mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
            // List all the bundles on which the test case depends
            mavenBundle("org.opendaylight.controller",
                        "clustering.services").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "clustering.services-implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "sal.implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation").versionAsInProject(),
            mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
            mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager").versionAsInProject(),
            mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager.shell").versionAsInProject(),
            junitBundles());
    }

    private String stateToString(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "Not CONVERTED";
        }
    }

    @Before
    public void areWeReady() {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (int i = 0; i < b.length; i++) {
            int state = b[i].getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.debug("Bundle:" + b[i].getSymbolicName() + " state:"
                          + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is "
                      + "unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);

        this.clusterServices = (IClusterServices)ServiceHelper
            .getGlobalInstance(IClusterServices.class, this);
        assertNotNull(this.clusterServices);

        this.clusterDefaultServices = (IClusterContainerServices)ServiceHelper
            .getInstance(IClusterContainerServices.class, "default", this);
        assertNotNull(this.clusterDefaultServices);

        this.clusterGlobalServices = (IClusterGlobalServices)ServiceHelper
            .getGlobalInstance(IClusterGlobalServices.class, this);
        assertNotNull(this.clusterGlobalServices);
    }

    @Test
    public void clusterTest() throws CacheExistException, CacheConfigException,
        CacheListenerAddException {

        String container1 = "Container1";
        String container2 = "Container2";
        String cache1 = "Cache1";
        String cache2 = "Cache2";
        String cache3 = "Cache3";

        HashSet<cacheMode> cacheModeSet = new HashSet<cacheMode>();
        cacheModeSet.add(cacheMode.NON_TRANSACTIONAL);
        ConcurrentMap cm11 = this.clusterServices.createCache(container1,
                cache1, cacheModeSet);
        assertNotNull(cm11);

        assertNull(this.clusterServices.getCache(container2, cache2));
        assertEquals(cm11, this.clusterServices.getCache(container1, cache1));

        assertFalse(this.clusterServices.existCache(container2, cache2));
        assertTrue(this.clusterServices.existCache(container1, cache1));

        ConcurrentMap cm12 = this.clusterServices.createCache(container1,
                cache2, cacheModeSet);
        ConcurrentMap cm23 = this.clusterServices.createCache(container2,
                cache3, cacheModeSet);

        HashSet<String> cacheList = (HashSet<String>) this.clusterServices
                .getCacheList(container1);
        assertEquals(2, cacheList.size());
        assertTrue(cacheList.contains(cache1));
        assertTrue(cacheList.contains(cache2));
        assertFalse(cacheList.contains(cache3));

        assertNotNull(this.clusterServices.getCacheProperties(container1,
                cache1));

        HashSet<IGetUpdates<?, ?>> listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices
                .getListeners(container1, cache1);
        assertEquals(0, listeners.size());

        IGetUpdates<?, ?> getUpdate1 = new GetUpdates();
        this.clusterServices.addListener(container1, cache1, getUpdate1);
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices
                .getListeners(container1, cache1);
        assertEquals(1, listeners.size());
        this.clusterServices.addListener(container1, cache1, new GetUpdates());
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices
                .getListeners(container1, cache1);
        assertEquals(2, listeners.size());

        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices
                .getListeners(container2, cache3);
        assertEquals(0, listeners.size());

        this.clusterServices.removeListener(container1, cache1, getUpdate1);
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices
                .getListeners(container1, cache1);
        assertEquals(1, listeners.size());

        InetAddress addr = this.clusterServices.getMyAddress();
        assertNotNull(addr);

        List<InetAddress> addrList = this.clusterServices
                .getClusteredControllers();

        this.clusterServices.destroyCache(container1, cache1);
        assertFalse(this.clusterServices.existCache(container1, cache1));

    }

    private class GetUpdates implements IGetUpdates<Integer, String> {

        @Override
        public void entryCreated(Integer key, String containerName,
                String cacheName, boolean originLocal) {
            return;
        }

        @Override
        public void entryUpdated(Integer key, String newValue,
                String containerName, String cacheName, boolean originLocal) {
            return;
        }

        @Override
        public void entryDeleted(Integer key, String containerName,
                String cacheName, boolean originLocal) {
            return;
        }
    }

    @Test
    public void clusterContainerAndGlobalTest() throws CacheExistException, CacheConfigException,
        CacheListenerAddException, InterruptedException {
        String cache1 = "Cache1";
        String cache2 = "Cache2";
        // Lets test the case of caches with same name in different
        // containers (actually global an container case)
        String cache3 = "Cache2";

        HashSet<cacheMode> cacheModeSet = new HashSet<cacheMode>();
        cacheModeSet.add(cacheMode.NON_TRANSACTIONAL);
        ConcurrentMap cm11 = this.clusterDefaultServices.createCache(cache1, cacheModeSet);
        assertNotNull(cm11);

        assertTrue(this.clusterDefaultServices.existCache(cache1));
        assertEquals(cm11, this.clusterDefaultServices.getCache(cache1));

        ConcurrentMap cm12 = this.clusterDefaultServices.createCache(cache2, cacheModeSet);
        ConcurrentMap cm23 = this.clusterGlobalServices.createCache(cache3, cacheModeSet);

        // Now given cahe2 and cache3 have same name lets make sure
        // they don't return the same reference
        assertNotNull(this.clusterGlobalServices.getCache(cache2));
        // cm12 reference must be different than cm23
        assertTrue(cm12 != cm23);

        HashSet<String> cacheList = (HashSet<String>) this.clusterDefaultServices
            .getCacheList();
        assertEquals(2, cacheList.size());
        assertTrue(cacheList.contains(cache1));
        assertTrue(cacheList.contains(cache2));

        assertNotNull(this.clusterDefaultServices.getCacheProperties(cache1));

        {
            /***********************************/
            /* Testing cacheAware in Container */
            /***********************************/
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            Set<String> propSet = new HashSet<String>();
            propSet.add(cache1);
            propSet.add(cache2);
            props.put("cachenames", propSet);
            CacheAware listener = new CacheAware();
            CacheAware listenerRepeated = new CacheAware();
            ServiceRegistration updateServiceReg = ServiceHelper.registerServiceWReg(ICacheUpdateAware.class, "default",
                                                                                     listener, props);
            assertNotNull(updateServiceReg);

            // Register another service for the same caches, this
            // should not get any update because we don't allow to
            // override the existing unless before unregistered
            ServiceRegistration updateServiceRegRepeated = ServiceHelper.registerServiceWReg(ICacheUpdateAware.class,
                                                                                             "default",
                                                                                             listenerRepeated, props);
            assertNotNull(updateServiceRegRepeated);
            CountDownLatch res = null;
            List<Update> ups = null;
            Update up = null;
            Integer k1 = new Integer(10);
            Long k2 = new Long(100L);

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // Start monitoring the updates
            res = listener.restart(2);
            // modify the cache
            cm11.put(k1, "foo");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 2);
            // Validate that first we get an update (yes even in case of a
            // new value added)
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k1));
            assertTrue(up.value.equals("foo"));
            assertTrue(up.cacheName.equals(cache1));
            // Validate that we then get a create
            up = ups.get(1);
            assertTrue(up.t.equals(UpdateType.ADDED));
            assertTrue(up.key.equals(k1));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache1));

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm11.put(k1, "baz");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get an update with expect fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k1));
            assertTrue(up.value.equals("baz"));
            assertTrue(up.cacheName.equals(cache1));

            /**********************************/
            /* RE-UPDATE AN EXISTING KEY CASE */
            /**********************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm11.put(k1, "baz");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get an update with expect fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k1));
            assertTrue(up.value.equals("baz"));
            assertTrue(up.cacheName.equals(cache1));

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm11.remove(k1);
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get a delete with expected fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.REMOVED));
            assertTrue(up.key.equals(k1));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache1));

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // Start monitoring the updates
            res = listener.restart(2);
            // modify the cache
            cm12.put(k2, new Short((short)15));
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 2);
            // Validate that first we get an update (yes even in case of a
            // new value added)
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k2));
            assertTrue(up.value.equals(new Short((short)15)));
            assertTrue(up.cacheName.equals(cache2));
            // Validate that we then get a create
            up = ups.get(1);
            assertTrue(up.t.equals(UpdateType.ADDED));
            assertTrue(up.key.equals(k2));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache2));

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm12.put(k2, "BAZ");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get an update with expect fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k2));
            assertTrue(up.value.equals("BAZ"));
            assertTrue(up.cacheName.equals(cache2));

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm12.remove(k2);
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get a delete with expected fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.REMOVED));
            assertTrue(up.key.equals(k2));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache2));

            /******************************************************************/
            /* NOW LETS REMOVE THE REGISTRATION AND MAKE SURE NO UPDATS COMES */
            /******************************************************************/
            updateServiceReg.unregister();
            // Start monitoring the updates, noone should come in
            res = listener.restart(1);

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // modify the cache
            cm11.put(k1, "foo");

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // modify the cache
            cm11.put(k1, "baz");

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // modify the cache
            cm11.remove(k1);

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // modify the cache
            cm12.put(k2, new Short((short)15));

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // modify the cache
            cm12.put(k2, "BAZ");

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // modify the cache
            cm12.remove(k2);


            // Wait to make sure no updates came in, clearly this is
            // error prone as logic, but cannot find a better way than
            // this to make sure updates didn't get in
            res.await(1L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 0);
        }

        {
            /***********************************/
            /* Testing cacheAware in Global */
            /***********************************/
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            Set<String> propSet = new HashSet<String>();
            propSet.add(cache3);
            props.put("cachenames", propSet);
            CacheAware listener = new CacheAware();
            ServiceRegistration updateServiceReg = ServiceHelper.registerGlobalServiceWReg(ICacheUpdateAware.class,
                                                                                           listener, props);
            assertNotNull(updateServiceReg);

            CountDownLatch res = null;
            List<Update> ups = null;
            Update up = null;
            Integer k1 = new Integer(10);

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // Start monitoring the updates
            res = listener.restart(2);
            // modify the cache
            cm23.put(k1, "foo");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 2);
            // Validate that first we get an update (yes even in case of a
            // new value added)
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k1));
            assertTrue(up.value.equals("foo"));
            assertTrue(up.cacheName.equals(cache3));
            // Validate that we then get a create
            up = ups.get(1);
            assertTrue(up.t.equals(UpdateType.ADDED));
            assertTrue(up.key.equals(k1));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache3));

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm23.put(k1, "baz");
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get an update with expect fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.CHANGED));
            assertTrue(up.key.equals(k1));
            assertTrue(up.value.equals("baz"));
            assertTrue(up.cacheName.equals(cache3));

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // Start monitoring the updates
            res = listener.restart(1);
            // modify the cache
            cm23.remove(k1);
            // Wait
            res.await(100L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 1);
            // Validate we get a delete with expected fields
            up = ups.get(0);
            assertTrue(up.t.equals(UpdateType.REMOVED));
            assertTrue(up.key.equals(k1));
            assertNull(up.value);
            assertTrue(up.cacheName.equals(cache3));

            /******************************************************************/
            /* NOW LETS REMOVE THE REGISTRATION AND MAKE SURE NO UPDATS COMES */
            /******************************************************************/
            updateServiceReg.unregister();
            // Start monitoring the updates, noone should come in
            res = listener.restart(1);

            /***********************/
            /* CREATE NEW KEY CASE */
            /***********************/
            // modify the cache
            cm23.put(k1, "foo");

            /*******************************/
            /* UPDATE AN EXISTING KEY CASE */
            /*******************************/
            // modify the cache
            cm23.put(k1, "baz");

            /********************************/
            /* REMOVAL OF EXISTING KEY CASE */
            /********************************/
            // modify the cache
            cm23.remove(k1);

            // Wait to make sure no updates came in, clearly this is
            // error prone as logic, but cannot find a better way than
            // this to make sure updates didn't get in
            res.await(1L, TimeUnit.SECONDS);
            // Analyze the updates
            ups = listener.getUpdates();
            assertTrue(ups.size() == 0);
        }

        InetAddress addr = this.clusterDefaultServices.getMyAddress();
        assertNotNull(addr);

        List<InetAddress> addrList = this.clusterDefaultServices
            .getClusteredControllers();

        this.clusterDefaultServices.destroyCache(cache1);
        assertFalse(this.clusterDefaultServices.existCache(cache1));
    }

    private class Update {
        Object key;
        Object value;
        String cacheName;
        UpdateType t;

        Update (UpdateType t, Object key, Object value, String cacheName) {
            this.t = t;
            this.key = key;
            this.value = value;
            this.cacheName = cacheName;
        }
    }

    private class CacheAware implements ICacheUpdateAware {
        private CopyOnWriteArrayList<Update> gotUpdates;
        private CountDownLatch latch = null;

        CacheAware() {
            this.gotUpdates = new CopyOnWriteArrayList<Update>();
        }


        /**
         * Restart the monitor of the updates on the CacheAware object
         *
         * @param expectedOperations Number of expected updates
         *
         * @return a countdown latch which will be used to wait till the updates are done
         */
        CountDownLatch restart(int expectedOperations) {
            this.gotUpdates.clear();
            this.latch = new CountDownLatch(expectedOperations);
            return this.latch;
        }

        List<Update> getUpdates() {
            return this.gotUpdates;
        }

        @Override
        public void entryCreated(Object key, String cacheName, boolean originLocal) {
            log.debug("CACHE[{}] Got an entry created for key:{}", cacheName, key);
            Update u = new Update(UpdateType.ADDED, key, null, cacheName);
            this.gotUpdates.add(u);
            this.latch.countDown();
        }

        @Override
        public void entryUpdated(Object key, Object newValue, String cacheName, boolean originLocal) {
            log.debug("CACHE[{}] Got an entry updated for key:{} newValue:{}", cacheName, key, newValue);
            Update u = new Update(UpdateType.CHANGED, key, newValue, cacheName);
            this.gotUpdates.add(u);
            this.latch.countDown();
        }

        @Override
        public void entryDeleted(Object key, String cacheName, boolean originLocal) {
            log.debug("CACHE[{}] Got an entry delete for key:{}", cacheName, key);
            Update u = new Update(UpdateType.REMOVED, key, null, cacheName);
            this.gotUpdates.add(u);
            this.latch.countDown();
        }
    }
}
