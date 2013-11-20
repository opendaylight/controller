/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.protocol_plugins.stub.internal.TopologyServices;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;


@RunWith(PaxExam.class)
public class TopologyManagerIT {
    private Logger log = LoggerFactory
            .getLogger(TopologyManagerIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    @Inject
    private ITopologyManager manager = null;

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
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic")
                        .versionAsInProject(),
                // needed by statisticsmanager
                mavenBundle("org.opendaylight.controller", "containermanager")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager.it.implementation")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.services")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub")
                    .versionAsInProject(),
                // needed by forwardingrulesmanager
                mavenBundle("org.opendaylight.controller", "configuration")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration.implementation")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker")
                    .versionAsInProject(),

                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "protocol_plugins.stub")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statisticsmanager")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statisticsmanager.implementation")
                    .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "forwardingrulesmanager")
                    .versionAsInProject(),

                // needed by hosttracker
                mavenBundle("org.opendaylight.controller", "topologymanager")
                        .versionAsInProject(),
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

    /**
     *  This test verifies that the isInternal method of the TopologyManager returns true when a node is internal and
     *  not otherwise
     *
     *  To make a node interval we add a node using the plugin interface (TopologyServices.addEdge) this is to ensure
     *  that when TopologyManager sees the edge is via it's dependency on the SAL ITopologyService.
     *
     * @throws Exception
     */
    @Test
    public void testIsInternal() throws Exception{
        Node node1 = new Node("STUB", 0xCAFE);
        Node node2 = new Node("STUB", 0XFACE);

        NodeConnector head = new NodeConnector("STUB", node1.getID(), node1);
        NodeConnector tail = new NodeConnector("STUB", node2.getID(), node2);

        assert(this.manager.isInternal(head));

        Set<Property> properties = new HashSet<Property>();

        ServiceReference r = bc.getServiceReference(IPluginInTopologyService.class
                .getName());
        TopologyServices topologyServices = null;
        if (r != null) {
            if(bc.getService(r) instanceof TopologyServices) {
                topologyServices = (TopologyServices) bc.getService(r);
            } else {
                throw new RuntimeException("topology service registered is not from the stub plugin implementation");
            }
        }

        topologyServices.addEdge(new Edge(tail, head), properties, UpdateType.ADDED);

        assert(this.manager.isInternal(head));
    }

}
