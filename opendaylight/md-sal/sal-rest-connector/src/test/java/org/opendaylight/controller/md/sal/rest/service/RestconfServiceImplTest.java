/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.service;

import static org.junit.Assert.fail;
import com.google.common.base.Optional;
import javax.ws.rs.core.UriInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.service
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Apr 21, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RestconfServiceImplTest {

    private RestConnectorProviderImpl connectorProvider;

    private RestconfService restServices;

    @Mock
    private ProviderSession session;
    @Mock
    private DOMMountPointService mPointService;
    @Mock
    private SchemaService schemaService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private DOMDataBroker dataBroker;
    @Mock
    private UriInfo uriInfo;


    @Before
    public void initialization() {
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules", null);
        Mockito.when(schemaService.getGlobalContext()).thenReturn(schemaCx);
        Mockito.when(session.getService(DOMMountPointService.class)).thenReturn(mPointService);
        Mockito.when(session.getService(SchemaService.class)).thenReturn(schemaService);
        Mockito.when(session.getService(DOMRpcService.class)).thenReturn(rpcService);
        Mockito.when(session.getService(DOMDataBroker.class)).thenReturn(dataBroker);
        connectorProvider = RestConnectorProviderImpl.getInstance();
        connectorProvider.setWebsocketPort(new PortNumber(8181));
        connectorProvider.onSessionInitiated(session);
        restServices = new RestconfServiceImpl();
    }

    @After
    public void closing() throws Exception {
        connectorProvider.close();
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModules(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModulesUriInfo() {
        final String expectedLocalName = "modules";
        final NormalizedNodeContext nnCx = restServices.getModules(uriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModules(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModulesStringUriInfo(){
        final DOMMountPoint mPoint = Mockito.mock(DOMMountPoint.class);
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules/modules-behind-mount-point/module1", null);
        Mockito.when(mPoint.getSchemaContext()).thenReturn(schemaCx);
        final Optional<DOMMountPoint> optMountPoint = Optional.<DOMMountPoint> of(mPoint);
        Mockito.when(mPointService.getMountPoint(Matchers.any(YangInstanceIdentifier.class))).thenReturn(optMountPoint);
        final String expectedLocalName = "modules";
        final String uri = "nested-module:depth1-cont/depth2-cont1/yang-ext:mount/module1-behind-mount-point/2014-02-03";
        final NormalizedNodeContext nnCx = restServices.getModules(uri, uriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModule(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModule(){
        final String expectedLocalName = "modules";
        final String uri = "nested-module/2014-06-3";
        final NormalizedNodeContext nnCx = restServices.getModule(uri, uriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModule(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModuleBehindMountPoint(){
        final DOMMountPoint mPoint = Mockito.mock(DOMMountPoint.class);
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules/modules-behind-mount-point", null);
        Mockito.when(mPoint.getSchemaContext()).thenReturn(schemaCx);
        final Optional<DOMMountPoint> optMountPoint = Optional.<DOMMountPoint> of(mPoint);
        Mockito.when(mPointService.getMountPoint(Matchers.any(YangInstanceIdentifier.class))).thenReturn(optMountPoint);
        final String expectedLocalName = "modules";
        final String uri = "nested-module:depth1-cont/depth2-cont1/yang-ext:mount/module1-behind-mount-point/2014-02-03";
        final NormalizedNodeContext nnCx = restServices.getModule(uri, uriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#getOperations(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testGetOperationsUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#getOperations(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testGetOperationsStringUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#invokeRpc(java.lang.String, org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testInvokeRpcStringNormalizedNodeContextUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#invokeRpc(java.lang.String, java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testInvokeRpcStringStringUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#readConfigurationData(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testReadConfigurationData(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#readOperationalData(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testReadOperationalData(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#updateConfigurationData(java.lang.String, org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext)}.
     */
    @Test
    @Ignore
    public void testUpdateConfigurationData(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#createConfigurationData(java.lang.String, org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testCreateConfigurationDataStringNormalizedNodeContextUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#createConfigurationData(org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testCreateConfigurationDataNormalizedNodeContextUriInfo(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#deleteConfigurationData(java.lang.String)}.
     */
    @Test
    @Ignore
    public void testDeleteConfigurationData(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.opendaylight.controller.md.sal.rest.service.RestconfServiceImpl#subscribeToStream(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testSubscribeToStream(){
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link RestconfServiceImpl#getAvailableStreams(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetAvailableStreams(){
        final String expectedLocalName = "streams";
        final NormalizedNodeContext nnCx = restServices.getAvailableStreams(uriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    private static void baseValidationNormalizedNodeContext(final NormalizedNodeContext nnCx, final String expectedLocalName) {
        Assert.assertNotNull(nnCx);
        Assert.assertNotNull(nnCx.getData());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext().getSchemaNode());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext().getSchemaContext());
        Assert.assertEquals(expectedLocalName, nnCx.getData().getNodeType().getLocalName());
        Assert.assertTrue(nnCx.getInstanceIdentifierContext().getSchemaNode().getQName().toString().contains(expectedLocalName));
    }
}
