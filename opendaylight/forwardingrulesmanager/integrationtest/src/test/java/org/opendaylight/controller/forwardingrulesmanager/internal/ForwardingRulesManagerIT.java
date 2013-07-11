package org.opendaylight.controller.forwardingrulesmanager.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.NodeCreator;
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
public class ForwardingRulesManagerIT {
    private Logger log = LoggerFactory.getLogger(ForwardingRulesManagerIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private IForwardingRulesManager manager = null;

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
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic")
                        .versionAsInProject(),
                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation")
                        .versionAsInProject(),

                mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub").versionAsInProject(),

                mavenBundle("org.opendaylight.controller", "containermanager")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation").versionAsInProject(),

                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager").versionAsInProject(),

                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager.implementation")
                        .versionAsInProject(),

                mavenBundle("org.opendaylight.controller",
                        "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration")
                        .versionAsInProject(),

                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.implementation").versionAsInProject(),

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
        for (Bundle element : b) {
            int state = element.getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.debug("Bundle:" + element.getSymbolicName() + " state:"
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

        ServiceReference r = bc.getServiceReference(IForwardingRulesManager.class.getName());
        if (r != null) {
            this.manager = (IForwardingRulesManager) bc.getService(r);
        }
        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.manager);

    }

    @Test
    public void testFlowEntries() {
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

        Node node = NodeCreator.createOFNode(1L);
        FlowEntry fe = new FlowEntry("g1", "f1", flow, node);

        Status stat = manager.installFlowEntry(fe);

        // OF plugin is not there in integration testing mode
        Assert.assertTrue(stat.getCode() == StatusCode.NOSERVICE);
    }

}
