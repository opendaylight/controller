/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.rest.connector.impl.RestBrokerFacadeImpl;
import org.opendaylight.controller.rest.connector.impl.RestSchemaContextImpl;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.errors.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.rest.providers.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.providers.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.rest.providers.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.rest.providers.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.services.RestconfServiceData;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestDeleteOperationTest extends JerseyTest {

    private static RestSchemaContextImpl controllerContext;
    private static RestBrokerFacadeImpl brokerFacade;
    private static RestconfServiceData restconfImpl;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        final Set<Module> allModules = TestUtils.loadModulesFrom("/test-config-data/yang1");
        assertNotNull(allModules);
        final SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = new RestSchemaContextImpl();
        controllerContext.setSchemas(schemaContext);
        brokerFacade = mock(RestBrokerFacadeImpl.class);
        restconfImpl = new RestconfServiceDataImpl(brokerFacade, controllerContext);
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, new NormalizedNodeJsonBodyWriter(),
                new NormalizedNodeXmlBodyWriter(), new XmlNormalizedNodeBodyReader(controllerContext),
                new JsonNormalizedNodeBodyReader(controllerContext));
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteConfigStatusCodes() throws UnsupportedEncodingException {
        final String uri = "/config/test-interface:interfaces";
        when(brokerFacade.commitConfigurationDataDelete(any(YangInstanceIdentifier.class))).thenReturn(
                mock(CheckedFuture.class));
        Response response = target(uri).request(MediaType.APPLICATION_XML).delete();
        assertEquals(200, response.getStatus());

        doThrow(RestconfDocumentedException.class).when(brokerFacade).commitConfigurationDataDelete(
                any(YangInstanceIdentifier.class));
        response = target(uri).request(MediaType.APPLICATION_XML).delete();
        assertEquals(500, response.getStatus());
    }
}
