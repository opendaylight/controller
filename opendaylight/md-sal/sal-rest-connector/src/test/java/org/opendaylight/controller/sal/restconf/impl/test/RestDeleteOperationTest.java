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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestDeleteOperationTest extends JerseyTest {

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/test-config-data/yang1");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
//        enable(TestProperties.LOG_TRAFFIC);
//        enable(TestProperties.DUMP_ENTITY);
//        enable(TestProperties.RECORD_LOG_LEVEL);
//        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                XmlToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void deleteConfigStatusCodes() throws UnsupportedEncodingException {
        String uri = "/config/test-interface:interfaces";
        Future<RpcResult<TransactionStatus>> dummyFuture = createFuture(TransactionStatus.COMMITED);
        when(brokerFacade.commitConfigurationDataDelete(any(InstanceIdentifier.class))).thenReturn(dummyFuture);
        Response response = target(uri).request(MediaType.APPLICATION_XML).delete();
        assertEquals(200, response.getStatus());
        
        dummyFuture = createFuture(TransactionStatus.FAILED);
        when(brokerFacade.commitConfigurationDataDelete(any(InstanceIdentifier.class))).thenReturn(dummyFuture);
        response = target(uri).request(MediaType.APPLICATION_XML).delete();
        assertEquals(500, response.getStatus());
    }
    
    private Future<RpcResult<TransactionStatus>> createFuture(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName).build();
        return DummyFuture.builder().rpcResult(rpcResult).build();
    }

}
