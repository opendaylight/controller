
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;
import javax.inject.Inject;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Capabilities.CapabilitiesType;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.switchmanager.*;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.switchmanager.ISwitchManager;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import static org.junit.Assert.*;
import org.ops4j.pax.exam.junit.Configuration;
import static org.ops4j.pax.exam.CoreOptions.*;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
public class SwitchmanagerIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(SwitchmanagerIntegrationTest.class);
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
                mavenBundle("org.slf4j", "slf4j-api", "1.7.2"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.2"),
                mavenBundle("ch.qos.logback", "logback-core", "1.0.9"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.0.9"),

                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "sal.implementation",
                        "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller", "containermanager.implementation",
                       "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller", "clustering.services",
                       "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller", "clustering.services-implementation",
                       "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration.implementation", 
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
            log.debug("Do some debugging because some bundle is "
                    + "unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);

         // Now lets create a hosttracker for testing purpose
         ServiceReference s = bc
             .getServiceReference(ISwitchManager.class.getName());
         if (s != null) {
                 this.switchManager = (ISwitchManager)bc.getService(s);
         }

         // If StatisticsManager is null, cannot run tests.
         assertNotNull(this.switchManager);
    }
    
    
    @Test
    public void testNodeProp() throws UnknownHostException {
        assertNotNull(this.switchManager);

        Node node = NodeCreator.createOFNode((long)2);
        Map<String, Property> propMap = this.switchManager.getNodeProps(node);
        Assert.assertFalse(propMap.isEmpty());

        Assert.assertTrue(this.switchManager.getNodeProp
                (node, Capabilities.CapabilitiesPropName)
                .equals(new Capabilities((int)3)));
        Assert.assertTrue(this.switchManager.getNodeProp
                (node, Actions.ActionsPropName)
                .equals(new Actions((int)2)));
        Assert.assertTrue(this.switchManager.getNodeProp
                (node, Buffers.BuffersPropName)
                .equals(new Buffers((int)1)));
        Assert.assertTrue(this.switchManager.getNodeProp
                (node, TimeStamp.TimeStampPropName)
                .equals(new TimeStamp(100000L, "connectedSince")));
    }               
            

    @Test
    public void testNodeConnectorProp() throws UnknownHostException {
        assertNotNull(this.switchManager);

        NodeConnector nc = NodeConnectorCreator.createOFNodeConnector
                ((short)2, NodeCreator.createOFNode((long)3));
        Map<String, Property> propMap = this.switchManager.getNodeConnectorProps(nc);
        Assert.assertFalse(propMap.isEmpty());

        Assert.assertTrue(this.switchManager.getNodeConnectorProp
                (nc, Capabilities.CapabilitiesPropName)
                .equals(new Capabilities 
                		(CapabilitiesType.FLOW_STATS_CAPABILITY.getValue())));
        Assert.assertTrue(this.switchManager.getNodeConnectorProp
                (nc, Bandwidth.BandwidthPropName)
                .equals(new Bandwidth (Bandwidth.BW1Gbps)));
        Assert.assertTrue(this.switchManager.getNodeConnectorProp
                (nc, State.StatePropName)
                .equals(new State (State.EDGE_UP)));
    }               
}
