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
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

/**
 * @See {@link InvokeRpcMethodTest}
 *
 */
public class RestconfImplTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;

    @BeforeClass
    public static void init() throws FileNotFoundException, URISyntaxException, ReactorException {
        final Set<Module> allModules = TestUtils.loadModulesFromDirPath("/full-versions/yangs");
        assertNotNull(allModules);
        final SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy(ControllerContext.getInstance());
        controllerContext.setSchemas(schemaContext);

    }

    @Before
    public void initMethod() {
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setControllerContext(controllerContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExample() throws FileNotFoundException, ParseException {
        @SuppressWarnings("rawtypes")
        final
        NormalizedNode normalizedNodeData = TestUtils.prepareNormalizedNodeWithIetfInterfacesInterfacesData();
        final BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.readOperationalData(any(YangInstanceIdentifier.class))).thenReturn(normalizedNodeData);
        assertEquals(normalizedNodeData,
                brokerFacade.readOperationalData(null));
    }

}
