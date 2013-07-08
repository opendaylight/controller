/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.UnknownHostException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.Capabilities.CapabilitiesType;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.TimeStamp;
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

@RunWith(PaxExam.class)
public class SwitchManagerIT {
    private final Logger log = LoggerFactory.getLogger(SwitchManagerIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private ISwitchManager switchManager = null;

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
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                                "protocol_plugins.stub").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3")
                        .versionAsInProject(),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager")
                        .versionAsInProject(), junitBundles());
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

        // Now lets create a hosttracker for testing purpose
        ServiceReference s = bc.getServiceReference(ISwitchManager.class
                .getName());
        if (s != null) {
            this.switchManager = (ISwitchManager) bc.getService(s);
        }

        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.switchManager);
    }

    @Test
    public void testNodeProp() throws UnknownHostException {
        assertNotNull(this.switchManager);

        Node node;
        try {
            node = new Node("STUB", new Integer(0xCAFE));
        } catch (ConstructionException e) {
            // test failed if node cannot be created.
            node = null;
            Assert.assertTrue(false);
        }
        Map<String, Property> propMap = this.switchManager.getNodeProps(node);
        Assert.assertFalse(propMap.isEmpty());

        Assert.assertTrue(this.switchManager.getNodeProp(node,
                Capabilities.CapabilitiesPropName).equals(
                new Capabilities(3)));
        Assert.assertTrue(this.switchManager.getNodeProp(node,
                Actions.ActionsPropName).equals(new Actions(2)));
        Assert.assertTrue(this.switchManager.getNodeProp(node,
                Buffers.BuffersPropName).equals(new Buffers(1)));
        Assert.assertTrue(this.switchManager.getNodeProp(node,
                TimeStamp.TimeStampPropName).equals(
                new TimeStamp(100000L, "connectedSince")));
    }

    @Test
    public void testNodeConnectorProp() throws UnknownHostException {
        assertNotNull(this.switchManager);
        Node node;
        NodeConnector nc;
        try {
            node = new Node("STUB", 0xCAFE);
            nc = new NodeConnector("STUB", 0xCAFE, node);
        } catch (ConstructionException e) {
            node = null;
            nc = null;
            Assert.assertTrue(false);
        }
        Map<String, Property> propMap = this.switchManager
                .getNodeConnectorProps(nc);
        Assert.assertFalse(propMap.isEmpty());

        Assert.assertTrue(this.switchManager.getNodeConnectorProp(nc,
                Capabilities.CapabilitiesPropName).equals(
                new Capabilities(CapabilitiesType.FLOW_STATS_CAPABILITY
                        .getValue())));
        Assert.assertTrue(this.switchManager.getNodeConnectorProp(nc,
                Bandwidth.BandwidthPropName).equals(
                new Bandwidth(Bandwidth.BW1Gbps)));
        Assert.assertTrue(this.switchManager.getNodeConnectorProp(nc,
                State.StatePropName).equals(new State(State.EDGE_UP)));
    }
}
