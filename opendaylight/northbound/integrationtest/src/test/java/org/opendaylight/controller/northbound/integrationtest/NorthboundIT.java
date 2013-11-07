package org.opendaylight.controller.northbound.integrationtest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.controller.commons.httpclient.HTTPClient;
import org.opendaylight.controller.commons.httpclient.HTTPRequest;
import org.opendaylight.controller.commons.httpclient.HTTPResponse;
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
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class NorthboundIT {
    private final Logger log = LoggerFactory.getLogger(NorthboundIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private IUserManager userManager = null;
    private IInventoryListener invtoryListener = null;
    private IListenTopoUpdates topoUpdates = null;

    private final Boolean debugMsg = false;

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
                log.debug("Bundle:" + element.getSymbolicName() + " state:" + stateToString(state));
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
            if (body != null) {
                System.out.println("body: " + body);
            }
        }

        try {
            this.userManager.getAuthorizationList();
            this.userManager.authenticate("admin", "admin");
            HTTPRequest request = new HTTPRequest();

            request.setUri(restUrl);
            request.setMethod(method);
            request.setTimeout(0);  // HostTracker doesn't respond
                                    // within default timeout during
                                    // IT so setting an indefinite
                                    // timeout till the issue is
                                    // sorted out

            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            String authString = "admin:admin";
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);
            List<String> header = new ArrayList<String>();
            header.add("Basic "+authStringEnc);
            headers.put("Authorization", header);
            header = new ArrayList<String>();
            header.add("application/json");
            headers.put("Accept", header);
            request.setHeaders(headers);
            request.setContentType("application/json");
            if (body != null) {
                request.setEntity(body);
            }

            HTTPResponse response = HTTPClient.sendRequest(request);

            // Response code for success should be 2xx
            httpResponseCode = response.getStatus();
            if (httpResponseCode > 299) {
                return httpResponseCode.toString();
            }

            if (debugMsg) {
                System.out.println("HTTP response code: " + response.getStatus());
                System.out.println("HTTP response message: " + response.getEntity());
            }

            return response.getEntity();
        } catch (Exception e) {
            if (debugMsg) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void testNodeProperties(JSONObject node, Integer nodeId, String nodeType, Integer timestamp,
            String timestampName, Integer actionsValue, Integer capabilitiesValue, Integer tablesValue,
            Integer buffersValue) throws JSONException {

        JSONObject nodeInfo = node.getJSONObject("node");
        Assert.assertEquals(nodeId, (Integer) nodeInfo.getInt("id"));
        Assert.assertEquals(nodeType, nodeInfo.getString("type"));

        JSONObject properties = node.getJSONObject("properties");

        if (timestamp == null || timestampName == null) {
            Assert.assertFalse(properties.has("timeStamp"));
        } else {
            Assert.assertEquals(timestamp, (Integer) properties.getJSONObject("timeStamp").getInt("value"));
            Assert.assertEquals(timestampName, properties.getJSONObject("timeStamp").getString("name"));
        }
        if (actionsValue == null) {
            Assert.assertFalse(properties.has("actions"));
        } else {
            Assert.assertEquals(actionsValue, (Integer) properties.getJSONObject("actions").getInt("value"));
        }
        if (capabilitiesValue == null) {
            Assert.assertFalse(properties.has("capabilities"));
        } else {
            Assert.assertEquals(capabilitiesValue,
                    (Integer) properties.getJSONObject("capabilities").getInt("value"));
        }
        if (tablesValue == null) {
            Assert.assertFalse(properties.has("tables"));
        } else {
            Assert.assertEquals(tablesValue, (Integer) properties.getJSONObject("tables").getInt("value"));
        }
        if (buffersValue == null) {
            Assert.assertFalse(properties.has("buffers"));
        } else {
            Assert.assertEquals(buffersValue, (Integer) properties.getJSONObject("buffers").getInt("value"));
        }
    }

    private void testNodeConnectorProperties(JSONObject nodeConnectorProperties, Integer ncId, String ncType,
            Integer nodeId, String nodeType, Integer state, Integer capabilities, Integer bandwidth)
            throws JSONException {

        JSONObject nodeConnector = nodeConnectorProperties.getJSONObject("nodeconnector");
        JSONObject node = nodeConnector.getJSONObject("node");
        JSONObject properties = nodeConnectorProperties.getJSONObject("properties");

        Assert.assertEquals(ncId, (Integer) nodeConnector.getInt("id"));
        Assert.assertEquals(ncType, nodeConnector.getString("type"));
        Assert.assertEquals(nodeId, (Integer) node.getInt("id"));
        Assert.assertEquals(nodeType, node.getString("type"));
        if (state == null) {
            Assert.assertFalse(properties.has("state"));
        } else {
            Assert.assertEquals(state, (Integer) properties.getJSONObject("state").getInt("value"));
        }
        if (capabilities == null) {
            Assert.assertFalse(properties.has("capabilities"));
        } else {
            Assert.assertEquals(capabilities,
                    (Integer) properties.getJSONObject("capabilities").getInt("value"));
        }
        if (bandwidth == null) {
            Assert.assertFalse(properties.has("bandwidth"));
        } else {
            Assert.assertEquals(bandwidth, (Integer) properties.getJSONObject("bandwidth").getInt("value"));
        }
    }

    @Test
    public void testSubnetsNorthbound() throws JSONException, ConstructionException {
        System.out.println("Starting Subnets JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/subnetservice/";

        String name1 = "testSubnet1";
        String subnet1 = "1.1.1.1/24";

        String name2 = "testSubnet2";
        String subnet2 = "2.2.2.2/24";

        String name3 = "testSubnet3";
        String subnet3 = "3.3.3.3/24";

        /*
         * Create the node connector string list for the two subnets as:
         * portList2 = {"OF|1@OF|00:00:00:00:00:00:00:02", "OF|2@OF|00:00:00:00:00:00:00:02", "OF|3@OF|00:00:00:00:00:00:00:02", "OF|4@OF|00:00:00:00:00:00:00:02"};
         * portList3 = {"OF|1@OF|00:00:00:00:00:00:00:03", "OF|2@OF|00:00:00:00:00:00:00:03", "OF|3@OF|00:00:00:00:00:00:00:03"};
         */
        Node node2 = new Node(Node.NodeIDType.OPENFLOW, 2L);
        List<String> portList2 = new ArrayList<String>();
        NodeConnector nc21 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)1, node2);
        NodeConnector nc22 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)2, node2);
        NodeConnector nc23 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)3, node2);
        NodeConnector nc24 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)3, node2);
        portList2.add(nc21.toString());
        portList2.add(nc22.toString());
        portList2.add(nc23.toString());
        portList2.add(nc24.toString());

        List<String> portList3 = new ArrayList<String>();
        Node node3 = new Node(Node.NodeIDType.OPENFLOW, 3L);
        NodeConnector nc31 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)1, node3);
        NodeConnector nc32 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)2, node3);
        NodeConnector nc33 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)3, node3);
        portList3.add(nc31.toString());
        portList3.add(nc32.toString());
        portList3.add(nc33.toString());

        // Test GET subnets in default container
        String result = getJsonResult(baseURL + "default/subnets");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        JSONArray subnetConfigs = json.getJSONArray("subnetConfig");
        Assert.assertEquals(subnetConfigs.length(), 1); // should only get the default subnet

        // Test GET subnet1 expecting 404
        result = getJsonResult(baseURL + "default/subnet/" + name1);
        Assert.assertEquals(404, httpResponseCode.intValue());

        // Test POST subnet1
        JSONObject jo = new JSONObject().put("name", name1).put("subnet", subnet1);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "default/subnet/" + name1, "PUT", jo.toString());
        Assert.assertTrue(httpResponseCode == 201);

        // Test GET subnet1
        result = getJsonResult(baseURL + "default/subnet/" + name1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        Assert.assertEquals(200, httpResponseCode.intValue());
        Assert.assertEquals(name1, json.getString("name"));
        Assert.assertEquals(subnet1, json.getString("subnet"));

        // Test PUT subnet2
        JSONObject jo2 = new JSONObject().put("name", name2).put("subnet", subnet2).put("nodeConnectors", portList2);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "default/subnet/" + name2, "PUT", jo2.toString());
        Assert.assertEquals(201, httpResponseCode.intValue());
        // Test PUT subnet3
        JSONObject jo3 = new JSONObject().put("name", name3).put("subnet", subnet3);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "default/subnet/" + name3, "PUT", jo3.toString());
        Assert.assertEquals(201, httpResponseCode.intValue());
        // Test POST subnet3 (modify port list: add)
        JSONObject jo3New = new JSONObject().put("name", name3).put("subnet", subnet3).put("nodeConnectors", portList3);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "default/subnet/" + name3, "POST", jo3New.toString());
        Assert.assertEquals(200, httpResponseCode.intValue());

        // Test GET all subnets in default container
        result = getJsonResult(baseURL + "default/subnets");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONArray subnetConfigArray = json.getJSONArray("subnetConfig");
        JSONObject subnetConfig;
        Assert.assertEquals(3, subnetConfigArray.length());
        for (int i = 0; i < subnetConfigArray.length(); i++) {
            subnetConfig = subnetConfigArray.getJSONObject(i);
            if (subnetConfig.getString("name").equals(name1)) {
                Assert.assertEquals(subnet1, subnetConfig.getString("subnet"));
            } else if (subnetConfig.getString("name").equals(name2)) {
                Assert.assertEquals(subnet2, subnetConfig.getString("subnet"));
                JSONArray portListGet = subnetConfig.getJSONArray("nodeConnectors");
                Assert.assertEquals(portList2.get(0), portListGet.get(0));
                Assert.assertEquals(portList2.get(1), portListGet.get(1));
                Assert.assertEquals(portList2.get(2), portListGet.get(2));
                Assert.assertEquals(portList2.get(3), portListGet.get(3));
            } else if (subnetConfig.getString("name").equals(name3)) {
                Assert.assertEquals(subnet3, subnetConfig.getString("subnet"));
                JSONArray portListGet = subnetConfig.getJSONArray("nodeConnectors");
                Assert.assertEquals(portList3.get(0), portListGet.get(0));
                Assert.assertEquals(portList3.get(1), portListGet.get(1));
                Assert.assertEquals(portList3.get(2), portListGet.get(2));
            } else {
                // Unexpected config name
                Assert.assertTrue(false);
            }
        }

        // Test POST subnet2 (modify port list: remove one port only)
        List<String> newPortList2 = new ArrayList<String>(portList2);
        newPortList2.remove(3);
        JSONObject jo2New = new JSONObject().put("name", name2).put("subnet", subnet2).put("nodeConnectors", newPortList2);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "default/subnet/" + name2, "POST", jo2New.toString());
        Assert.assertEquals(200, httpResponseCode.intValue());

        // Test GET subnet2: verify contains only the first three ports
        result = getJsonResult(baseURL + "default/subnet/" + name2);
        jt = new JSONTokener(result);
        subnetConfig = new JSONObject(jt);
        Assert.assertEquals(200, httpResponseCode.intValue());
        JSONArray portListGet2 = subnetConfig.getJSONArray("nodeConnectors");
        Assert.assertEquals(portList2.get(0), portListGet2.get(0));
        Assert.assertEquals(portList2.get(1), portListGet2.get(1));
        Assert.assertEquals(portList2.get(2), portListGet2.get(2));
        Assert.assertTrue(portListGet2.length() == 3);

        // Test DELETE subnet1
        result = getJsonResult(baseURL + "default/subnet/" + name1, "DELETE");
        Assert.assertEquals(204, httpResponseCode.intValue());

        // Test GET deleted subnet1
        result = getJsonResult(baseURL + "default/subnet/" + name1);
        Assert.assertEquals(404, httpResponseCode.intValue());

        // TEST PUT bad subnet, expect 400, validate JSON exception mapper
        JSONObject joBad = new JSONObject().put("foo", "bar");
        result = getJsonResult(baseURL + "default/subnet/foo", "PUT", joBad.toString());
        Assert.assertEquals(400, httpResponseCode.intValue());
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
        String result = getJsonResult(baseURL + "default/routes");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        JSONArray staticRoutes = json.getJSONArray("staticRoute");
        Assert.assertEquals(staticRoutes.length(), 0);

        // Test insert static route
        String requestBody = "{\"name\":\"" + name1 + "\", \"prefix\":\"" + prefix1 + "\", \"nextHop\":\"" + nextHop1
                + "\"}";
        result = getJsonResult(baseURL + "default/route/" + name1, "PUT", requestBody);
        Assert.assertEquals(201, httpResponseCode.intValue());

        requestBody = "{\"name\":\"" + name2 + "\", \"prefix\":\"" + prefix2 + "\", \"nextHop\":\"" + nextHop2 + "\"}";
        result = getJsonResult(baseURL + "default/route/" + name2, "PUT", requestBody);
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test Get all static routes
        result = getJsonResult(baseURL + "default/routes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONArray staticRouteArray = json.getJSONArray("staticRoute");
        Assert.assertEquals(2, staticRouteArray.length());
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
        result = getJsonResult(baseURL + "default/route/" + name1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertEquals(name1, json.getString("name"));
        Assert.assertEquals(prefix1, json.getString("prefix"));
        Assert.assertEquals(nextHop1, json.getString("nextHop"));

        result = getJsonResult(baseURL + "default/route/" + name2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertEquals(name2, json.getString("name"));
        Assert.assertEquals(prefix2, json.getString("prefix"));
        Assert.assertEquals(nextHop2, json.getString("nextHop"));

        // Test delete static route
        result = getJsonResult(baseURL + "default/route/" + name1, "DELETE");
        Assert.assertEquals(204, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "default/routes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        staticRouteArray = json.getJSONArray("staticRoute");
        JSONObject singleStaticRoute = staticRouteArray.getJSONObject(0);
        Assert.assertEquals(name2, singleStaticRoute.getString("name"));

    }

    @Test
    public void testSwitchManager() throws JSONException {
        System.out.println("Starting SwitchManager JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/switchmanager/default/";

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
        JSONArray nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        JSONObject nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test second node
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);


        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_2, ncType, nodeId_2, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test third node
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_3);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);
        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_3, ncType, nodeId_3, nodeType, ncState,
                ncCapabilities, ncBandwidth);

        // Test add property to node
        // Add Tier and Description property to node1
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1 + "/property/tier/1001", "PUT");
        Assert.assertEquals(201, httpResponseCode.intValue());
        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1 + "/property/description/node1", "PUT");
        Assert.assertEquals(201, httpResponseCode.intValue());

        // Test for first node
        result = getJsonResult(baseURL + "nodes");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node = getJsonInstance(json, "nodeProperties", nodeId_1);
        Assert.assertNotNull(node);
        Assert.assertEquals(1001, node.getJSONObject("properties").getJSONObject("tier").getInt("value"));
        Assert.assertEquals("node1", node.getJSONObject("properties").getJSONObject("description").getString("value"));

        // Test delete nodeConnector property
        // Delete state property of nodeconnector1
        result = getJsonResult(baseURL + "nodeconnector/STUB/" + nodeId_1 + "/STUB/" + nodeConnectorId_1
                + "/property/state", "DELETE");
        Assert.assertEquals(204, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "node/STUB/" + nodeId_1);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);

        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, null,
                ncCapabilities, ncBandwidth);

        // Delete capabilities property of nodeconnector2
        result = getJsonResult(baseURL + "nodeconnector/STUB/" + nodeId_2 + "/STUB/" + nodeConnectorId_2
                + "/property/capabilities", "DELETE");
        Assert.assertEquals(204, httpResponseCode.intValue());

        result = getJsonResult(baseURL + "node/STUB/" + nodeId_2);
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);

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
        nodeConnectorPropertiesArray = json.getJSONArray("nodeConnectorProperties");
        nodeConnectorProperties = nodeConnectorPropertiesArray.getJSONObject(0);

        // Check for new bandwidth value, state value removed from previous
        // test
        testNodeConnectorProperties(nodeConnectorProperties, nodeConnectorId_1, ncType, nodeId_1, nodeType, null,
                ncCapabilities, newBandwidth);

    }

    @Test
    public void testStatistics() throws JSONException {
        final String actionTypes[] = { "DROP", "LOOPBACK", "FLOOD", "FLOOD_ALL", "CONTROLLER", "SW_PATH", "HW_PATH", "OUTPUT",
                "SET_DL_SRC", "SET_DL_DST", "SET_DL_TYPE", "SET_VLAN_ID", "SET_VLAN_PCP", "SET_VLAN_CFI", "POP_VLAN", "PUSH_VLAN",
                "SET_NW_SRC", "SET_NW_DST", "SET_NW_TOS", "SET_TP_SRC", "SET_TP_DST" };
        System.out.println("Starting Statistics JAXB client.");

        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/statistics/default/";

        String result = getJsonResult(baseURL + "flow");
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        JSONObject flowStatistics = getJsonInstance(json, "flowStatistics", 0xCAFE);
        JSONObject node = flowStatistics.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node.getInt("id") == 0xCAFE);
        Assert.assertEquals(node.getString("type"), "STUB");

        // test that flow statistics results are correct
        JSONArray flowStats = flowStatistics.getJSONArray("flowStatistic");
        for (int i = 0; i < flowStats.length(); i++) {

            JSONObject flowStat = flowStats.getJSONObject(i);
            testFlowStat(flowStat, actionTypes[i], i);

        }

        // for /controller/nb/v2/statistics/default/port
        result = getJsonResult(baseURL + "port");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        JSONObject portStatistics = getJsonInstance(json, "portStatistics", 0xCAFE);
        JSONObject node2 = portStatistics.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node2.getInt("id") == 0xCAFE);
        Assert.assertEquals(node2.getString("type"), "STUB");

        // test that port statistic results are correct
        JSONArray portStatArray = portStatistics.getJSONArray("portStatistic");
        JSONObject portStat = portStatArray.getJSONObject(0);
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
        result = getJsonResult(baseURL + "flow/node/STUB/51966");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node = json.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node.getInt("id") == 0xCAFE);
        Assert.assertEquals(node.getString("type"), "STUB");

        // test that flow statistics results are correct
        flowStats = json.getJSONArray("flowStatistic");
        for (int i = 0; i < flowStats.length(); i++) {
            JSONObject flowStat = flowStats.getJSONObject(i);
            testFlowStat(flowStat, actionTypes[i], i);
        }

        result = getJsonResult(baseURL + "port/node/STUB/51966");
        jt = new JSONTokener(result);
        json = new JSONObject(jt);
        node2 = json.getJSONObject("node");
        // test that node was returned properly
        Assert.assertTrue(node2.getInt("id") == 0xCAFE);
        Assert.assertEquals(node2.getString("type"), "STUB");

        // test that port statistic results are correct
        portStatArray = json.getJSONArray("portStatistic");
        portStat = portStatArray.getJSONObject(0);

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

    private void testFlowStat(JSONObject flowStat, String actionType, int actIndex) throws JSONException {
        Assert.assertTrue(flowStat.getInt("tableId") == 1);
        Assert.assertTrue(flowStat.getInt("durationSeconds") == 40);
        Assert.assertTrue(flowStat.getInt("durationNanoseconds") == 400);
        Assert.assertTrue(flowStat.getInt("packetCount") == 200);
        Assert.assertTrue(flowStat.getInt("byteCount") == 100);

        // test that flow information is correct
        JSONObject flow = flowStat.getJSONObject("flow");
        Assert.assertTrue(flow.getInt("priority") == (3500 + actIndex));
        Assert.assertTrue(flow.getInt("idleTimeout") == 1000);
        Assert.assertTrue(flow.getInt("hardTimeout") == 2000);
        Assert.assertTrue(flow.getInt("id") == 12345);

        JSONArray matches = (flow.getJSONObject("match").getJSONArray("matchField"));
        Assert.assertEquals(matches.length(), 1);
        JSONObject match = matches.getJSONObject(0);
        Assert.assertTrue(match.getString("type").equals("NW_DST"));
        Assert.assertTrue(match.getString("value").equals("1.1.1.1"));

        JSONArray actionsArray = flow.getJSONArray("actions");
        Assert.assertEquals(actionsArray.length(), 1);
        JSONObject act = actionsArray.getJSONObject(0);
        Assert.assertTrue(act.getString("type").equals(actionType));

        if (act.getString("type").equals("OUTPUT")) {
            JSONObject port = act.getJSONObject("port");
            JSONObject port_node = port.getJSONObject("node");
            Assert.assertTrue(port.getInt("id") == 51966);
            Assert.assertTrue(port.getString("type").equals("STUB"));
            Assert.assertTrue(port_node.getInt("id") == 51966);
            Assert.assertTrue(port_node.getString("type").equals("STUB"));
        }

        if (act.getString("type").equals("SET_DL_SRC")) {
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

        if (act.getString("type").equals("SET_DL_DST")) {
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
        if (act.getString("type").equals("SET_DL_TYPE")) {
            Assert.assertTrue(act.getInt("dlType") == 10);
        }
        if (act.getString("type").equals("SET_VLAN_ID")) {
            Assert.assertTrue(act.getInt("vlanId") == 2);
        }
        if (act.getString("type").equals("SET_VLAN_PCP")) {
            Assert.assertTrue(act.getInt("pcp") == 3);
        }
        if (act.getString("type").equals("SET_VLAN_CFI")) {
            Assert.assertTrue(act.getInt("cfi") == 1);
        }

        if (act.getString("type").equals("SET_NW_SRC")) {
            Assert.assertTrue(act.getString("address").equals("2.2.2.2"));
        }
        if (act.getString("type").equals("SET_NW_DST")) {
            Assert.assertTrue(act.getString("address").equals("1.1.1.1"));
        }

        if (act.getString("type").equals("PUSH_VLAN")) {
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
        if (act.getString("type").equals("SET_NW_TOS")) {
            Assert.assertTrue(act.getInt("tos") == 16);
        }
        if (act.getString("type").equals("SET_TP_SRC")) {
            Assert.assertTrue(act.getInt("port") == 4201);
        }
        if (act.getString("type").equals("SET_TP_DST")) {
            Assert.assertTrue(act.getInt("port") == 8080);
        }
    }

    @Test
    public void testFlowProgrammer() throws JSONException {
        System.out.println("Starting FlowProgrammer JAXB client.");
        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/flowprogrammer/default/";
        // Attempt to get a flow that doesn't exit. Should return 404
        // status.
        String result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test1", "GET");
        Assert.assertTrue(result.equals("404"));

        // test add flow1
        String fc = "{\"name\":\"test1\", \"node\":{\"id\":\"51966\",\"type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test1", "PUT", fc);
        Assert.assertTrue(httpResponseCode == 201);

        // test get returns flow that was added.
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test1", "GET");
        // check that result came out fine.
        Assert.assertTrue(httpResponseCode == 200);
        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        Assert.assertEquals(json.getString("name"), "test1");
        JSONArray actionsArray = json.getJSONArray("actions");
        Assert.assertEquals(actionsArray.getString(0), "DROP");
        Assert.assertEquals(json.getString("installInHw"), "true");
        JSONObject node = json.getJSONObject("node");
        Assert.assertEquals(node.getString("type"), "STUB");
        Assert.assertEquals(node.getString("id"), "51966");
        // test adding same flow again succeeds with a change in any field ..return Success
        // code
        fc = "{\"name\":\"test1\", \"node\":{\"id\":\"51966\",\"type\":\"STUB\"}, \"actions\":[\"LOOPBACK\"]}";
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test1", "PUT", fc);
        Assert.assertTrue(result.equals("Success"));

        fc = "{\"name\":\"test2\", \"node\":{\"id\":\"51966\",\"type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test2", "PUT", fc);
        // test should return 409 for error due to same flow being added.
        Assert.assertTrue(result.equals("409"));

        // add second flow that's different
        fc = "{\"name\":\"test2\", \"nwSrc\":\"1.1.1.1\", \"node\":{\"id\":\"51966\",\"type\":\"STUB\"}, \"actions\":[\"DROP\"]}";
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test2", "PUT", fc);
        Assert.assertTrue(httpResponseCode == 201);

        // check that request returns both flows given node.
        result = getJsonResult(baseURL + "node/STUB/51966/", "GET");
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
        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test2", "DELETE");
        Assert.assertTrue(httpResponseCode == 204);

        result = getJsonResult(baseURL + "node/STUB/51966/staticFlow/test2", "GET");
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
                Integer nid = result.getJSONObject("node").getInt("id");
                if (nid.equals(nodeId)) {
                    break;
                }
            }
        } else {
            result = json.getJSONObject(array_name);
            Integer nid = result.getJSONObject("node").getInt("id");
            if (!nid.equals(nodeId)) {
                result = null;
            }
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
        String vlan_1 = "";

        // 2nd host
        String networkAddress_2 = "10.1.1.1";
        String dataLayerAddress_2 = "1A:2B:3C:4D:5E:6F";
        String nodeType_2 = "STUB";
        Integer nodeId_2 = 4477;
        String nodeConnectorType_2 = "STUB";
        Integer nodeConnectorId_2 = 34;
        String vlan_2 = "123";

        String baseURL = "http://127.0.0.1:8080/controller/nb/v2/hosttracker/default";

        // test PUT method: addHost()
        JSONObject fc_json = new JSONObject();
        fc_json.put("dataLayerAddress", dataLayerAddress_1);
        fc_json.put("nodeType", nodeType_1);
        fc_json.put("nodeId", nodeId_1);
        fc_json.put("nodeConnectorType", nodeType_1);
        fc_json.put("nodeConnectorId", nodeConnectorId_1.toString());
        fc_json.put("vlan", vlan_1);
        fc_json.put("staticHost", "true");
        fc_json.put("networkAddress", networkAddress_1);

        String result = getJsonResult(baseURL + "/address/" + networkAddress_1, "PUT", fc_json.toString());
        Assert.assertTrue(httpResponseCode == 201);

        fc_json = new JSONObject();
        fc_json.put("dataLayerAddress", dataLayerAddress_2);
        fc_json.put("nodeType", nodeType_2);
        fc_json.put("nodeId", nodeId_2);
        fc_json.put("nodeConnectorType", nodeType_2);
        fc_json.put("nodeConnectorId", nodeConnectorId_2.toString());
        fc_json.put("vlan", vlan_2);
        fc_json.put("staticHost", "true");
        fc_json.put("networkAddress", networkAddress_2);

        result = getJsonResult(baseURL + "/address/" + networkAddress_2 , "PUT", fc_json.toString());
        Assert.assertTrue(httpResponseCode == 201);

        // define variables for decoding returned strings
        String networkAddress;
        JSONObject host_jo;

        // the two hosts should be in inactive host DB
        // test GET method: getInactiveHosts()
        result = getJsonResult(baseURL + "/hosts/inactive", "GET");
        Assert.assertTrue(httpResponseCode == 200);

        JSONTokener jt = new JSONTokener(result);
        JSONObject json = new JSONObject(jt);
        // there should be at least two hosts in the DB
        Assert.assertTrue(json.get("hostConfig") instanceof JSONArray);
        JSONArray ja = json.getJSONArray("hostConfig");
        Integer count = ja.length();
        Assert.assertTrue(count == 2);

        for (int i = 0; i < count; i++) {
            host_jo = ja.getJSONObject(i);
            networkAddress = host_jo.getString("networkAddress");
            if (networkAddress.equalsIgnoreCase(networkAddress_1)) {
                Assert.assertTrue(host_jo.getString("dataLayerAddress").equalsIgnoreCase(dataLayerAddress_1));
                Assert.assertTrue(host_jo.getString("nodeConnectorType").equalsIgnoreCase(nodeConnectorType_1));
                Assert.assertTrue(host_jo.getInt("nodeConnectorId") == nodeConnectorId_1);
                Assert.assertTrue(host_jo.getString("nodeType").equalsIgnoreCase(nodeType_1));
                Assert.assertTrue(host_jo.getInt("nodeId") == nodeId_1);
                Assert.assertTrue(host_jo.getString("vlan").equals("0"));
                Assert.assertTrue(host_jo.getBoolean("staticHost"));
            } else if (networkAddress.equalsIgnoreCase(networkAddress_2)) {
                Assert.assertTrue(host_jo.getString("dataLayerAddress").equalsIgnoreCase(dataLayerAddress_2));
                Assert.assertTrue(host_jo.getString("nodeConnectorType").equalsIgnoreCase(nodeConnectorType_2));
                Assert.assertTrue(host_jo.getInt("nodeConnectorId") == nodeConnectorId_2);
                Assert.assertTrue(host_jo.getString("nodeType").equalsIgnoreCase(nodeType_2));
                Assert.assertTrue(host_jo.getInt("nodeId") == nodeId_2);
                Assert.assertTrue(host_jo.getString("vlan").equalsIgnoreCase(vlan_2));
                Assert.assertTrue(host_jo.getBoolean("staticHost"));
            } else {
                Assert.assertTrue(false);
            }
        }

        // test GET method: getActiveHosts() - no host expected
        result = getJsonResult(baseURL + "/hosts/active", "GET");
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

        result = getJsonResult(baseURL + "/hosts/active", "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertTrue(hostInJson(json, networkAddress_1));

        // test GET method for getHostDetails()

        result = getJsonResult(baseURL + "/address/" + networkAddress_1, "GET");
        Assert.assertTrue(httpResponseCode == 200);

        jt = new JSONTokener(result);
        json = new JSONObject(jt);

        Assert.assertFalse(json.length() == 0);

        Assert.assertTrue(json.getString("dataLayerAddress").equalsIgnoreCase(dataLayerAddress_1));
        Assert.assertTrue(json.getString("nodeConnectorType").equalsIgnoreCase(nodeConnectorType_1));
        Assert.assertTrue(json.getInt("nodeConnectorId") == nodeConnectorId_1);
        Assert.assertTrue(json.getString("nodeType").equalsIgnoreCase(nodeType_1));
        Assert.assertTrue(json.getInt("nodeId") == nodeId_1);
        Assert.assertTrue(json.getString("vlan").equals("0"));
        Assert.assertTrue(json.getBoolean("staticHost"));

        // test DELETE method for deleteFlow()

        result = getJsonResult(baseURL + "/address/" + networkAddress_1, "DELETE");
        Assert.assertTrue(httpResponseCode == 204);

        // verify host_1 removed from active host DB
        // test GET method: getActiveHosts() - no host expected

        result = getJsonResult(baseURL + "/hosts/active", "GET");
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
        if (json.get("hostConfig") instanceof JSONArray) {
            JSONArray ja = json.getJSONArray("hostConfig");
            for (int i = 0; i < ja.length(); i++) {
                String na = ja.getJSONObject(i).getString("networkAddress");
                if (na.equalsIgnoreCase(hostIp)) {
                    return true;
                }
            }
            return false;
        } else {
            JSONObject ja = json.getJSONObject("hostConfig");
            String na = ja.getString("networkAddress");
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

            if (headNC.getInt("id") == headNC1_nodeConnId) {
                Assert.assertEquals(headNode.getString("type"), nodeType);
                Assert.assertEquals(headNode.getLong("id"), headNC1_nodeId);
                Assert.assertEquals(headNC.getString("type"), nodeConnType);
                Assert.assertEquals(tailNode.getString("type"),nodeType);
                Assert.assertEquals(tailNode.getString("type"), nodeConnType);
                Assert.assertEquals(tailNC.getLong("id"), tailNC1_nodeConnId);
                Assert.assertEquals(bandw.getLong("value"), bw_1);
                Assert.assertTrue((short) stt.getInt("value") == state_1);
                Assert.assertEquals(ltc.getLong("value"), lat_1);
            } else if (headNC.getInt("id") == headNC2_nodeConnId) {
                Assert.assertEquals(headNode.getString("type"),nodeType);
                Assert.assertEquals(headNode.getLong("id"), headNC2_nodeId);
                Assert.assertEquals(headNC.getString("type"), nodeConnType);
                Assert.assertEquals(tailNode.getString("type"), nodeType);
                Assert.assertTrue(tailNode.getInt("id") == tailNC2_nodeId);
                Assert.assertEquals(tailNC.getString("type"), nodeConnType);
                Assert.assertEquals(tailNC.getLong("id"), tailNC2_nodeConnId);
                Assert.assertEquals(bandw.getLong("value"), bw_2);
                Assert.assertTrue((short) stt.getInt("value") == state_2);
                Assert.assertEquals(ltc.getLong("value"), lat_2);
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
                .put("name", "userLink_1")
                .put("srcNodeConnector",
                        nodeConnectorType_1 + "|" + nodeConnectorId_1 + "@" + nodeType_1 + "|" + nodeId_1)
                .put("dstNodeConnector",
                        nodeConnectorType_2 + "|" + nodeConnectorId_2 + "@" + nodeType_2 + "|" + nodeId_2);
        // execute HTTP request and verify response code
        result = getJsonResult(baseURL + "/userLink/userLink_1", "PUT", jo.toString());
        Assert.assertTrue(httpResponseCode == 201);

        // === test GET method for getUserLinks()
        result = getJsonResult(baseURL + "/userLinks", "GET");
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
                if (userlink.getString("name").equalsIgnoreCase("userLink_1")) {
                    break;
                }
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
        Assert.assertTrue(httpResponseCode == 204);

        // execute another getUserLinks() request to verify that userLink_1 is
        // removed
        result = getJsonResult(baseURL + "/userLinks", "GET");
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
                mavenBundle("org.slf4j", "jcl-over-slf4j").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),

                // the plugin stub to get data for the tests
                mavenBundle("org.opendaylight.controller", "protocol_plugins.stub").versionAsInProject(),

                // List all the opendaylight modules
                mavenBundle("org.opendaylight.controller", "configuration").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "configuration.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager.it.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.services-implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "security").versionAsInProject().noStart(),
                mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.connection").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.connection.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "connectionmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "connectionmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "forwardingrulesmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller",
                            "forwardingrulesmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statisticsmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statisticsmanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "arphandler").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "arphandler").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "routing.dijkstra_implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "topologymanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "usermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "usermanager.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "logging.bridge").versionAsInProject(),
//                mavenBundle("org.opendaylight.controller", "clustering.test").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "forwarding.staticrouting").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "bundlescanner").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "bundlescanner.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "commons.httpclient").versionAsInProject(),

                // Northbound bundles
                mavenBundle("org.opendaylight.controller", "commons.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "forwarding.staticrouting.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "statistics.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "topology.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "hosttracker.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "flowprogrammer.northbound").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "subnets.northbound").versionAsInProject(),

                mavenBundle("org.codehaus.jackson", "jackson-mapper-asl").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-core-asl").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-jaxrs").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-xc").versionAsInProject(),
                mavenBundle("org.codehaus.jettison", "jettison").versionAsInProject(),

                mavenBundle("commons-io", "commons-io").versionAsInProject(),

                mavenBundle("commons-fileupload", "commons-fileupload").versionAsInProject(),

                mavenBundle("equinoxSDK381", "javax.servlet").versionAsInProject(),
                mavenBundle("equinoxSDK381", "javax.servlet.jsp").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("orbit", "javax.xml.rpc").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.cm").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.launcher").versionAsInProject(),

                mavenBundle("geminiweb", "org.eclipse.gemini.web.core").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.extender").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.tomcat").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.kernel.equinox.extensions").versionAsInProject().noStart(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.common").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.io").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.math").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi.manifest").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.parser.manifest").versionAsInProject(),

                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell").versionAsInProject(),

                mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
                mavenBundle("virgomirror", "org.eclipse.jdt.core.compiler.batch").versionAsInProject(),
                mavenBundle("eclipselink", "javax.persistence").versionAsInProject(),
                mavenBundle("eclipselink", "javax.resource").versionAsInProject(),

                mavenBundle("orbit", "javax.activation").versionAsInProject(),
                mavenBundle("orbit", "javax.annotation").versionAsInProject(),
                mavenBundle("orbit", "javax.ejb").versionAsInProject(),
                mavenBundle("orbit", "javax.el").versionAsInProject(),
                mavenBundle("orbit", "javax.mail.glassfish").versionAsInProject(),
                mavenBundle("orbit", "javax.xml.rpc").versionAsInProject(),
                mavenBundle("orbit", "org.apache.catalina").versionAsInProject(),
                // these are bundle fragments that can't be started on its own
                mavenBundle("orbit", "org.apache.catalina.ha").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.catalina.tribes").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.coyote").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.jasper").versionAsInProject().noStart(),

                mavenBundle("orbit", "org.apache.el").versionAsInProject(),
                mavenBundle("orbit", "org.apache.juli.extras").versionAsInProject(),
                mavenBundle("orbit", "org.apache.tomcat.api").versionAsInProject(),
                mavenBundle("orbit", "org.apache.tomcat.util").versionAsInProject().noStart(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl").versionAsInProject(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl.impl").versionAsInProject(),

                mavenBundle("org.ops4j.pax.exam", "pax-exam-container-native").versionAsInProject(),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-junit4").versionAsInProject(),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-link-mvn").versionAsInProject(),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether").versionAsInProject(),

                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),

                mavenBundle("org.springframework", "org.springframework.asm").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.aop").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.context").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.context.support").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.core").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.beans").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.expression").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.web").versionAsInProject(),

                mavenBundle("org.aopalliance", "com.springsource.org.aopalliance").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.web.servlet").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-config").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-core").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-web").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-taglibs").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.transaction").versionAsInProject(),

                mavenBundle("org.ow2.chameleon.management", "chameleon-mbeans").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "net.sf.jung2").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "com.sun.jersey.jersey-servlet")
                .versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "org.apache.catalina.filters.CorsFilter")
                .versionAsInProject().noStart(),

                // Jersey needs to be started before the northbound application
                // bundles, using a lower start level
                mavenBundle("com.sun.jersey", "jersey-client").versionAsInProject(),
                mavenBundle("com.sun.jersey", "jersey-server").versionAsInProject().startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-core").versionAsInProject().startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-json").versionAsInProject().startLevel(2), junitBundles());
    }

}
