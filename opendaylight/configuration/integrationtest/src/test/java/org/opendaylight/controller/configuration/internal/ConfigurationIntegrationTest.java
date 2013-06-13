package org.opendaylight.controller.configuration.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
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
public class ConfigurationIntegrationTest {
    private Logger logger = LoggerFactory
            .getLogger(ConfigurationIntegrationTest.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private IClusterServices clusterService = null;
    private IConfigurationService configService = null;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
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
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec", "1.0.1.Final"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),
                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.5.0-SNAPSHOT"), // SAL connects the protocols
                                           // plug-ins to other stuff
                mavenBundle("org.opendaylight.controller",
                        "sal.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub", "0.4.0-SNAPSHOT"),
                // needed bundles by switchmanager
                mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation", "0.4.0-SNAPSHOT"),
                // needed bundles by configuration
                mavenBundle("org.opendaylight.controller",
                        "clustering.services", "0.4.0-SNAPSHOT"), // what are
                                                                  // the
                                                                  // clustering
                                                                  // services
                                                                  // for
                mavenBundle("org.opendaylight.controller", "clustering.stub",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services-implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation", "0.4.0-SNAPSHOT"),
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
    public void areWeReadyForClustering() {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (int i = 0; i < b.length; i++) {
            int state = b[i].getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                logger.debug("Bundle:" + b[i].getSymbolicName() + " state:"
                        + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            logger.debug("Do some debugging because some bundle is unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);
        ServiceReference r = bc.getServiceReference(IClusterServices.class
                .getName());
        if (r != null) {
            this.clusterService = (IClusterServices) bc.getService(r);
        }
        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.clusterService);

    }

    @Before
    public void areWeReadyForConfiguration() {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (int i = 0; i < b.length; i++) {
            int state = b[i].getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                logger.debug("Bundle:" + b[i].getSymbolicName() + " state:"
                        + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            logger.debug("Do some debugging because some bundle is unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);
        ServiceReference r = bc.getServiceReference(IConfigurationService.class
                .getName());
        if (r != null) {
            this.configService = (IConfigurationService) bc.getService(r);
        }
        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.configService);

    }

    @Test
    public void saveConfiguration() {
        File f = new File(GlobalConstants.STARTUPHOME.name());
        boolean success = f.exists();
        if (!success) {
            success = f.mkdirs();
        }
        if(success){
            Status status = new Status(StatusCode.SUCCESS, null);
            if (this.configService != null) {
                status = this.configService.saveConfigurations();
            }
            if (status.getCode().equals(StatusCode.SUCCESS)) {
                Assert.assertTrue("Successfully saved config for "
                        + configService.getClass().getSimpleName(), status
                        .getCode().equals(StatusCode.SUCCESS));
            } else {
                Assert.assertFalse("Failed to save config for "
                        + configService.getClass().getSimpleName(), status
                        .getCode().equals(StatusCode.INTERNALERROR));
            }
        }
    }

}