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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * @See {@link InvokeRpcMethodTest}
 *
 */
public class RestconfImplTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        final Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
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

    @Test
    public void testRpcForMountpoint() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        doReturn(new MultivaluedHashMap<>()).when(uriInfo).getQueryParameters(anyBoolean());

        final NormalizedNodeContext ctx = mock(NormalizedNodeContext.class);
        final InstanceIdentifierContext iiCtx = mock(InstanceIdentifierContext.class);
        doReturn(iiCtx).when(ctx).getInstanceIdentifierContext();
        final SchemaNode schemaNode = mock(RpcDefinition.class);
        doReturn(schemaNode).when(iiCtx).getSchemaNode();
        doReturn(mock(SchemaPath.class)).when(schemaNode).getPath();
        doReturn(QName.create("namespace", "2010-10-10", "localname")).when(schemaNode).getQName();

        final DOMMountPoint mount = mock(DOMMountPoint.class);
        doReturn(mount).when(iiCtx).getMountPoint();
        final DOMRpcService rpcService = mock(DOMRpcService.class);
        doReturn(Optional.of(rpcService)).when(mount).getService(DOMRpcService.class);
        doReturn(Futures.immediateCheckedFuture(mock(DOMRpcResult.class))).when(rpcService).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));
        restconfImpl.invokeRpc("randomId", ctx, uriInfo);
        restconfImpl.invokeRpc("ietf-netconf", ctx, uriInfo);
        verify(rpcService, times(2)).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));
    }
}
