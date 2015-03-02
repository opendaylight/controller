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
import org.junit.Ignore;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class RestStream extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        final ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    @Ignore // FIXME : find problem with codec
    public void testCallRpcCallGet() throws UnsupportedEncodingException, InterruptedException {
        String uri = "/operations/sal-remote:create-data-change-event-subscription";
        final Response responseWithStreamName = post(uri, MediaType.APPLICATION_XML, getRpcInput());
        final Document xmlResponse = responseWithStreamName.readEntity(Document.class);
        assertNotNull(xmlResponse);
        final Element outputElement = xmlResponse.getDocumentElement();
        assertEquals("output",outputElement.getLocalName());

        final Node streamNameElement = outputElement.getFirstChild();
        assertEquals("stream-name",streamNameElement.getLocalName());
        assertEquals("ietf-interfaces:interfaces/ietf-interfaces:interface/eth0/datastore=CONFIGURATION/scope=BASE",streamNameElement.getTextContent());

        uri = "/streams/stream/ietf-interfaces:interfaces/ietf-interfaces:interface/eth0/datastore=CONFIGURATION/scope=BASE";
        final Response responseWithRedirectionUri = get(uri, MediaType.APPLICATION_XML);
        final URI websocketServerUri = responseWithRedirectionUri.getLocation();
        assertNotNull(websocketServerUri);
        assertTrue(websocketServerUri.toString().matches(".*http://localhost:[\\d]+/ietf-interfaces:interfaces/ietf-interfaces:interface/eth0.*"));
    }

    private Response post(final String uri, final String mediaType, final String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType));
    }

    private Response get(final String uri, final String mediaType) {
        return target(uri).request(mediaType).get();
    }

    private String getRpcInput() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<input xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\">");
        sb.append("<path xmlns:int=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\">/int:interfaces/int:interface[int:name='eth0']</path>");
        sb.append("</input>");
        return sb.toString();
    }

}
