/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.websockets.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.websockets.client.IClientMessageCallback;
import org.opendaylight.controller.sal.restconf.impl.websockets.client.WebSocketClient;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestStream extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static CompositeNode answerFromGet;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
//        answerFromGet = prepareCompositeNodeWithIetfInterfacesInterfacesData();
    }
    
    private static CompositeNode prepareCompositeNodeWithIetfInterfacesInterfacesData() {
        CompositeNode intface;
        intface = new CompositeNodeWrapper(URI.create("interface"), "interface");
        List<Node<?>> childs = new ArrayList<>();

        childs.add(new SimpleNodeWrapper(URI.create("name"), "name", "eth0"));
        childs.add(new SimpleNodeWrapper(URI.create("type"), "type", "ethernetCsmacd"));
        childs.add(new SimpleNodeWrapper(URI.create("enabled"), "enabled", Boolean.FALSE));
        childs.add(new SimpleNodeWrapper(URI.create("description"), "description", "some interface"));
        intface.setValue(childs);
        return intface;
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
//         enable(TestProperties.LOG_TRAFFIC);
//         enable(TestProperties.DUMP_ENTITY);
//         enable(TestProperties.RECORD_LOG_LEVEL);
//         set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void testCallRpcCallGet() throws UnsupportedEncodingException, InterruptedException {
        String uri = createUri("/operations/", "sal-remote:create-data-change-event-subscription");
        Response responseWithStreamName = post(uri, MediaType.APPLICATION_XML, getRpcInput());
        String xmlResponse = responseWithStreamName.readEntity(String.class);
        assertNotNull(xmlResponse);
        assertTrue(xmlResponse.contains("<stream-name>ietf-interfaces:interfaces/ietf-interfaces:interface/eth0</stream-name>"));
        
        uri = createUri("/streams/stream/", "ietf-interfaces:interfaces/ietf-interfaces:interface/eth0");
        Response responseWithRedirectionUri = get(uri, MediaType.APPLICATION_XML);
        final URI websocketServerUri = responseWithRedirectionUri.getLocation();
        assertNotNull(websocketServerUri);
        assertEquals(websocketServerUri.toString(), "http://localhost:8181/ietf-interfaces:interfaces/ietf-interfaces:interface/eth0");
        
//        WebSocketServer webSocketServer = new WebSocketServer();
//        
//        Thread serverThread = new Thread(webSocketServer);
//        serverThread.setName("Web socket server");
//        serverThread.start();
//        
//        Thread clientThread = new Thread(new Runnable() {
//            
//            @Override
//            public void run() {
//                try {
//                    WebSocketClient webSocketClient = new WebSocketClient(websocketServerUri, new ClientMessageCallback());
//                    webSocketClient.connect();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        clientThread.setName("Web socket client");
//        clientThread.start();
        
    }
    
    public void testWebsocketServer() {
        
    }

    private Response post(String uri, String mediaType, String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType));
    }
    
    private Response get(String uri, String mediaType) {
        return target(uri).request(mediaType).get();
    }

    private void mockReadConfigurationDataMethod() {
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(answerFromGet);
    }
    
    private String getRpcInput() {
        StringBuilder sb = new StringBuilder();
        sb.append("<input xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\">");
        sb.append("<path xmlns:int=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\">/int:interfaces/int:interface[int:name='eth0']</path>");
        sb.append("</input>");
        return sb.toString();
    }
    
    private static class ClientMessageCallback implements IClientMessageCallback {
        @Override
        public void onMessageReceived(Object message) {
            System.out.println("received message : " + ((TextWebSocketFrame)message).text());
        }
    }

}
