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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.rest.AbstractRestConnectorTest;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.service
 *
 * Base test class method for quick testing of {@link RestconfService} implementation
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Apr 21, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RestconfServiceImplTest extends AbstractRestConnectorTest {

    private RestconfService restServices;

    @Mock
    private UriInfo mockUriInfo;

    @Before
    @Override
    public void initialization() {
        super.initialization();
        restServices = new RestconfServiceImpl();
    }

    @After
    @Override
    public void closing() throws Exception {
        super.closing();
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModules(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModulesUriInfo() {
        final String expectedLocalName = "modules";
        final NormalizedNodeContext nnCx = restServices.getModules(mockUriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModules(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModulesStringUriInfo() {
        final DOMMountPoint mPoint = Mockito.mock(DOMMountPoint.class);
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules/modules-behind-mount-point/module1", null);
        Mockito.when(mPoint.getSchemaContext()).thenReturn(schemaCx);
        final Optional<DOMMountPoint> optMountPoint = Optional.<DOMMountPoint> of(mPoint);
        Mockito.when(mockMountPointService.getMountPoint(Matchers.any(YangInstanceIdentifier.class))).thenReturn(optMountPoint);
        final String expectedLocalName = "modules";
        final String uri = "nested-module:depth1-cont/depth2-cont1/yang-ext:mount/module1-behind-mount-point/2014-02-03";
        final NormalizedNodeContext nnCx = restServices.getModules(uri, mockUriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModule(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModule() {
        final String expectedLocalName = "modules";
        final String uri = "nested-module/2014-06-3";
        final NormalizedNodeContext nnCx = restServices.getModule(uri, mockUriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getModule(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetModuleBehindMountPoint() {
        final DOMMountPoint mPoint = Mockito.mock(DOMMountPoint.class);
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules/modules-behind-mount-point", null);
        Mockito.when(mPoint.getSchemaContext()).thenReturn(schemaCx);
        final Optional<DOMMountPoint> optMountPoint = Optional.<DOMMountPoint> of(mPoint);
        Mockito.when(mockMountPointService.getMountPoint(Matchers.any(YangInstanceIdentifier.class))).thenReturn(optMountPoint);
        final String expectedLocalName = "modules";
        final String uri = "nested-module:depth1-cont/depth2-cont1/yang-ext:mount/module1-behind-mount-point/2014-02-03";
        final NormalizedNodeContext nnCx = restServices.getModule(uri, mockUriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }

    /**
     * Test method for {@link RestconfServiceImpl#getOperations(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testGetOperationsUriInfo() {
        // FIXME : fix method and add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link RestconfServiceImpl#getOperations(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testGetOperationsStringUriInfo() {
        // FIXME : fix method and add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link RestconfServiceImpl#invokeRpc(java.lang.String, NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testInvokeRpcStringNormalizedNodeContextUriInfo() {
        final String uri = "module1:dummy-rpc1-module1";
        final DOMRpcResult rpcResult = Mockito.mock(DOMRpcResult.class);
        Mockito.when(rpcResult.getResult()).thenReturn(null);
        final CheckedFuture<DOMRpcResult, DOMRpcException> checkedFuture = Futures.immediateCheckedFuture(rpcResult);
        Mockito.when(mockRpcService.invokeRpc(Matchers.any(SchemaPath.class),
                Matchers.any(NormalizedNode.class))).thenReturn(checkedFuture);
        final InstanceIdentifierContext<?> yiiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final NormalizedNodeContext nnCx = new NormalizedNodeContext(yiiCx, Mockito.mock(NormalizedNode.class));
        final NormalizedNodeContext result = restServices.invokeRpc(uri, nnCx, mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getData());
    }

    /**
     * Test method for {@link RestconfServiceImpl#invokeRpc(java.lang.String, java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testInvokeRpcStringStringUriInfo() {
        final String uri = "module1:dummy-rpc1-module1";
        final DOMRpcResult rpcResult = Mockito.mock(DOMRpcResult.class);
        Mockito.when(rpcResult.getResult()).thenReturn(null);
        final CheckedFuture<DOMRpcResult, DOMRpcException> checkedFuture = Futures.immediateCheckedFuture(rpcResult);
        Mockito.when(mockRpcService.invokeRpc(Matchers.any(SchemaPath.class),
                Matchers.any(NormalizedNode.class))).thenReturn(checkedFuture);
        final NormalizedNodeContext result = restServices.invokeRpc(uri, "", mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getData());
    }

    /**
     * Test method for {@link RestconfServiceImpl#readConfigurationData(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testReadConfigurationData() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final QName qName = QName.create("urn:nested:module", "2014-06-03", "depth4-cont1");
        Mockito.when(nn.getNodeType()).thenReturn(qName);
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final YangInstanceIdentifier yii = iiCx.getInstanceIdentifier();
        mockReadDefaultDataBroker(nn, yii, LogicalDatastoreType.CONFIGURATION);
        final NormalizedNodeContext result = restServices.readConfigurationData(uri, mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getData());
        Assert.assertEquals(nn, result.getData());
        Assert.assertNotNull(result.getInstanceIdentifierContext());
        Assert.assertNotNull(result.getInstanceIdentifierContext().getSchemaNode());
        Assert.assertEquals(iiCx.getSchemaNode(), result.getInstanceIdentifierContext().getSchemaNode());
    }

    /**
     * Test method for {@link RestconfServiceImpl#readOperationalData(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testReadOperationalData() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final QName qName = QName.create("urn:nested:module", "2014-06-03", "depth4-cont1");
        Mockito.when(nn.getNodeType()).thenReturn(qName);
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final YangInstanceIdentifier yii = iiCx.getInstanceIdentifier();
        mockReadDefaultDataBroker(nn, yii, LogicalDatastoreType.OPERATIONAL);
        final NormalizedNodeContext result = restServices.readOperationalData(uri, mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getData());
        Assert.assertEquals(nn, result.getData());
        Assert.assertNotNull(result.getInstanceIdentifierContext());
        Assert.assertNotNull(result.getInstanceIdentifierContext().getSchemaNode());
        Assert.assertEquals(iiCx.getSchemaNode(), result.getInstanceIdentifierContext().getSchemaNode());
    }

    /**
     * Test method for {@link RestconfServiceImpl#updateConfigurationData(java.lang.String, NormalizedNodeContext)}.
     */
    @Test
    public void testUpdateConfigurationData() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final QName qName = QName.create("urn:nested:module", "2014-06-03", "depth4-cont1");
        Mockito.when(nn.getNodeType()).thenReturn(qName);
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final YangInstanceIdentifier yii = iiCx.getInstanceIdentifier();
        final NormalizedNodeContext nnCx = new NormalizedNodeContext(iiCx, nn);
        mockWriteDefaultDataBroker(yii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final Response result = restServices.updateConfigurationData(uri, nnCx);
        Assert.assertNotNull(result);
        Assert.assertEquals(Status.OK, result.getStatusInfo());
        Mockito.verify(wTx, Mockito.timeout(3)).exists(LogicalDatastoreType.CONFIGURATION, yii);
        Mockito.verify(wTx, Mockito.times(1)).merge(LogicalDatastoreType.CONFIGURATION, yii, nn);
    }

    /**
     * Test method for {@link RestconfServiceImpl#createConfigurationData(java.lang.String, NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testCreateConfigurationDataStringNormalizedNodeContextUriInfo() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final QName qName = QName.create("urn:nested:module", "2014-06-03", "depth4-cont1");
        Mockito.when(nn.getNodeType()).thenReturn(qName);
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final YangInstanceIdentifier yii = iiCx.getInstanceIdentifier();
        final NormalizedNodeContext nnCx = new NormalizedNodeContext(iiCx, nn);
        mockWriteDefaultDataBroker(yii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final Response result = restServices.createConfigurationData(uri, nnCx, mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertEquals(Status.NO_CONTENT, result.getStatusInfo());
        Mockito.verify(wTx, Mockito.timeout(1)).read(LogicalDatastoreType.CONFIGURATION, yii);
        Mockito.verify(wTx, Mockito.timeout(3)).exists(LogicalDatastoreType.CONFIGURATION, yii);
        Mockito.verify(wTx, Mockito.times(1)).put(LogicalDatastoreType.CONFIGURATION, yii, nn);
    }

    /**
     * Test method for {@link RestconfServiceImpl#createConfigurationData(NormalizedNodeContext, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testCreateConfigurationDataNormalizedNodeContextUriInfo() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final QName qName = QName.create("urn:nested:module", "2014-06-03", "depth4-cont1");
        Mockito.when(nn.getNodeType()).thenReturn(qName);
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri);
        final YangInstanceIdentifier yii = iiCx.getInstanceIdentifier();
        final NormalizedNodeContext nnCx = new NormalizedNodeContext(iiCx, nn);
        mockWriteDefaultDataBroker(yii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final Response result = restServices.createConfigurationData(nnCx, mockUriInfo);
        Assert.assertNotNull(result);
        Assert.assertEquals(Status.NO_CONTENT, result.getStatusInfo());
        Mockito.verify(wTx, Mockito.timeout(1)).read(LogicalDatastoreType.CONFIGURATION, yii);
        Mockito.verify(wTx, Mockito.timeout(3)).exists(LogicalDatastoreType.CONFIGURATION, yii);
        Mockito.verify(wTx, Mockito.times(1)).put(LogicalDatastoreType.CONFIGURATION, yii, nn);
    }

    /**
     * Test method for {@link RestconfServiceImpl#deleteConfigurationData(java.lang.String)}.
     */
    @Test
    public void testDeleteConfigurationData() {
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final YangInstanceIdentifier yii = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(uri).getInstanceIdentifier();
        mockDeleteDefaultDataBroker();
        final DOMDataWriteTransaction wTx = mockDataBroker.newWriteOnlyTransaction();
        final Response result = restServices.deleteConfigurationData(uri);
        Assert.assertNotNull(result);
        Assert.assertEquals(Status.OK, result.getStatusInfo());
        Mockito.verify(wTx, Mockito.times(1)).delete(LogicalDatastoreType.CONFIGURATION, yii);
    }

    /**
     * Test method for {@link RestconfServiceImpl#subscribeToStream(java.lang.String, javax.ws.rs.core.UriInfo)}.
     */
    @Test
    @Ignore
    public void testSubscribeToStream() {
        // FIXME : add test
    fail("Not yet implemented");
    }

    /**
     * Test method for {@link RestconfServiceImpl#getAvailableStreams(javax.ws.rs.core.UriInfo)}.
     */
    @Test
    public void testGetAvailableStreams() {
        final String expectedLocalName = "streams";
        final NormalizedNodeContext nnCx = restServices.getAvailableStreams(mockUriInfo);
        baseValidationNormalizedNodeContext(nnCx, expectedLocalName);
    }
}
