package org.opendaylight.controller.northbound.integrationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import static org.junit.Assert.*;
import org.ops4j.pax.exam.junit.Configuration;
import static org.ops4j.pax.exam.CoreOptions.*;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import org.opendaylight.controller.usermanager.IUserManager;

@RunWith(PaxExam.class)
public class NorthboundIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(NorthboundIntegrationTest.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private IUserManager users = null;

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

        ServiceReference r = bc.getServiceReference(IUserManager.class
                .getName());
        if (r != null) {
            this.users = (IUserManager) bc.getService(r);
        }
        // If UserManager is null, cannot login to run tests.
        assertNotNull(this.users);

    }

    // static variable to pass response code from getJsonResult()
    private static Integer httpResponseCode = null;

    private String getJsonResult(String restUrl) {
        return getJsonResult(restUrl, "GET");
    }

    private String getJsonResult(String restUrl, String method) {
        // initialize response code to indicate error
        httpResponseCode = 400;

        try {
            URL url = new URL(restUrl);

            this.users.getAuthorizationList();
            this.users.authenticate("admin", "admin");
            String authString = "admin:admin";
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);

            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Basic "
                    + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            connection.connect();
            connection.getContentType();

            // Response code for success should be 2xx
            httpResponseCode = connection.getResponseCode();

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is,
                    Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            is.close();
            connection.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }

    }

    @Test
    public void testStatistics() {
        String actionTypes[] = { "drop", "loopback", "flood", "floodAll",
                "controller", "swPath", "hwPath", "output", "setDlSrc",
                "setDlDst", "setDlType", "setVlanId", "setVlanPcp",
                "setVlanCfi", "popVlan", "pushVlan", "setNwSrc", "setNwDst",
                "setNwTos", "setTpSrc", "setTpDst" };
        System.out.println("Starting Statistics JAXB client.");

        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/statistics/default/";
        try {
            String result = getJsonResult(baseURL + "flowstats");
            JSONTokener jt = new JSONTokener(result);
            JSONObject json = new JSONObject(jt);
            JSONObject flowStatistics = json.getJSONObject("flowStatistics");
            JSONObject node = flowStatistics.getJSONObject("node");
            // test that node was returned properly
            Assert.assertTrue(node.getInt("@id") == 0xCAFE);
            Assert.assertTrue(node.getString("@type").equals("STUB"));

            // test that flow statistics results are correct
            JSONArray flowStats = flowStatistics.getJSONArray("flowStat");
            for (int i = 0; i < flowStats.length(); i++) {

                JSONObject flowStat = flowStats.getJSONObject(i);
                testFlowStat(flowStat, actionTypes[i]);

            }

            // for /controller/nb/v2/statistics/default/portstats
            result = getJsonResult(baseURL + "portstats");
            jt = new JSONTokener(result);
            json = new JSONObject(jt);
            JSONObject portStatistics = json.getJSONObject("portStatistics");
            JSONObject node2 = portStatistics.getJSONObject("node");
            // test that node was returned properly
            Assert.assertTrue(node2.getInt("@id") == 0xCAFE);
            Assert.assertTrue(node2.getString("@type").equals("STUB"));

            // test that port statistic results are correct
            JSONObject portStat = portStatistics.getJSONObject("portStat");
            Assert.assertTrue(portStat.getInt("receivePackets") == 250);
            Assert.assertTrue(portStat.getInt("transmitPackets") == 500);
            Assert.assertTrue(portStat.getInt("receiveBytes") == 1000);
            Assert.assertTrue(portStat.getInt("transmitBytes") == 5000);
            Assert.assertTrue(portStat.getInt("receiveDrops") == 2);
            Assert.assertTrue(portStat.getInt("transmitDrops") == 50);
            Assert.assertTrue(portStat.getInt("receiveErrors") == 3);
            Assert.assertTrue(portStat.getInt("transmitErrors") == 10);
            Assert.assertTrue(portStat.getInt("receiveFrameError") == 5);
            Assert.assertTrue(portStat.getInt("receiveOverRunError") == 6);
            Assert.assertTrue(portStat.getInt("receiveCrcError") == 1);
            Assert.assertTrue(portStat.getInt("collisionCount") == 4);

            // test for getting one specific node's stats
            result = getJsonResult(baseURL + "flowstats/STUB/51966");
            jt = new JSONTokener(result);
            json = new JSONObject(jt);
            node = json.getJSONObject("node");
            // test that node was returned properly
            Assert.assertTrue(node.getInt("@id") == 0xCAFE);
            Assert.assertTrue(node.getString("@type").equals("STUB"));

            // test that flow statistics results are correct
            flowStats = json.getJSONArray("flowStat");
            for (int i = 0; i < flowStats.length(); i++) {
                JSONObject flowStat = flowStats.getJSONObject(i);
                testFlowStat(flowStat, actionTypes[i]);
            }

            result = getJsonResult(baseURL + "portstats/STUB/51966");
            jt = new JSONTokener(result);
            json = new JSONObject(jt);
            node2 = json.getJSONObject("node");
            // test that node was returned properly
            Assert.assertTrue(node2.getInt("@id") == 0xCAFE);
            Assert.assertTrue(node2.getString("@type").equals("STUB"));

            // test that port statistic results are correct
            portStat = json.getJSONObject("portStat");
            Assert.assertTrue(portStat.getInt("receivePackets") == 250);
            Assert.assertTrue(portStat.getInt("transmitPackets") == 500);
            Assert.assertTrue(portStat.getInt("receiveBytes") == 1000);
            Assert.assertTrue(portStat.getInt("transmitBytes") == 5000);
            Assert.assertTrue(portStat.getInt("receiveDrops") == 2);
            Assert.assertTrue(portStat.getInt("transmitDrops") == 50);
            Assert.assertTrue(portStat.getInt("receiveErrors") == 3);
            Assert.assertTrue(portStat.getInt("transmitErrors") == 10);
            Assert.assertTrue(portStat.getInt("receiveFrameError") == 5);
            Assert.assertTrue(portStat.getInt("receiveOverRunError") == 6);
            Assert.assertTrue(portStat.getInt("receiveCrcError") == 1);
            Assert.assertTrue(portStat.getInt("collisionCount") == 4);

        } catch (Exception e) {
            // Got an unexpected exception
            Assert.assertTrue(false);

        }
    }

    private void testFlowStat(JSONObject flowStat, String actionType) {
        try {
            Assert.assertTrue(flowStat.getInt("tableId") == 1);
            Assert.assertTrue(flowStat.getInt("durationSeconds") == 40);
            Assert.assertTrue(flowStat.getInt("durationNanoseconds") == 400);
            Assert.assertTrue(flowStat.getInt("packetCount") == 200);
            Assert.assertTrue(flowStat.getInt("byteCount") == 100);

            // test that flow information is correct
            JSONObject flow = flowStat.getJSONObject("flow");
            Assert.assertTrue(flow.getInt("priority") == 3500);
            Assert.assertTrue(flow.getInt("idleTimeout") == 1000);
            Assert.assertTrue(flow.getInt("hardTimeout") == 2000);
            Assert.assertTrue(flow.getInt("id") == 12345);

            JSONObject match = (flow.getJSONObject("match")
                    .getJSONObject("matchField"));
            Assert.assertTrue(match.getString("type").equals("NW_DST"));
            Assert.assertTrue(match.getString("value").equals("1.1.1.1"));

            JSONObject act = flow.getJSONObject("actions");
            Assert.assertTrue(act.getString("@type").equals(actionType));

            if (act.getString("@type").equals("output")) {
                JSONObject port = act.getJSONObject("port");
                JSONObject port_node = port.getJSONObject("node");
                Assert.assertTrue(port.getInt("@id") == 51966);
                Assert.assertTrue(port.getString("@type").equals("STUB"));
                Assert.assertTrue(port_node.getInt("@id") == 51966);
                Assert.assertTrue(port_node.getString("@type").equals("STUB"));
            }

            if (act.getString("@type").equals("setDlSrc")) {
                byte srcMatch[] = { (byte) 5, (byte) 4, (byte) 3, (byte) 2,
                        (byte) 1 };
                String src = act.getString("address");
                byte srcBytes[] = new byte[5];
                srcBytes[0] = Byte.parseByte(src.substring(0, 2));
                srcBytes[1] = Byte.parseByte(src.substring(2, 4));
                srcBytes[2] = Byte.parseByte(src.substring(4, 6));
                srcBytes[3] = Byte.parseByte(src.substring(6, 8));
                srcBytes[4] = Byte.parseByte(src.substring(8, 10));
                Assert.assertTrue(Arrays.equals(srcBytes, srcMatch));
            }

            if (act.getString("@type").equals("setDlDst")) {
                byte dstMatch[] = { (byte) 1, (byte) 2, (byte) 3, (byte) 4,
                        (byte) 5 };
                String dst = act.getString("address");
                byte dstBytes[] = new byte[5];
                dstBytes[0] = Byte.parseByte(dst.substring(0, 2));
                dstBytes[1] = Byte.parseByte(dst.substring(2, 4));
                dstBytes[2] = Byte.parseByte(dst.substring(4, 6));
                dstBytes[3] = Byte.parseByte(dst.substring(6, 8));
                dstBytes[4] = Byte.parseByte(dst.substring(8, 10));
                Assert.assertTrue(Arrays.equals(dstBytes, dstMatch));
            }
            if (act.getString("@type").equals("setDlType"))
                Assert.assertTrue(act.getInt("dlType") == 10);
            if (act.getString("@type").equals("setVlanId"))
                Assert.assertTrue(act.getInt("vlanId") == 2);
            if (act.getString("@type").equals("setVlanPcp"))
                Assert.assertTrue(act.getInt("pcp") == 3);
            if (act.getString("@type").equals("setVlanCfi"))
                Assert.assertTrue(act.getInt("cfi") == 1);

            if (act.getString("@type").equals("setNwSrc"))
                Assert.assertTrue(act.getString("address").equals("2.2.2.2"));
            if (act.getString("@type").equals("setNwDst"))
                Assert.assertTrue(act.getString("address").equals("1.1.1.1"));

            if (act.getString("@type").equals("pushVlan")) {
                int head = act.getInt("VlanHeader");
                // parsing vlan header
                int id = head & 0xfff;
                int cfi = (head >> 12) & 0x1;
                int pcp = (head >> 13) & 0x7;
                int tag = (head >> 16) & 0xffff;
                Assert.assertTrue(id == 1234);
                Assert.assertTrue(cfi == 1);
                Assert.assertTrue(pcp == 1);
                Assert.assertTrue(tag == 0x8100);
            }
            if (act.getString("@type").equals("setNwTos"))
                Assert.assertTrue(act.getInt("tos") == 16);
            if (act.getString("@type").equals("setTpSrc"))
                Assert.assertTrue(act.getInt("port") == 4201);
            if (act.getString("@type").equals("setTpDst"))
                Assert.assertTrue(act.getInt("port") == 8080);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

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
                systemProperty("org.eclipse.gemini.web.tomcat.config.path")
                        .value(PathUtils.getBaseDir()
                                + "/src/test/resources/tomcat-server.xml"),

                // setting default level. Jersey bundles will need to be started
                // earlier.
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),

                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                mavenBundle("javax.servlet", "servlet-api", "2.5"),

                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.2"),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.2"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.2"),
                mavenBundle("ch.qos.logback", "logback-core", "1.0.9"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.0.9"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),

                // the plugin stub to get data for the tests
                mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub", "0.4.0-SNAPSHOT"),

                // List all the opendaylight modules
                mavenBundle("org.opendaylight.controller", "security",
                        "0.4.0-SNAPSHOT").noStart(),
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.5.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "sal.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "statisticsmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "statisticsmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager.implementation",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services-implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "routing.dijkstra_implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "topologymanager",
                        "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller", "usermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "logging.bridge",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.test",
                        "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller",
                        "forwarding.staticrouting", "0.4.0-SNAPSHOT"),

                // Northbound bundles
                mavenBundle("org.opendaylight.controller",
                        "commons.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwarding.staticrouting.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "statistics.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "topology.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "flowprogrammer.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "subnets.northbound", "0.4.0-SNAPSHOT"),

                mavenBundle("org.codehaus.jackson", "jackson-mapper-asl",
                        "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-core-asl", "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-jaxrs", "1.9.8"),
                mavenBundle("org.codehaus.jettison", "jettison", "1.3.3"),

                mavenBundle("commons-io", "commons-io", "2.3"),

                mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),

                mavenBundle("equinoxSDK381", "javax.servlet",
                        "3.0.0.v201112011016"),
                mavenBundle("equinoxSDK381", "javax.servlet.jsp",
                        "2.2.0.v201112011158"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds",
                        "1.4.0.v20120522-1841"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util",
                        "1.0.400.v20120522-2049"),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services",
                        "3.3.100.v20120522-1822"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell",
                        "0.8.0.v201110170705"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.cm",
                        "1.0.400.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console",
                        "1.0.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.launcher",
                        "1.3.0.v20120522-1813"),

                mavenBundle("geminiweb", "org.eclipse.gemini.web.core",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.extender",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.tomcat",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.kernel.equinox.extensions",
                        "3.6.0.RELEASE").noStart(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.common",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.io",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.math",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.util.osgi.manifest", "3.6.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.util.parser.manifest",
                        "3.6.0.RELEASE"),

                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager.shell", "3.0.1"),

                mavenBundle("com.google.code.gson", "gson", "2.1"),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec", "1.0.1.Final"),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall",
                        "3.1.6"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("commons-codec", "commons-codec"),
                mavenBundle("virgomirror",
                        "org.eclipse.jdt.core.compiler.batch",
                        "3.8.0.I20120518-2145"),
                mavenBundle("eclipselink", "javax.persistence",
                        "2.0.4.v201112161009"),

                mavenBundle("orbit", "javax.activation", "1.1.0.v201211130549"),
                mavenBundle("orbit", "javax.annotation", "1.1.0.v201209060031"),
                mavenBundle("orbit", "javax.ejb", "3.1.1.v201204261316"),
                mavenBundle("orbit", "javax.el", "2.2.0.v201108011116"),
                mavenBundle("orbit", "javax.mail.glassfish",
                        "1.4.1.v201108011116"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("orbit", "org.apache.catalina",
                        "7.0.32.v201211201336"),
                // these are bundle fragments that can't be started on its own
                mavenBundle("orbit", "org.apache.catalina.ha",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.catalina.tribes",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.coyote",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.jasper",
                        "7.0.32.v201211201952").noStart(),

                mavenBundle("orbit", "org.apache.el", "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.juli.extras",
                        "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.api",
                        "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.util",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl",
                        "1.2.0.v201105211821"),
                mavenBundle("orbit", "javax.servlet.jsp.jstl.impl",
                        "1.2.0.v201210211230"),

                mavenBundle("org.ops4j.pax.exam", "pax-exam-container-native"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-junit4"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-link-mvn"),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether"),

                mavenBundle("org.springframework", "org.springframework.asm",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.aop",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.context", "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.context.support", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.core",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.beans",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.expression", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.web",
                        "3.1.3.RELEASE"),

                mavenBundle("org.aopalliance",
                        "com.springsource.org.aopalliance", "1.0.0"),
                mavenBundle("org.springframework",
                        "org.springframework.web.servlet", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-config", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-core", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-web", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-taglibs", "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.transaction", "3.1.3.RELEASE"),

                mavenBundle("org.ow2.chameleon.management", "chameleon-mbeans",
                        "1.0.0"),
                mavenBundle("org.opendaylight.controller.thirdparty",
                        "net.sf.jung2", "2.0.1-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller.thirdparty",
                        "com.sun.jersey.jersey-servlet", "1.17-SNAPSHOT"),

                // Jersey needs to be started before the northbound application
                // bundles, using a lower start level
                mavenBundle("com.sun.jersey", "jersey-client", "1.17"),
                mavenBundle("com.sun.jersey", "jersey-server", "1.17")
                        .startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-core", "1.17")
                        .startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-json", "1.17")
                        .startLevel(2), junitBundles());
    }
}