package org.opendaylight.controller.statisticsmanager.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class StatisticsManagerIT {
    private Logger log = LoggerFactory
            .getLogger(StatisticsManagerIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private IStatisticsManager manager = null;

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
                mavenBundle("org.opendaylight.controller", "connectionmanager.implementation").
                    versionAsInProject(),
                mavenBundle("org.opendaylight.controller",  "connectionmanager").
                    versionAsInProject(),
                mavenBundle("org.opendaylight.controller",  "sal.connection").
                    versionAsInProject(),
                mavenBundle("org.opendaylight.controller",  "sal.connection.implementation").
                    versionAsInProject(),

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

        ServiceReference r = bc.getServiceReference(IStatisticsManager.class
                .getName());
        if (r != null) {
            this.manager = (IStatisticsManager) bc.getService(r);
        }
        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.manager);

    }

    @Test
    public void testGetFlows() {
        try {
            Node node = new Node("STUB", new Integer(0xCAFE));
            List<FlowOnNode> flows = this.manager.getFlows(node);
            FlowOnNode fn = flows.get(0);
            Assert.assertTrue(fn.getByteCount() == 100);
            Assert.assertTrue(fn.getDurationNanoseconds() == 400);
            Assert.assertTrue(fn.getDurationSeconds() == 40);
            Assert.assertTrue(fn.getTableId() == (byte) 0x1);
            Assert.assertTrue(fn.getPacketCount() == 200);

            Match match = new Match();
            try {
                match.setField(MatchType.NW_DST,
                        InetAddress.getByName("1.1.1.1"));
            } catch (UnknownHostException e) {
                fail("Couldn't create match");
            }
            Assert.assertTrue(match.equals(fn.getFlow().getMatch()));
            Assert.assertTrue(fn.getFlow().getActions().get(0)
                    .equals(new Drop()));
        } catch (ConstructionException e) {
            // Got an unexpected exception
            Assert.assertTrue(false);
        }

    }

    @Test
    public void testGetFlowStatistics() {
        Flow flow = new Flow();

        Match match = new Match();
        try {
            match.setField(MatchType.NW_DST, InetAddress.getByName("1.1.1.1"));
        } catch (UnknownHostException e) {
        }
        flow.setMatch(match);
        Action action = new Drop();

        List<Action> actions = new ArrayList<Action>();
        actions.add(action);
        flow.setActions(actions);
        // as in stub
        flow.setPriority((short) 3500);
        flow.setIdleTimeout((short) 1000);
        flow.setHardTimeout((short) 2000);
        flow.setId(12345);

        try {
            Node node = new Node("STUB", 0xCAFE);
            FlowEntry fe = new FlowEntry("g1", "f1", flow, node);
            List<FlowEntry> list = new ArrayList<FlowEntry>();
            list.add(fe);
            FlowEntry fe2 = new FlowEntry("g1", "f2", flow, node);
            list.add(fe2);

            Map<Node, List<FlowOnNode>> result = this.manager
                    .getFlowStatisticsForFlowList(null);
            Assert.assertTrue(result.isEmpty());
            result = this.manager.getFlowStatisticsForFlowList(list);
            List<FlowOnNode> results = result.get(node);
            FlowOnNode fn = results.get(0);
            Assert.assertTrue(fn.getByteCount() == 100);
            Assert.assertTrue(fn.getDurationNanoseconds() == 400);
            Assert.assertTrue(fn.getDurationSeconds() == 40);
            Assert.assertTrue(fn.getTableId() == (byte) 0x1);
            Assert.assertTrue(fn.getPacketCount() == 200);
            Assert.assertTrue(fn.getFlow().equals(flow));
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        }

    }

    @Test
    public void testGetFlowsNumber() {
        try {
            Node node = new Node("STUB", 0xCAFE);
            Assert.assertEquals(21, this.manager.getFlowsNumber(node));
        }catch(ConstructionException e){
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testGetNodeDescription() {
        try {
            Node node = new Node("STUB", 0xCAFE);
            NodeDescription desc = this.manager.getNodeDescription(node);
            Assert.assertTrue(desc.getDescription().equals(
                    "This is a sample node description"));
            Assert.assertTrue(desc.getHardware().equals("stub hardware"));
            Assert.assertTrue(desc.getSoftware().equals("stub software"));
            Assert.assertTrue(desc.getSerialNumber().equals("123"));
            Assert.assertTrue(desc.getManufacturer().equals("opendaylight"));
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        }

    }

    @Test
    public void testGetNodeConnectorStatistics() {
        try {
            Node node = new Node("STUB", 0xCAFE);
            List<NodeConnectorStatistics> stats = this.manager
                    .getNodeConnectorStatistics(node);
            NodeConnectorStatistics ns = stats.get(0);
            Assert.assertTrue(ns.getCollisionCount() == 4);
            Assert.assertTrue(ns.getReceiveByteCount() == 1000);
            Assert.assertTrue(ns.getReceiveCRCErrorCount() == 1);
            Assert.assertTrue(ns.getReceiveDropCount() == 2);
            Assert.assertTrue(ns.getReceiveErrorCount() == 3);
            Assert.assertTrue(ns.getReceiveFrameErrorCount() == 5);
            Assert.assertTrue(ns.getReceiveOverRunErrorCount() == 6);
            Assert.assertTrue(ns.getReceivePacketCount() == 250);
            Assert.assertTrue(ns.getTransmitByteCount() == 5000);
            Assert.assertTrue(ns.getTransmitDropCount() == 50);
            Assert.assertTrue(ns.getTransmitErrorCount() == 10);
            Assert.assertTrue(ns.getTransmitPacketCount() == 500);

            NodeConnector nc = ns.getNodeConnector();
            NodeConnectorStatistics ns2 = this.manager
                    .getNodeConnectorStatistics(nc);
            Assert.assertTrue(ns2.getCollisionCount() == 4);
            Assert.assertTrue(ns2.getReceiveByteCount() == 1000);
            Assert.assertTrue(ns2.getReceiveCRCErrorCount() == 1);
            Assert.assertTrue(ns2.getReceiveDropCount() == 2);
            Assert.assertTrue(ns2.getReceiveErrorCount() == 3);
            Assert.assertTrue(ns2.getReceiveFrameErrorCount() == 5);
            Assert.assertTrue(ns2.getReceiveOverRunErrorCount() == 6);
            Assert.assertTrue(ns2.getReceivePacketCount() == 250);
            Assert.assertTrue(ns2.getTransmitByteCount() == 5000);
            Assert.assertTrue(ns2.getTransmitDropCount() == 50);
            Assert.assertTrue(ns2.getTransmitErrorCount() == 10);
            Assert.assertTrue(ns2.getTransmitPacketCount() == 500);

        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        }
    }

}
