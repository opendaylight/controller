/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.input.to.normalized;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.RestconfApplication;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class LoadDataToNormalizedNodeTest extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static NormalizedNode<?, ?> transformedValue;

    @BeforeClass
    public static void initialize() throws FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/json-to-cnsn/simple-container-yang");
        ControllerContext.getInstance().setSchemas(schemaContext);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(ControllerContext.getInstance());
    }

    @Test
    public void loadJsonToNormalizedNodeTest() throws IOException {
        String jsonData = TestUtils.loadTextFile(TestUtils.class.getResource("/json-to-cnsn/simple-container.json")
                .getPath());
        String uri = "/simple-container-yang:cont";
        target(uri).request("application/json").post(Entity.entity(jsonData, "application/json"));
        assertEquals("Json input wasn't read correctly", awaitedData(false), transformedValue);
    }

    /**
     * load incorrect data (wrong json format)
     */
    @Test
    public void loadIncorrectJsonToNormalizedNodeTest() throws IOException {
        String jsonData = TestUtils.loadTextFile(TestUtils.class.getResource(
                "/json-to-cnsn/unsupported-json-format.json").getPath());
        String uri = "/simple-container-yang:cont";
        Response response = target(uri).request("application/json").post(Entity.entity(jsonData, "application/json"));
        assertEquals("400 HTTP code (malformed message) was awaited.", 400, response.getStatus());
    }

    /**
     * After fixing bug 1975 commented input in xml file should be uncommented
     *
     * @throws IOException
     */
    @Test
    public void loadXmlToNormalizedNodeTest() throws IOException {
        String xmlData = TestUtils.loadTextFile(TestUtils.class.getResource("/xml-to-cnsn/simple-container.xml")
                .getPath());
        String uri = "/simple-container-yang:cont";
        target(uri).request("application/xml").post(Entity.entity(xmlData, "application/xml"));
        assertEquals("Xml input wasn't read correctly", awaitedData(true), transformedValue);
    }

    @Test
    public void loadIncorrectXmlToNormalizedNodeTest() throws IOException {
        String xmlData = TestUtils.loadTextFile(TestUtils.class.getResource("/xml-to-cnsn/wrong-data.xml").getPath());
        String uri = "/simple-container-yang:cont";
        Response response = target(uri).request("application/xml").post(Entity.entity(xmlData, "application/xml"));
        assertEquals("400 HTTP code (malformed message) was awaited.", 400, response.getStatus());
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(new DummyRestConfService(),
                new JsonNormalizedNodeBodyReader(), new XmlNormalizedNodeBodyReader());
        resourceConfig.registerClasses(new RestconfApplication().getClasses());
        return resourceConfig;
    }

    /**
     *
     * @param isForXml
     *            - in the future should be removed. During testing it was found that if in yang is specified unkeyed
     *            list it is incorrectly translated to ImmutableMapNode instead of ImmutableUnkeyedListNode See bug 1975
     * @return
     */
    private NormalizedNode<?, ?> awaitedData(final boolean isForXml) {
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contBuilder = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create("simple:container:yang", "2013-11-12", "cont")));
        contBuilder.withChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create("simple:container:yang", "2013-11-12", "cont1")))
                .build());
        if (!isForXml) {
            contBuilder
                    .withChild(Builders
                            .unkeyedListBuilder()
                            .withNodeIdentifier(
                                    new NodeIdentifier(QName.create("simple:container:yang", "2013-11-12", "lst1")))
                            .withChild(
                                    Builders.unkeyedListEntryBuilder()
                                            .withNodeIdentifier(
                                                    new NodeIdentifier(QName.create("simple:container:yang",
                                                            "2013-11-12", "lst1"))).build()).build());
        }
        contBuilder.withChild(Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create("simple:container:yang", "2013-11-12", "lflst1")))
                .withChildValue("lflst1_1").withChildValue("lflst1_2").build());
        contBuilder.withChild(Builders.leafBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create("simple:container:yang", "2013-11-12", "lf1")))
                .withValue("lf1").build());
        return contBuilder.build();
    }

    @Path("/")
    public class DummyRestConfService {

        @POST
        @Path("/{identifier:.+}")
        @Consumes({ "application/json", "application/xml" })
        public void postData(final NormalizedNodeContext payload) {
            transformedValue = payload.getData();
        }

    }

}
