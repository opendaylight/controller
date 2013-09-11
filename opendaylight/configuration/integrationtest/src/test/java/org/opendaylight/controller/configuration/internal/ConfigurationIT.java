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
public class ConfigurationIT {
    private final Logger logger = LoggerFactory
            .getLogger(ConfigurationIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
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
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("eclipselink", "javax.resource").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "protocol_plugins.stub").versionAsInProject(),
                // needed bundles by switchmanager
                mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager.it.implementation").versionAsInProject(),
                // needed bundles by configuration
                mavenBundle("org.opendaylight.controller", "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration.implementation").versionAsInProject(),
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
        File f = new File(GlobalConstants.STARTUPHOME.toString());
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
