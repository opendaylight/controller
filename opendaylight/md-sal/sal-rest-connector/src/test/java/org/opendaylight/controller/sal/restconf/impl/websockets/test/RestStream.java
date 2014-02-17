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
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

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
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestStream extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
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
        String uri = "/operations/sal-remote:create-data-change-event-subscription";
        Response responseWithStreamName = post(uri, MediaType.APPLICATION_XML, getRpcInput());
        String xmlResponse = responseWithStreamName.readEntity(String.class);
        assertNotNull(xmlResponse);
        assertTrue(xmlResponse.contains("<stream-name>ietf-interfaces:interfaces/ietf-interfaces:interface/eth0</stream-name>"));

        uri = "/streams/stream/ietf-interfaces:interfaces/ietf-interfaces:interface/eth0";
        Response responseWithRedirectionUri = get(uri, MediaType.APPLICATION_XML);
        final URI websocketServerUri = responseWithRedirectionUri.getLocation();
        assertNotNull(websocketServerUri);
        assertEquals(websocketServerUri.toString(), "http://localhost:8181/ietf-interfaces:interfaces/ietf-interfaces:interface/eth0");
    }

    private Response post(String uri, String mediaType, String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType));
    }

    private Response get(String uri, String mediaType) {
        return target(uri).request(mediaType).get();
    }

    private String getRpcInput() {
        StringBuilder sb = new StringBuilder();
        sb.append("<input xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\">");
        sb.append("<path xmlns:int=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\">/int:interfaces/int:interface[int:name='eth0']</path>");
        sb.append("</input>");
        return sb.toString();
    }

}
