/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.opendaylight.controller.hosttracker.*;

@RunWith(PaxExam.class)
public class HostTrackerIT {
    private Logger log = LoggerFactory.getLogger(HostTrackerIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private IfIptoHost hosttracker = null;
    private IInventoryListener invtoryListener = null;
    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(

                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                // List framework bundles
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console", "1.0.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util", "1.0.400.v20120522-2049"),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services","3.3.100.v20120522-1822"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds", "1.4.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command", "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime", "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell", "0.8.0.v201110170705"),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),

                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.connection").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.connection.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "connectionmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "connectionmanager.implementation").versionAsInProject(),

                // needed by statisticsmanager
                mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager.implementation").versionAsInProject(),

                mavenBundle("org.opendaylight.controller", "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub").versionAsInProject(),

                // needed by forwardingrulesmanager
                mavenBundle("org.opendaylight.controller", "switchmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "forwardingrulesmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statisticsmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker.implementation").versionAsInProject(),

                // needed by hosttracker
                mavenBundle("org.opendaylight.controller", "topologymanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "arphandler").versionAsInProject(),

                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
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
                log.debug("Bundle:" + b[i].getSymbolicName() + " state:" + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is " + "unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);

        // Now lets create a hosttracker for testing purpose
        ServiceReference s = bc.getServiceReference(IfIptoHost.class.getName());
        if (s != null) {
            this.hosttracker = (IfIptoHost) bc.getService(s);
            this.invtoryListener = (IInventoryListener) this.hosttracker;
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
        Status st = this.hosttracker.addStaticHost("192.168.0.8", "11:22:33:44:55:66", nc1_1, "0");
        Assert.assertTrue(st.isSuccess());
        st = this.hosttracker.addStaticHost("192.168.0.13", "11:22:33:44:55:77", nc1_2, "0");
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
        Status st = this.hosttracker.addStaticHost("192.168.0.8", "11:22:33:44:55:66", nc1_1, "0");
        st = this.hosttracker.addStaticHost("192.168.0.13", "11:22:33:44:55:77", nc1_2, "0");

        this.invtoryListener.notifyNodeConnector(nc1_1, UpdateType.ADDED, null);

        // check all host list
        Iterator<HostNodeConnector> hnci = this.hosttracker.getAllHosts().iterator();
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
        Status st = this.hosttracker.addStaticHost("192.168.0.8", "11:22:33:44:55:66", nc1_1, "0");
        st = this.hosttracker.addStaticHost("192.168.0.13", "11:22:33:44:55:77", nc1_2, "0");

        HostNodeConnector hnc_1 = this.hosttracker.hostFind(InetAddress.getByName("192.168.0.8"));
        assertNull(hnc_1);

        this.invtoryListener.notifyNodeConnector(nc1_1, UpdateType.ADDED, null);

        hnc_1 = this.hosttracker.hostFind(InetAddress.getByName("192.168.0.8"));
        assertNotNull(hnc_1);

    }

}
