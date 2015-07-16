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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.impl.RestBrokerFacadeImpl;
import org.opendaylight.controller.rest.connector.impl.RestSchemaContextImpl;
import org.opendaylight.controller.rest.services.RestconfServiceData;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @See {@link InvokeRpcMethodTest}
 *
 */
public class RestconfImplTest {

    private RestconfServiceData restDataServ = null;
    private static RestSchemaContextImpl controllerContext = null;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        final Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        final SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy(new RestSchemaContextImpl());
        controllerContext.setSchemas(schemaContext);

    }

    @Before
    public void initMethod() {
        final RestBrokerFacade broker = Mockito.mock(RestBrokerFacade.class);
        restDataServ = new RestconfServiceDataImpl(broker, controllerContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExample() throws FileNotFoundException, ParseException {
        @SuppressWarnings("rawtypes")
        final
        NormalizedNode normalizedNodeData = TestUtils.prepareNormalizedNodeWithIetfInterfacesInterfacesData();
        final RestBrokerFacadeImpl brokerFacade = mock(RestBrokerFacadeImpl.class);
        when(brokerFacade.readOperationalData(any(YangInstanceIdentifier.class))).thenReturn(normalizedNodeData);
        assertEquals(normalizedNodeData,
                brokerFacade.readOperationalData(null));
    }

}
