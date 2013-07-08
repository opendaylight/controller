package org.opendaylight.controller.northbound.integrationtest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.usermanager.IUserManager;
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
public class NorthboundIT {
    private Logger log = LoggerFactory.getLogger(NorthboundIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private IUserManager userManager = null;
    private IInventoryListener invtoryListener = null;
    private IListenTopoUpdates topoUpdates = null;

    private Boolean debugMsg = false;

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

        ServiceReference r = bc.getServiceReference(IUserManager.class.getName());
        if (r != null) {
            this.userManager = (IUserManager) bc.getService(r);
        }
        // If UserManager is null, cannot login to run tests.
        assertNotNull(this.userManager);

        r = bc.getServiceReference(IfIptoHost.class.getName());
        if (r != null) {
            this.invtoryListener = (IInventoryListener) bc.getService(r);
        }

        // If inventoryListener is null, cannot run hosttracker tests.
        assertNotNull(this.invtoryListener);

        r = bc.getServiceReference(IListenTopoUpdates.class.getName());
        if (r != null) {
            this.topoUpdates = (IListenTopoUpdates) bc.getService(r);
        }

        // If topologyManager is null, cannot run topology North tests.
        assertNotNull(this.topoUpdates);

    }

    // static variable to pass response code from getJsonResult()
    private static Integer httpResponseCode = null;

    private String getJsonResult(String restUrl) {
        return getJsonResult(restUrl, "GET", null);
    }

    private String getJsonResult(String restUrl, String method) {
        return getJsonResult(restUrl, method, null);
    }

    private String getJsonResult(String restUrl, String method, String body) {
        // initialize response code to indicate error
        httpResponseCode = 400;

        if (debugMsg) {
            System.out.println("HTTP method: " + method + " url: " + restUrl.toString());
            if (body != null)
                System.out.println("body: " + body);
        }

        try {
            URL url = new URL(restUrl);
            this.userManager.getAuthorizationList();
            this.userManager.authenticate("admin", "admin");
            String authString = "admin:admin";
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            if (body != null) {
                connection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(body);
                wr.flush();
            }
            connection.connect();
            connection.getContentType();

            // Response code for success should be 2xx
            httpResponseCode = connection.getResponseCode();
            if (httpResponseCode > 299)
                return httpResponseCode.toString();

            if (debugMsg) {
                System.out.println("HTTP response code: " + connection.getResponseCode());
                System.out.println("HTTP response message: " + connection.getResponseMessage());
            }

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
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

    private void testNodeProperties(JSONObject node, Integer nodeId, String nodeType, Integer timestamp,
            String timestampName, Integer actionsValue, Integer capabilitiesValue, Integer tablesValue,
            Integer buffersValue) throws JSONException {

        JSONObject nodeInfo = node.getJSONObject("node");
        Assert.assertEquals(nodeId, (Integer) nodeInfo.getInt("@id"));
        Assert.assertEquals(nodeType, nodeInfo.getString("@type"));

        JSONObject properties = node.getJSONObject("properties");

        if (timestamp == null || timestampName == null) {
            Assert.assertFalse(properties.has("timeStamp"));
        } else {
            Assert.assertEquals(timestamp, (Integer) properties.getJSONObject("timeStamp").getInt("timestamp"));
            Assert.assertEquals(timestampName, properties.getJSONObject("timeStamp").getString("timestampName"));
        }
        if (actionsValue == null) {
            Assert.assertFalse(properties.has("actions"));
        } else {
            Assert.assertEquals(actionsValue, (Integer) properties.getJSONObject("actions").getInt("actionsValue"));
        }
        if (capabilitiesValue == null) {
            Assert.assertFalse(properties.has("capabilities"));
        } else {
            Assert.assertEquals(capabilitiesValue,
                    (Integer) properties.getJSONObject("capabilities").getInt("capabilitiesValue"));
        }
        if (tablesValue == null) {
            Assert.assertFalse(properties.has("tables"));
        } else {
            Assert.assertEquals(tablesValue, (Integer) properties.getJSONObject("tables").getInt("tablesValue"));
        }
        if (buffersValue == null) {
            Assert.assertFalse(properties.has("buffers"));
        } else {
            Assert.assertEquals(buffersValue, (Integer) properties.getJSONObject("buffers").getInt("buffersValue"));
        }
    }

    private void testNodeConnectorProperties(JSONObject nodeConnectorProperties, Integer ncId, String ncType,
            Integer nodeId, String nodeType, Integer state, Integer capabilities, Integer bandwidth)
            throws JSONException {

        JSONObject nodeConnector = nodeConnectorProperties.getJSONObject("nodeconnector");
        JSONObject node = nodeConnector.getJSONObject("node");
        JSONObject properties = nodeConnectorProperties.getJSONObject("properties");

        Assert.assertEquals(ncId, (Integer) nodeConnector.getInt("@id"));
        Assert.assertEquals(ncType, nodeConnector.getString("@type"));
        Assert.assertEquals(nodeId, (Integer) node.getInt("@id"));
        Assert.assertEquals(nodeType, node.getString("@type"));
        if (state == null) {
            Assert.assertFalse(properties.has("state"));
        } else {
            Assert.assertEquals(state, (Integer) properties.getJSONObject("state").getInt("stateValue"));
        }
        if (capabilities == null) {
            Assert.assertFalse(properties.has("capabilities"));
        } else {
            Assert.assertEquals(capabilities,
                    (Integer) properties.getJSONObject("capabilities").getInt("capabilitiesValue"));
        }
        if (bandwidth == null) {
            Assert.assertFalse(properties.has("bandwidth"));
        } else {
            Assert.assertEquals(bandwidth, (Integer) properties.getJSONObject("bandwidth").getInt("bandwidthValue"));
        }

    }

    @Test
    public void testSubnetsNorthbound() throws JSONException {
        System.out.println("Starting Subnets JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/subnet/";

        String name1 = "testSubnet1";
        String subnet1 = "1.1.1.1/24";
        String name2 = "testSubnet2";
        String subnet2 = "2.2.2.2/24";

        // Test GET subnets in default container
        String result = getJsonResult(baseURL + "default");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        Assert.assertEquals("{}", result);

        // Test GET subnet1 expecting 404
        result = getJsonResult(baseURL + "default/" + name1);
        Assert.assertEquals(404, httpResponseCode.intValue());

        // Test POST subnet1
        String queryParameter = new QueryParameter("subnetName", name1).add("subnet", subnet1).getString();
        result = getJsonResult(baseURL + "default/" + name1 + queryParameter, "POST");
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test GET subnet1
        result = getJsonResult(baseURL + "default/" + name1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        Assert.assertEquals(200, httpResponseCode.intValue());
        Assert.assertEquals(name1, json.getString("@name"));
        Assert.assertEquals(subnet1, json.getString("@subnet"));

        // Test POST subnet2
        queryParameter = new QueryParameter("subnetName", name2).add("subnet", subnet2).getString();
        result = getJsonResult(baseURL + "default/" + name2 + queryParameter, "POST");
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test GET all subnets in default container
        result = getJsonResult(baseURL + "default");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONArray subnetConfigArray = json.getJSONArray("subnetConfig");
        JSONObject subnetConfig;
        Assert.assertEquals(2, subnetConfigArray.length());
        for (int i = 0; i < subnetConfigArray.length(); i++) {
            subnetConfig = subnetConfigArray.getJSONObject(i);
            if (subnetConfig.getString("@name").equals(name1)) {
                Assert.assertEquals(subnet1, subnetConfig.getString("@subnet"));
            } else if (subnetConfig.getString("@name").equals(name2)) {
                Assert.assertEquals(subnet2, subnetConfig.getString("@subnet"));
            } else {
                // Unexpected config name
                Assert.assertTrue(false);
            }
        }

        // Test DELETE subnet1
        result = getJsonResult(baseURL + "default/" + name1, "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        // Test GET deleted subnet1
        result = getJsonResult(baseURL + "default/" + name1);
        Assert.assertEquals(404, httpResponseCode.intValue());

    }

    @Test
    public void testStaticRoutingNorthbound() throws JSONException {
        System.out.println("Starting StaticRouting JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/staticroute/";

        String name1 = "testRoute1";
        String prefix1 = "192.168.1.1/24";
        String nextHop1 = "0.0.0.0";
        String name2 = "testRoute2";
        String prefix2 = "192.168.1.1/16";
        String nextHop2 = "1.1.1.1";

        // Test GET static routes in default container, expecting no results
        String result = getJsonResult(baseURL + "default");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        Assert.assertEquals("{}", result);

        // Test insert static route
        String requestBody = "{\"name\":\"" + name1 + "\", \"prefix\":\"" + prefix1 + "\", \"nextHop\":\"" + nextHop1
                + "\"}";
        result = getJsonResult(baseURL + "default/" + name1, "POST", requestBody);
        Assert.assertEquals(201, httpResponseCode.intValue());

        requestBody = "{\"name\":\"" + name2 + "\", \"prefix\":\"" + prefix2 + "\", \"nextHop\":\"" + nextHop2 + "\"}";
        result = getJsonResult(baseURL + "default/" + name2, "POST", requestBody);
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test Get all static routes
        result = getJsonResult(baseURL + "default");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONArray staticRoutes = json.getJSONArray("staticRoute");
        Assert.assertEquals(2, staticRoutes.length());
        JSONObject route;
        for (int i = 0; i < staticRoutes.length(); i++) {
            route = staticRoutes.getJSONObject(i);
            if (route.getString("name").equals(name1)) {
                Assert.assertEquals(prefix1, route.getString("prefix"));
                Assert.assertEquals(nextHop1, route.getString("nextHop"));
            } else if (route.getString("name").equals(name2)) {
                Assert.assertEquals(prefix2, route.getString("prefix"));
                Assert.assertEquals(nextHop2, route.getString("nextHop"));
            } else {
                // static route has unknown name
                Assert.assertTrue(false);
            }
        }

        // Test get specific static route
        result = getJsonResult(baseURL + "default/" + name1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertEquals(name1, json.getString("name"));
        Assert.assertEquals(prefix1, json.getString("prefix"));
        Assert.assertEquals(nextHop1, json.getString("nextHop"));

        result = getJsonResult(baseURL + "default/" + name2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertEquals(name2, json.getString("name"));
        Assert.assertEquals(prefix2, json.getString("prefix"));
        Assert.assertEquals(nextHop2, json.getString("nextHop"));

        // Test delete static route
        result = getJsonResult(baseURL + "default/" + name1, "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "default");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONObject singleStaticRoute = json.getJSONObject("staticRoute");
        Assert.assertEquals(name2, singleStaticRoute.getString("name"));

    }

    @Test
    public void testSwitchManager() throws JSONException {
        System.out.println("Starting SwitchManager JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/switch/default/";

        // define Node/NodeConnector attributes for test
        int nodeId_1 = 51966;
        int nodeId_2 = 3366;
        int nodeId_3 = 4477;
        int nodeConnectorId_1 = 51966;
        int nodeConnectorId_2 = 12;
        int nodeConnectorId_3 = 34;
        String nodeType = "STUB";
        String ncType = "STUB";
        int timestamp_1 = 100000;
        String timestampName_1 = "connectedSince";
        int actionsValue_1 = 2;
        int capabilitiesValue_1 = 3;
        int tablesValue_1 = 1;
        int buffersValue_1 = 1;
        int ncState = 1;
        int ncCapabilities = 1;
        int ncBandwidth = 1000000000;

        // Test GET all nodes

        String result = getJsonResult(baseURL + "nodes");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);

        // Test for first node
        JSONObject node = getJsonInstance(json, "nodeProperties", nodeId_1);
        Assert.assertNotNull(node);
        testNodeProperties(node, nodeId_1, nodeType, timestamp_1, timestampName_1, actionsValue_1, capabilitiesValue_1,
                tablesValue_1, buffersValue_1);

        // Test 2nd node, properties of 2nd node same as first node
        node = getJsonInstance(json, "nodeProperties", nodeId_2);
        Assert.assertNotNull(node);
        testNodeProperties(node, nodeId_2, nodeType, timestamp_1, timestampName_1, actionsValue_1, capabilitiesValue_1,
                tablesValue_1, buffersValue_1);

        // Test 3rd node, properties of 3rd node same as first node
        node = getJsonInstance(json, "nodeProperties", nodeId_3);
        Assert.assertNotNull(node);
        testNodeProperties(node, nodeId_3, nodeType, timestamp_1, timestampName_1, actionsValue_1, capabilitiesValue_1,
                tablesValue_1, buffersValue_1);

        // Test GET nodeConnectors of a node
        // Test first node
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONObject nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test second node
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_2, ncType, nodeId_2, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test third node
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_3);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");
        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_3, ncType, nodeId_3, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test delete node property
        // Delete timestamp property from node1
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1 + "/property/timeStamp", "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        // Check node1
        result = getJsonResult(baseURL + "nodes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node = getJsonInstance(json, "nodeProperties", nodeId_1);
        Assert.assertNotNull(node);
        testNodeProperties(node, nodeId_1, nodeType, null, null, actionsValue_1, capabilitiesValue_1, tablesValue_1,
                buffersValue_1);

        // Delete actions property from node2
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_2 + "/property/actions", "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        // Check node2
        result = getJsonResult(baseURL + "nodes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node = getJsonInstance(json, "nodeProperties", nodeId_2);
        Assert.assertNotNull(node);
        testNodeProperties(node, nodeId_2, nodeType, timestamp_1, timestampName_1, null, capabilitiesValue_1,
                tablesValue_1, buffersValue_1);

        // Test add property to node
        // Add Tier and Bandwidth property to node1
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1 + "/property/tier/1001", "PUT");
        Assert.assertEquals(201, httpResponseCode.intValue());
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1 + "/property/bandwidth/1002", "PUT");
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test for first node
        result = getJsonResult(baseURL + "nodes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node = getJsonInstance(json, "nodeProperties", nodeId_1);
        Assert.assertNotNull(node);
        Assert.assertEquals(1001, node.getJSONObject("properties").getJSONObject("tier").getInt("tierValue"));
        Assert.assertEquals(1002, node.getJSONObject("properties").getJSONObject("bandwidth").getInt("bandwidthValue"));

        // Test delete nodeConnector property
        // Delete state property of nodeconnector1
        result = getJsonResult(baseURL + "nodeconnector/STUB/" + nodeId_1 + "/STUB/" + nodeConnectorId_1
                + "/property/state", "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, null,
                ncCapabilities, ncBandwidth);

        // Delete capabilities property of nodeconnector2
        result = getJsonResult(baseURL + "nodeconnector/STUB/" + nodeId_2 + "/STUB/" + nodeConnectorId_2
                + "/property/capabilities", "DELETE");
        Assert.assertEquals(200, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "node/STUB/" + nodeId_2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_2, ncType, nodeId_2, nodeType, ncState,
                null, ncBandwidth);

        // Test PUT nodeConnector property
        int newBandwidth = 1001;

        // Add Name/Bandwidth property to nodeConnector1
        result = getJsonResult(baseURL + "nodeconnector/STUB/" + nodeId_1 + "/STUB/" + nodeConnectorId_1
                + "/property/bandwidth/" + newBandwidth, "PUT");
        Assert.assertEquals(201, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorProperties = json.getJSONObject("nodeConnectorProperties");

        // Check for new bandwidth value, state value removed from previous
        // test
        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, null,
                ncCapabilities, newBandwidth);

    }

    @Test
    public void testStatistics() throws JSONException {
        String actionTypes[] = { "drop", "loopback", "flood", "floodAll", "controller", "swPath", "hwPath", "output",
                "setDlSrc", "setDlDst", "setDlType", "setVlanId", "setVlanPcp", "setVlanCfi", "popVlan", "pushVlan",
                "setNwSrc", "setNwDst", "setNwTos", "setTpSrc", "setTpDst" };
        System.out.println("Starting Statistics JAXB client.");

        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/statistics/default/";

        String result = getJsonResult(baseURL + "flowstats");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        JSONObject flowStatistics = getJsonInstance(json, "flowStatistics", 0xCAFE);
        JSONObject node = flowStatistics.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node.getInt("@id") == 0xCAFE);
        Assert.assertEquals(node.getString("@type"), "STUB");

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
        JSONObject portStatistics = getJsonInstance(json, "portStatistics", 0xCAFE);
        JSONObject node2 = portStatistics.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node2.getInt("@id") == 0xCAFE);
        Assert.assertEquals(node2.getString("@type"), "STUB");

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
        Assert.assertEquals(node.getString("@type"), "STUB");

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
        Assert.assertEquals(node2.getString("@type"), "STUB");

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
    }

    private void testFlowStat(JSONObject flowStat, String actionType) throws JSONException {
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

        JSONObject match = (flow.getJSONObject("match").getJSONObject("matchField"));
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
            byte srcMatch[] = { (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1 };
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
            byte dstMatch[] = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
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
    }

    @Test
    public void testFlowProgrammer() throws JSONException {
        System.out.println("Starting FlowProgrammer JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/flow/default/";
        // Attempt to get a flow that doesn't exit. Should return 404
        // status.
        String result = getJsonResult(baseURL + "STUB/51966/test1", "GET");
        Assert.assertTrue(result.equals("404"));

        // test add flow1
        String fc = "{\"dynamic\":\"false\", \"name\":\"test1\", \"node\":{\"@id\":\"51966\",\"@type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "STUB/51966/test1", "POST", fc);
        Assert.assertTrue(httpResponseCode == 201);

        // test get returns flow that was added.
        result = getJsonResult(baseURL + "STUB/51966/test1", "GET");
        // check that result came out fine.
        Assert.assertTrue(httpResponseCode == 200);
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        Assert.assertEquals(json.getString("name"), "test1");
        Assert.assertEquals(json.getString("actions"), "DROP");
        Assert.assertEquals(json.getString("installInHw"), "true");
        JSONObject node = json.getJSONObject("node");
        Assert.assertEquals(node.getString("@type"), "STUB");
        Assert.assertEquals(node.getString("@id"), "51966");
        // test adding same flow again fails due to repeat name..return 409
        // code
        result = getJsonResult(baseURL + "STUB/51966/test1", "POST", fc);
        Assert.assertTrue(result.equals("409"));

        fc = "{\"dynamic\":\"false\", \"name\":\"test2\", \"node\":{\"@id\":\"51966\",\"@type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "STUB/51966/test2", "POST", fc);
        // test should return 500 for error due to same flow being added.
        Assert.assertTrue(result.equals("500"));

        // add second flow that's different
        fc = "{\"dynamic\":\"false\", \"name\":\"test2\", \"nwSrc\":\"1.1.1.1\", \"node\":{\"@id\":\"51966\",\"@type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "STUB/51966/test2", "POST", fc);
        Assert.assertTrue(httpResponseCode == 201);

        // check that request returns both flows given node.
        result = getJsonResult(baseURL + "STUB/51966/", "GET");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        Assert.assertTrue(json.get("flowConfig") instanceof JSONArray);
        JSONArray ja = json.getJSONArray("flowConfig");
        Integer count = ja.length();
        Assert.assertTrue(count == 2);

        // check that request returns both flows given just container.
        result = getJsonResult(baseURL);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        Assert.assertTrue(json.get("flowConfig") instanceof JSONArray);
        ja = json.getJSONArray("flowConfig");
        count = ja.length();
        Assert.assertTrue(count == 2);

        // delete a flow, check that it's no longer in list.
        result = getJsonResult(baseURL + "STUB/51966/test2", "DELETE");
        Assert.assertTrue(httpResponseCode == 200);

        result = getJsonResult(baseURL + "STUB/51966/test2", "GET");
        Assert.assertTrue(result.equals("404"));
    }

    // method to extract a JSONObject with specified node ID from a JSONObject
    // that may contain an array of JSONObjects
    // This is specifically written for statistics manager northbound REST
    // interface
    // array_name should be either "flowStatistics" or "portStatistics"
    private JSONObject getJsonInstance(JSONObject json, String array_name, Integer nodeId) throws JSONException {
        JSONObject result = null;
        if (json.get(array_name) instanceof JSONArray) {
            JSONArray json_array = json.getJSONArray(array_name);
            for (int i = 0; i < json_array.length(); i++) {
                result = json_array.getJSONObject(i);
                Integer nid = result.getJSONObject("node").getInt("@id");
                if (nid.equals(nodeId))
                    break;
            }
        } else {
            result = json.getJSONObject(array_name);
            Integer nid = result.getJSONObject("node").getInt("@id");
            if (!nid.equals(nodeId))
                result = null;
        }
        return result;
    }

    // a class to construct query parameter for HTTP request
    private class QueryParameter {
        StringBuilder queryString = null;

        // constructor
        QueryParameter(String key, String value) {
            queryString = new StringBuilder();
            queryString.append("?").append(key).append("=").append(value);
        }

        // method to add more query parameter
        QueryParameter add(String key, String value) {
            this.queryString.append("&").append(key).append("=").append(value);
            return this;
        }

        // method to get the query parameter string
        String getString() {
            return this.queryString.toString();
        }

    }

    @Test
    public void testHostTracker() throws JSONException {

        System.out.println("Starting HostTracker JAXB client.");

        // setup 2 host models for @POST method
        // 1st host
        String networkAddress_1 = "192.168.0.8";
        String dataLayerAddress_1 = "11:22:33:44:55:66";
        String nodeType_1 = "STUB";
        Integer nodeId_1 = 3366;
        String nodeConnectorType_1 = "STUB";
        Integer nodeConnectorId_1 = 12;
        String vlan_1 = "4";

        // 2nd host
        String networkAddress_2 = "10.1.1.1";
        String dataLayerAddress_2 = "1A:2B:3C:4D:5E:6F";
        String nodeType_2 = "STUB";
        Integer nodeId_2 = 4477;
        String nodeConnectorType_2 = "STUB";
        Integer nodeConnectorId_2 = 34;
        String vlan_2 = "0";

        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/host/default";

        // test POST method: addHost()
        String queryParameter = new QueryParameter("dataLayerAddress", dataLayerAddress_1).add("nodeType", nodeType_1)
                .add("nodeId", nodeId_1.toString()).add("nodeConnectorType", nodeConnectorType_1)
                .add("nodeConnectorId", nodeConnectorId_1.toString()).add("vlan", vlan_1).getString();

        String result = getJsonResult(baseURL + "/" + networkAddress_1 + queryParameter, "POST");
        Assert.assertTrue(httpResponseCode == 201);

        // vlan is not passed through query parameter but should be
        // defaulted to "0"
        queryParameter = new QueryParameter("dataLayerAddress", dataLayerAddress_2).add("nodeType", nodeType_2)
                .add("nodeId", nodeId_2.toString()).add("nodeConnectorType", nodeConnectorType_2)
                .add("nodeConnectorId", nodeConnectorId_2.toString()).getString();

        result = getJsonResult(baseURL + "/" + networkAddress_2 + queryParameter, "POST");
        Assert.assertTrue(httpResponseCode == 201);

        // define variables for decoding returned strings
        String networkAddress;
        JSONObject host_jo, dl_jo, nc_jo, node_jo;

        // the two hosts should be in inactive host DB
        // test GET method: getInactiveHosts()
        result = getJsonResult(baseURL + "/inactive", "GET");
        Assert.assertTrue(httpResponseCode == 200);

        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        // there should be at least two hosts in the DB
        Assert.assertTrue(json.get("host") instanceof JSONArray);
        JSONArray ja = json.getJSONArray("host");
        Integer count = ja.length();
        Assert.assertTrue(count == 2);

        for (int i = 0; i < count; i++) {
            host_jo = ja.getJSONObject(i);
            dl_jo = host_jo.getJSONObject("dataLayerAddress");
            nc_jo = host_jo.getJSONObject("nodeConnector");
            node_jo = nc_jo.getJSONObject("node");

            networkAddress = host_jo.getString("networkAddress");
            if (networkAddress.equalsIgnoreCase(networkAddress_1)) {
                Assert.assertTrue(dl_jo.getString("macAddress").equalsIgnoreCase(dataLayerAddress_1));
                Assert.assertTrue(nc_jo.getString("@type").equalsIgnoreCase(nodeConnectorType_1));
                Assert.assertTrue(nc_jo.getInt("@id") == nodeConnectorId_1);
                Assert.assertTrue(node_jo.getString("@type").equalsIgnoreCase(nodeType_1));
                Assert.assertTrue(node_jo.getInt("@id") == nodeId_1);
                Assert.assertTrue(host_jo.getString("vlan").equalsIgnoreCase(vlan_1));
            } else if (networkAddress.equalsIgnoreCase(networkAddress_2)) {
                Assert.assertTrue(dl_jo.getString("macAddress").equalsIgnoreCase(dataLayerAddress_2));
                Assert.assertTrue(nc_jo.getString("@type").equalsIgnoreCase(nodeConnectorType_2));
                Assert.assertTrue(nc_jo.getInt("@id") == nodeConnectorId_2);
                Assert.assertTrue(node_jo.getString("@type").equalsIgnoreCase(nodeType_2));
                Assert.assertTrue(node_jo.getInt("@id") == nodeId_2);
                Assert.assertTrue(host_jo.getString("vlan").equalsIgnoreCase(vlan_2));
            } else {
                Assert.assertTrue(false);
            }
        }

        // test GET method: getActiveHosts() - no host expected
        result = getJsonResult(baseURL, "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        Assert.assertFalse(hostInJson(json, networkAddress_1));
        Assert.assertFalse(hostInJson(json, networkAddress_2));

        // put the 1st host into active host DB
        Node nd;
        NodeConnector ndc;
        try {
            nd = new Node(nodeType_1, nodeId_1);
            ndc = new NodeConnector(nodeConnectorType_1, nodeConnectorId_1, nd);
            this.invtoryListener.notifyNodeConnector(ndc, UpdateType.ADDED, null);
        } catch (ConstructionException e) {
            ndc = null;
            nd = null;
        }

        // verify the host shows up in active host DB

        result = getJsonResult(baseURL, "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertTrue(hostInJson(json, networkAddress_1));

        // test GET method for getHostDetails()

        result = getJsonResult(baseURL + "/" + networkAddress_1, "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertFalse(json.length() == 0);

        dl_jo = json.getJSONObject("dataLayerAddress");
        nc_jo = json.getJSONObject("nodeConnector");
        node_jo = nc_jo.getJSONObject("node");

        Assert.assertTrue(json.getString("networkAddress").equalsIgnoreCase(networkAddress_1));
        Assert.assertTrue(dl_jo.getString("macAddress").equalsIgnoreCase(dataLayerAddress_1));
        Assert.assertTrue(nc_jo.getString("@type").equalsIgnoreCase(nodeConnectorType_1));
        Assert.assertTrue(Integer.parseInt(nc_jo.getString("@id")) == nodeConnectorId_1);
        Assert.assertTrue(node_jo.getString("@type").equalsIgnoreCase(nodeType_1));
        Assert.assertTrue(Integer.parseInt(node_jo.getString("@id")) == nodeId_1);
        Assert.assertTrue(json.getString("vlan").equalsIgnoreCase(vlan_1));

        // test DELETE method for deleteFlow()

        result = getJsonResult(baseURL + "/" + networkAddress_1, "DELETE");
        Assert.assertTrue(httpResponseCode == 200);

        // verify host_1 removed from active host DB
        // test GET method: getActiveHosts() - no host expected

        result = getJsonResult(baseURL, "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertFalse(hostInJson(json, networkAddress_1));

    }

    private Boolean hostInJson(JSONObject json, String hostIp) throws JSONException {
        // input JSONObject may be empty
        if (json.length() == 0) {
            return false;
        }
        if (json.get("host") instanceof JSONArray) {
            JSONArray ja = json.getJSONArray("host");
            for (int i = 0; i < ja.length(); i++) {
                String na = ja.getJSONObject(i).getString("networkAddress");
                if (na.equalsIgnoreCase(hostIp))
                    return true;
            }
            return false;
        } else {
            String na = json.getJSONObject("host").getString("networkAddress");
            return (na.equalsIgnoreCase(hostIp)) ? true : false;
        }
    }

    @Test
    public void testTopology() throws JSONException, ConstructionException {
        System.out.println("Starting Topology JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/topology/default";

        // === test GET method for getTopology()
        short state_1 = State.EDGE_UP, state_2 = State.EDGE_DOWN;
        long bw_1 = Bandwidth.BW10Gbps, bw_2 = Bandwidth.BW100Mbps;
        long lat_1 = Latency.LATENCY100ns, lat_2 = Latency.LATENCY1ms;
        String nodeType = "STUB";
        String nodeConnType = "STUB";
        int headNC1_nodeId = 1, headNC1_nodeConnId = 11;
        int tailNC1_nodeId = 2, tailNC1_nodeConnId = 22;
        int headNC2_nodeId = 2, headNC2_nodeConnId = 21;
        int tailNC2_nodeId = 1, tailNC2_nodeConnId = 12;

        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        NodeConnector headnc1 = new NodeConnector(nodeConnType, headNC1_nodeConnId, new Node(nodeType, headNC1_nodeId));
        NodeConnector tailnc1 = new NodeConnector(nodeConnType, tailNC1_nodeConnId, new Node(nodeType, tailNC1_nodeId));
        Edge e1 = new Edge(tailnc1, headnc1);
        Set<Property> props_1 = new HashSet<Property>();
        props_1.add(new State(state_1));
        props_1.add(new Bandwidth(bw_1));
        props_1.add(new Latency(lat_1));
        TopoEdgeUpdate teu1 = new TopoEdgeUpdate(e1, props_1, UpdateType.ADDED);
        topoedgeupdateList.add(teu1);

        NodeConnector headnc2 = new NodeConnector(nodeConnType, headNC2_nodeConnId, new Node(nodeType, headNC2_nodeId));
        NodeConnector tailnc2 = new NodeConnector(nodeConnType, tailNC2_nodeConnId, new Node(nodeType, tailNC2_nodeId));
        Edge e2 = new Edge(tailnc2, headnc2);
        Set<Property> props_2 = new HashSet<Property>();
        props_2.add(new State(state_2));
        props_2.add(new Bandwidth(bw_2));
        props_2.add(new Latency(lat_2));

        TopoEdgeUpdate teu2 = new TopoEdgeUpdate(e2, props_2, UpdateType.ADDED);
        topoedgeupdateList.add(teu2);

        topoUpdates.edgeUpdate(topoedgeupdateList);

        // HTTP request
        String result = getJsonResult(baseURL, "GET");
        Assert.assertTrue(httpResponseCode == 200);
        if (debugMsg) {
            System.out.println("Get Topology: " + result);
        }

        // returned data must be an array of edges
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        Assert.assertTrue(json.get("edgeProperties") instanceof JSONArray);
        JSONArray ja = json.getJSONArray("edgeProperties");

        for (int i = 0; i < ja.length(); i++) {
            JSONObject edgeProp = ja.getJSONObject(i);
            JSONObject edge = edgeProp.getJSONObject("edge");
            JSONObject tailNC = edge.getJSONObject("tailNodeConnector");
            JSONObject tailNode = tailNC.getJSONObject("node");

            JSONObject headNC = edge.getJSONObject("headNodeConnector");
            JSONObject headNode = headNC.getJSONObject("node");
            JSONObject Props = edgeProp.getJSONObject("properties");
            JSONObject bandw = Props.getJSONObject("bandwidth");
            JSONObject stt = Props.getJSONObject("state");
            JSONObject ltc = Props.getJSONObject("latency");

            if (headNC.getInt("@id") == headNC1_nodeConnId) {
                Assert.assertEquals(headNode.getString("@type"), nodeType);
                Assert.assertEquals(headNode.getLong("@id"), headNC1_nodeId);
                Assert.assertEquals(headNC.getString("@type"), nodeConnType);
                Assert.assertEquals(tailNode.getString("@type"),nodeType);
                Assert.assertEquals(tailNode.getString("@type"), nodeConnType);
                Assert.assertEquals(tailNC.getLong("@id"), tailNC1_nodeConnId);
                Assert.assertEquals(bandw.getLong("bandwidthValue"), bw_1);
                Assert.assertTrue((short) stt.getInt("stateValue") == state_1);
                Assert.assertEquals(ltc.getLong("latencyValue"), lat_1);
            } else if (headNC.getInt("@id") == headNC2_nodeConnId) {
                Assert.assertEquals(headNode.getString("@type"),nodeType);
                Assert.assertEquals(headNode.getLong("@id"), headNC2_nodeId);
                Assert.assertEquals(headNC.getString("@type"), nodeConnType);
                Assert.assertEquals(tailNode.getString("@type"), nodeType);
                Assert.assertTrue(tailNode.getInt("@id") == tailNC2_nodeId);
                Assert.assertEquals(tailNC.getString("@type"), nodeConnType);
                Assert.assertEquals(tailNC.getLong("@id"), tailNC2_nodeConnId);
                Assert.assertEquals(bandw.getLong("bandwidthValue"), bw_2);
                Assert.assertTrue((short) stt.getInt("stateValue") == state_2);
                Assert.assertEquals(ltc.getLong("latencyValue"), lat_2);
            }
        }

        // === test POST method for addUserLink()
        // define 2 sample nodeConnectors for user link configuration tests
        String nodeType_1 = "STUB";
        Integer nodeId_1 = 3366;
        String nodeConnectorType_1 = "STUB";
        Integer nodeConnectorId_1 = 12;

        String nodeType_2 = "STUB";
        Integer nodeId_2 = 4477;
        String nodeConnectorType_2 = "STUB";
        Integer nodeConnectorId_2 = 34;

        JSONObject jo = new JSONObject()
                .append("name", "userLink_1")
                .append("srcNodeConnector",
                        nodeConnectorType_1 + "|" + nodeConnectorId_1 + "@" + nodeType_1 + "|" + nodeId_1)
                .append("dstNodeConnector",
                        nodeConnectorType_2 + "|" + nodeConnectorId_2 + "@" + nodeType_2 + "|" + nodeId_2);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "/userLink", "POST", jo.toString());
        Assert.assertTrue(httpResponseCode == 201);

        // === test GET method for getUserLinks()
        result = getJsonResult(baseURL + "/userLink", "GET");
        Assert.assertTrue(httpResponseCode == 200);
        if (debugMsg) {
            System.out.println("result:" + result);
        }

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        // should have at least one object returned
        Assert.assertFalse(json.length() == 0);
        JSONObject userlink = new JSONObject();

        if (json.get("userLinks") instanceof JSONArray) {
            ja = json.getJSONArray("userLinks");
            int i;
            for (i = 0; i < ja.length(); i++) {
                userlink = ja.getJSONObject(i);
                if (userlink.getString("name").equalsIgnoreCase("userLink_1"))
                    break;
            }
            Assert.assertFalse(i == ja.length());
        } else {
            userlink = json.getJSONObject("userLinks");
            Assert.assertTrue(userlink.getString("name").equalsIgnoreCase("userLink_1"));
        }

        String[] level_1, level_2;
        level_1 = userlink.getString("srcNodeConnector").split("\\@");
        level_2 = level_1[0].split("\\|");
        Assert.assertTrue(level_2[0].equalsIgnoreCase(nodeConnectorType_1));
        Assert.assertTrue(Integer.parseInt(level_2[1]) == nodeConnectorId_1);
        level_2 = level_1[1].split("\\|");
        Assert.assertTrue(level_2[0].equalsIgnoreCase(nodeType_1));
        Assert.assertTrue(Integer.parseInt(level_2[1]) == nodeId_1);
        level_1 = userlink.getString("dstNodeConnector").split("\\@");
        level_2 = level_1[0].split("\\|");
        Assert.assertTrue(level_2[0].equalsIgnoreCase(nodeConnectorType_2));
        Assert.assertTrue(Integer.parseInt(level_2[1]) == nodeConnectorId_2);
        level_2 = level_1[1].split("\\|");
        Assert.assertTrue(level_2[0].equalsIgnoreCase(nodeType_2));
        Assert.assertTrue(Integer.parseInt(level_2[1]) == nodeId_2);

        // === test DELETE method for deleteUserLink()
        String userName = "userLink_1";
        result = getJsonResult(baseURL + "/userLink/" + userName, "DELETE");
        Assert.assertTrue(httpResponseCode == 200);

        // execute another getUserLinks() request to verify that userLink_1 is
        // removed
        result = getJsonResult(baseURL + "/userLink", "GET");
        Assert.assertTrue(httpResponseCode == 200);
        if (debugMsg) {
            System.out.println("result:" + result);
        }
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        if (json.length() != 0) {
            if (json.get("userLinks") instanceof JSONArray) {
                ja = json.getJSONArray("userLinks");
                for (int i = 0; i < ja.length(); i++) {
                    userlink = ja.getJSONObject(i);
                    Assert.assertFalse(userlink.getString("name").equalsIgnoreCase("userLink_1"));
                }
            } else {
                userlink = json.getJSONObject("userLinks");
                Assert.assertFalse(userlink.getString("name").equalsIgnoreCase("userLink_1"));
            }
        }
    }

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                systemProperty("org.eclipse.gemini.web.tomcat.config.path").value(
                        PathUtils.getBaseDir() + "/src/test/resources/tomcat-server.xml"),

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
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager", "3.1.0"),

                // the plugin stub to get data for the tests
                mavenBundle("org.opendaylight.controller", "protocol_plugins.stub", "0.4.0-SNAPSHOT"),

                // List all the opendaylight modules
                mavenBundle("org.opendaylight.controller", "configuration", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "containermanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "containermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.services", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.services-implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "security", "0.4.0-SNAPSHOT").noStart(),
                mavenBundle("org.opendaylight.controller", "sal", "0.5.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "sal.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "forwardingrulesmanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "forwardingrulesmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "statisticsmanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "statisticsmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "routing.dijkstra_implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "topologymanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "usermanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "usermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "logging.bridge", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.test", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "forwarding.staticrouting", "0.4.0-SNAPSHOT"),

                // Northbound bundles
                mavenBundle("org.opendaylight.controller", "commons.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "forwarding.staticrouting.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "statistics.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "topology.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "flowprogrammer.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "subnets.northbound", "0.4.0-SNAPSHOT"),

                mavenBundle("org.codehaus.jackson", "jackson-mapper-asl", "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-core-asl", "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-jaxrs", "1.9.8"),
                mavenBundle("org.codehaus.jettison", "jettison", "1.3.3"),

                mavenBundle("commons-io", "commons-io", "2.3"),

                mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),

                mavenBundle("equinoxSDK381", "javax.servlet", "3.0.0.v201112011016"),
                mavenBundle("equinoxSDK381", "javax.servlet.jsp", "2.2.0.v201112011158"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds", "1.4.0.v20120522-1841"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util", "1.0.400.v20120522-2049"),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services", "3.3.100.v20120522-1822"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command", "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime", "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell", "0.8.0.v201110170705"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.cm", "1.0.400.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console", "1.0.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.launcher", "1.3.0.v20120522-1813"),

                mavenBundle("geminiweb", "org.eclipse.gemini.web.core", "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.extender", "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.tomcat", "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.kernel.equinox.extensions", "3.6.0.RELEASE").noStart(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.common", "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.io", "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.math", "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi", "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi.manifest", "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.parser.manifest", "3.6.0.RELEASE"),

                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager", "3.1.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell", "3.0.1"),

                mavenBundle("com.google.code.gson", "gson", "2.1"),
                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec", "1.0.1.Final"),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall", "3.1.6"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("commons-codec", "commons-codec"),
                mavenBundle("virgomirror", "org.eclipse.jdt.core.compiler.batch", "3.8.0.I20120518-2145"),
                mavenBundle("eclipselink", "javax.persistence", "2.0.4.v201112161009"),

                mavenBundle("orbit", "javax.activation", "1.1.0.v201211130549"),
                mavenBundle("orbit", "javax.annotation", "1.1.0.v201209060031"),
                mavenBundle("orbit", "javax.ejb", "3.1.1.v201204261316"),
                mavenBundle("orbit", "javax.el", "2.2.0.v201108011116"),
                mavenBundle("orbit", "javax.mail.glassfish", "1.4.1.v201108011116"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("orbit", "org.apache.catalina", "7.0.32.v201211201336"),
                // these are bundle fragments that can't be started on its own
                mavenBundle("orbit", "org.apache.catalina.ha", "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.catalina.tribes", "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.coyote", "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.jasper", "7.0.32.v201211201952").noStart(),

                mavenBundle("orbit", "org.apache.el", "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.juli.extras", "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.api", "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.util", "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl", "1.2.0.v201105211821"),
                mavenBundle("orbit", "javax.servlet.jsp.jstl.impl", "1.2.0.v201210211230"),

                mavenBundle("org.ops4j.pax.exam", "pax-exam-container-native"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-junit4"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-link-mvn"),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether"),

                mavenBundle("org.springframework", "org.springframework.asm", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.aop", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.context", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.context.support", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.core", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.beans", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.expression", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.web", "3.1.3.RELEASE"),

                mavenBundle("org.aopalliance", "com.springsource.org.aopalliance", "1.0.0"),
                mavenBundle("org.springframework", "org.springframework.web.servlet", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security", "spring-security-config", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security", "spring-security-core", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security", "spring-security-web", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security", "spring-security-taglibs", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.transaction", "3.1.3.RELEASE"),

                mavenBundle("org.ow2.chameleon.management", "chameleon-mbeans", "1.0.0"),
                mavenBundle("org.opendaylight.controller.thirdparty", "net.sf.jung2", "2.0.1-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller.thirdparty", "com.sun.jersey.jersey-servlet", "1.17-SNAPSHOT"),

                // Jersey needs to be started before the northbound application
                // bundles, using a lower start level
                mavenBundle("com.sun.jersey", "jersey-client", "1.17"),
                mavenBundle("com.sun.jersey", "jersey-server", "1.17").startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-core", "1.17").startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-json", "1.17").startLevel(2), junitBundles());
    }

}
