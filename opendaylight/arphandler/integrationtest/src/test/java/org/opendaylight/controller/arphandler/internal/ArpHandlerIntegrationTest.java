/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.InetAddress;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.arphandler.IArpHandler;
import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
@RunWith(PaxExam.class)
public class ArpHandlerIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(ArpHandlerIntegrationTest.class);

    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private ISwitchManager switchManager = null;

    private IfIptoHost hostTracker = null;

    private IfHostListener hostListener = null;

    IArpHandler arpHandler = null;

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

                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "arphandler.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation", "0.4.0-SNAPSHOT"),

                // needed by hosttracker
                mavenBundle("org.opendaylight.controller", "topologymanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation", "0.4.0-SNAPSHOT"),
                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.5.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "sal.implementation", "0.4.0-SNAPSHOT"),

                // needed by statisticsmanager
                mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation", "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller",
                        "clustering.services", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.stub",
                        "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub", "0.4.0-SNAPSHOT"),
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
            log.debug("Do some debugging because some bundle is unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);

        // Now lets create a switch manager for testing purpose
        ServiceReference s = bc.getServiceReference(ISwitchManager.class
                .getName());
        if (s != null) {
            switchManager = (ISwitchManager) bc.getService(s);
        }
        assertNotNull("ISwitchManager is Null", switchManager);

        // If HostTracker is not found, we can't proceed
        s = bc.getServiceReference(IfIptoHost.class.getName());
        if (s != null) {
            hostTracker = (IfIptoHost) bc.getService(s);
        }
        assertNotNull("IfIptoHost is Null", hostTracker);

        if (hostTracker instanceof IfHostListener) {
            hostListener = (IfHostListener) hostTracker;
        } else {
            // implementation is not HostTracker!
            s = bc.getServiceReference(IfHostListener.class.getName());
            if (s != null) {
                hostListener = (IfHostListener) bc.getService(s);
            }
        }
        assertNotNull("IfHostListener is Null", hostListener);

        // ArpHandler service instance must be registered
        s = bc.getServiceReference(IArpHandler.class.getName());
        if (s != null) {
            arpHandler = (IArpHandler) bc.getService(s);
        }
        assertNotNull("IArpHandler is Null", arpHandler);

    }

    /**
     * Test find() and probe() functions of IArpHandler (IHostFinder). To avoid
     * being environment dependent, for now only use the localhost, which should
     * never fail due to being unknown.
     * <p/>
     * NOTE: these two functions are likely to have been used when testing the
     * HostTracker module.
     */
    @Test
    public void testFindProbe() {

        InetAddress localAddr = null;
        try {
            localAddr = InetAddress.getLocalHost();
            arpHandler.find(localAddr);
        } catch (Exception e) {
            // e.printStackTrace();
            fail("Exception in arpHandler.find(" + localAddr + "): "
                    + e.getMessage());
        }

        HostNodeConnector nodeC = null;
        try {
            nodeC = new HostNodeConnector(localAddr);
            arpHandler.probe(nodeC);
        } catch (Exception e) {
            // e.printStackTrace();
            fail("Exception in arpHandler.probe(" + nodeC + "): "
                    + e.getMessage());
        }
    }
}
