
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.internal;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
//import org.opendaylight.controller.hosttracker.*;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;

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
public class HostTrackerIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(HostTrackerIntegrationTest.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    
    private IfIptoHost hosttracker = null;
    private ISwitchManagerAware switchManagerAware = null;
    private IInventoryListener invtoryListener = null;
    private IfHostListener hostListener = null;
    private ITopologyManagerAware topologyManagerAware = null;
    
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
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.5.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "sal.implementation",
                        "0.4.0-SNAPSHOT"),

               // needed by statisticsmanager
               mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller", "containermanager.implementation",
                       "0.4.0-SNAPSHOT"),
               
               mavenBundle("org.opendaylight.controller",
                        "clustering.services", "0.4.0-SNAPSHOT"),
               mavenBundle("org.opendaylight.controller",
                        "clustering.stub", "0.4.0-SNAPSHOT"),

                // needed by forwardingrulesmanager
                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker.implementation",
                        "0.4.0-SNAPSHOT"),

                // needed by hosttracker
                mavenBundle("org.opendaylight.controller", "topologymanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),


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
             .getServiceReference(IfIptoHost.class.getName());
         if (s != null) {
                 this.hosttracker = (IfIptoHost)bc.getService(s);
                 this.switchManagerAware = (ISwitchManagerAware) this.hosttracker;
                 this.invtoryListener = (IInventoryListener) this.hosttracker;
                 this.hostListener = (IfHostListener) this.hosttracker;
                 this.topologyManagerAware = (ITopologyManagerAware) this.hosttracker;
         }

         // If StatisticsManager is null, cannot run tests.
         assertNotNull(this.hosttracker);
    }
    
    
    @Test
    public void testStaticHost() throws UnknownHostException {
        String ip;
        
        assertNotNull(this.hosttracker);
        
        // create one node and two node connectors
        Node node1 = NodeCreator.createOFNode(1L);
        NodeConnector nc1_1 = NodeConnectorCreator.createOFNodeConnector((short) 1, node1);
        NodeConnector nc1_2 = NodeConnectorCreator.createOFNodeConnector((short) 2, node1);
        
        // test addStaticHost(), store into inactive host DB 
        Status st = this.hosttracker.addStaticHost("192.168.0.8", "11:22:33:44:55:66",
                nc1_1, "0");
        Assert.assertTrue(st.isSuccess());
        st = this.hosttracker.addStaticHost("192.168.0.13", "11:22:33:44:55:77",
                nc1_2, "0");
        Assert.assertTrue(st.isSuccess());
        
        // check inactive DB
        Iterator<HostNodeConnector> hnci = this.hosttracker.getInactiveStaticHosts().iterator();
        while (hnci.hasNext()) {
            ip = hnci.next().getNetworkAddressAsString();
            Assert.assertTrue(ip.equals("192.168.0.8") || ip.equals("192.168.0.13"));
        }
        
        // check active host DB
        hnci = this.hosttracker.getActiveStaticHosts().iterator();
        Assert.assertFalse(hnci.hasNext());
        
        // test removeStaticHost()
        st = this.hosttracker.removeStaticHost("192.168.0.8");
        Assert.assertTrue(st.isSuccess());

        hnci = this.hosttracker.getInactiveStaticHosts().iterator();
        while (hnci.hasNext()) {
            ip = hnci.next().getNetworkAddressAsString();
            Assert.assertTrue(ip.equals("192.168.0.13"));
        }
    }               
            

    
    @Test
    public void testNotifyNodeConnector() throws UnknownHostException {
        String ip;
        
        assertNotNull(this.invtoryListener);

        // create one node and two node connectors
        Node node1 = NodeCreator.createOFNode(1L);
        NodeConnector nc1_1 = NodeConnectorCreator.createOFNodeConnector((short) 1, node1);
        NodeConnector nc1_2 = NodeConnectorCreator.createOFNodeConnector((short) 2, node1);
        
        // test addStaticHost(), put into inactive host DB if not verifiable
        Status st = this.hosttracker.addStaticHost("192.168.0.8", "11:22:33:44:55:66",
                nc1_1, "0");
        st = this.hosttracker.addStaticHost("192.168.0.13", "11:22:33:44:55:77",
                nc1_2, "0");
        
        this.invtoryListener.notifyNodeConnector(nc1_1,
                UpdateType.ADDED, null);

        // check all host list
        Iterator<HostNodeConnector> hnci = this.hosttracker
                .getAllHosts().iterator();
        while (hnci.hasNext()) {
            ip = hnci.next().getNetworkAddressAsString();
            Assert.assertTrue(ip.equals("192.168.0.8"));
        }
        
        // check active host DB
        hnci = this.hosttracker.getActiveStaticHosts().iterator();
        while (hnci.hasNext()) {
            ip = hnci.next().getNetworkAddressAsString();
            Assert.assertTrue(ip.equals("192.168.0.8"));
        }
        
        // check inactive host DB
        hnci = this.hosttracker.getInactiveStaticHosts().iterator();
        while (hnci.hasNext()) {
            ip = hnci.next().getNetworkAddressAsString();
            Assert.assertTrue(ip.equals("192.168.0.13"));
        }
    }
    
    
    
    @Test
    public void testHostFind() throws UnknownHostException {
        
        assertNotNull(this.invtoryListener);

        // create one node and two node connectors
        Node node1 = NodeCreator.createOFNode(1L);
        NodeConnector nc1_1 = NodeConnectorCreator.createOFNodeConnector((short) 1, node1);
        NodeConnector nc1_2 = NodeConnectorCreator.createOFNodeConnector((short) 2, node1);
        
        // test addStaticHost(), put into inactive host DB if not verifiable
        Status st = this.hosttracker.addStaticHost(
                "192.168.0.8", "11:22:33:44:55:66", nc1_1, "0");
        st = this.hosttracker.addStaticHost(
                "192.168.0.13", "11:22:33:44:55:77", nc1_2, "0");
        
        HostNodeConnector hnc_1 = this.hosttracker
                .hostFind(InetAddress.getByName("192.168.0.8"));
        assertNull(hnc_1);
        
        this.invtoryListener.notifyNodeConnector(nc1_1,
                UpdateType.ADDED, null);

        hnc_1 = this.hosttracker.hostFind(InetAddress.getByName("192.168.0.8"));
        assertNotNull(hnc_1);
        
    }
    
}
