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

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;
import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class ClusteringServicesIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(ClusteringServicesIntegrationTest.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private IClusterServices clusterServices = null;

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
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console",
                        "1.0.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util",
                        "1.0.400.v20120522-2049"),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services",
                        "3.3.100.v20120522-1822"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds",
                        "1.4.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell",
                        "0.8.0.v201110170705"),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api", "1.7.2"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.2"),
                mavenBundle("ch.qos.logback", "logback-core", "1.0.9"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.0.9"),
                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "clustering.services-implementation",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.services",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "sal.implementation", "0.4.0-SNAPSHOT"),
                /*mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub", "0.4.0-SNAPSHOT"),*/


                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec", "1.0.1.Final"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),
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

        ServiceReference r = bc.getServiceReference(IClusterServices.class
                .getName());
        if (r != null) {
            this.clusterServices = (IClusterServices) bc.getService(r);
        }
        assertNotNull(this.clusterServices);

    }

    @Test
    public void clusterTest() throws CacheExistException, CacheConfigException, CacheListenerAddException{
        
        String container1 = "Container1";
        String container2 = "Container2";
        String cache1 = "Cache1";
        String cache2 = "Cache2";
        String cache3 = "Cache3";
        
        HashSet<cacheMode> cacheModeSet = new HashSet<cacheMode>();
        cacheModeSet.add(cacheMode.NON_TRANSACTIONAL);
        ConcurrentMap cm11 = this.clusterServices.createCache(container1, cache1, cacheModeSet);
        assertNotNull(cm11);
        
        assertNull(this.clusterServices.getCache(container2, cache2));
        assertEquals(cm11, this.clusterServices.getCache(container1, cache1));
        
        assertFalse(this.clusterServices.existCache(container2, cache2));
        assertTrue(this.clusterServices.existCache(container1, cache1));
        
        ConcurrentMap cm12 = this.clusterServices.createCache(container1, cache2, cacheModeSet);
        ConcurrentMap cm23 = this.clusterServices.createCache(container2, cache3, cacheModeSet);

        HashSet<String> cacheList = (HashSet<String>) this.clusterServices.getCacheList(container1);
        assertEquals(2, cacheList.size());
        assertTrue(cacheList.contains(cache1));
        assertTrue(cacheList.contains(cache2));
        assertFalse(cacheList.contains(cache3));
        
        assertNotNull(this.clusterServices.getCacheProperties(container1, cache1));
        
        HashSet<IGetUpdates<?,?>> listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices.getListeners(container1, cache1);
        assertEquals(0, listeners.size());
        
        IGetUpdates<?,?> getUpdate1 = new GetUpdates();
        this.clusterServices.addListener(container1, cache1, getUpdate1);
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices.getListeners(container1, cache1);
        assertEquals(1, listeners.size());
        this.clusterServices.addListener(container1,cache1, new GetUpdates());
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices.getListeners(container1, cache1);
        assertEquals(2, listeners.size());
        
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices.getListeners(container2, cache3);
        assertEquals(0, listeners.size());

        this.clusterServices.removeListener(container1, cache1, getUpdate1);
        listeners = (HashSet<IGetUpdates<?, ?>>) this.clusterServices.getListeners(container1, cache1);
        assertEquals(1, listeners.size());
        
        
        InetAddress addr = this.clusterServices.getMyAddress();
        assertNotNull(addr);
        
        List<InetAddress> addrList = this.clusterServices.getClusteredControllers();
        
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
        public void entryUpdated(Integer key, String new_value,
                String containerName, String cacheName, boolean originLocal) {
            return;
        }

        @Override
        public void entryDeleted(Integer key, String containerName,
                String cacheName, boolean originLocal) {
            return;
        }
    }
}
