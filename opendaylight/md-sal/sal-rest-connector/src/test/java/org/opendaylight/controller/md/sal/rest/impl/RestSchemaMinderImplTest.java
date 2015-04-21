/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.impl;

import static org.junit.Assert.fail;
import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.rest.RestSchemaMinder;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.impl
 *
 * Base test class method for quick testing of {@link RestSchemaMinder} implementation
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Apr 20, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RestSchemaMinderImplTest {

    private RestSchemaMinder restSchemaMinder;

    @Mock
    private DOMMountPointService mountPointService;
    @Mock
    private DOMMountPoint mountPoint;


    @Before
    public void initialization() {
        Mockito.when(mountPointService.getMountPoint(Matchers.any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountPoint));
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules", null);
        restSchemaMinder = new RestSchemaMinderImpl(mountPointService, schemaCx);
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#RestSchemaMinderImpl(DOMMountPointService, SchemaContext)}.
     */
    @Test(expected=NullPointerException.class)
    public void testRestSchemaMinderImplNullInputs() {
        final RestSchemaMinder rsMinder = new RestSchemaMinderImpl(null, null);
        fail("Expect Exception not new class " + rsMinder);
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#RestSchemaMinderImpl(DOMMountPointService, SchemaContext)}.
     */
    @Test(expected=NullPointerException.class)
    public void testRestSchemaMinderImplNullSchema() {
        final RestSchemaMinder rsMinder = new RestSchemaMinderImpl(mountPointService, null);
        fail("Expect Exception not new class " + rsMinder);
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#tell(SchemaContext)}.
     */
    @Test
    public void testTell() {
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules/modules-behind-mount-point", null);
        restSchemaMinder.tell(schemaCx);
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#tell(SchemaContext)}.
     */
    @Test(expected=NullPointerException.class)
    public void testTellNull() {
        restSchemaMinder.tell(null);
        fail("Expect Exception");
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getRpcDefinition(java.lang.String)}.
     */
    @Test
    public void testGetRpcDefinition() {
        final Map<String, String> rpcs = new HashMap<>(4);
        rpcs.put("dummy-rpc1-module1", "module1");
        rpcs.put("dummy-rpc2-module1", "module1");
        rpcs.put("dummy-rpc1-module2", "module2");
        rpcs.put("dummy-rpc2-module2", "module2");
        for (final Entry<String, String> rpc : rpcs.entrySet()) {
            final RpcDefinition rpcDef = restSchemaMinder.getRpcDefinition(rpc.getValue() + ":" + rpc.getKey());
            Assert.assertNotNull(rpcDef);
            Assert.assertNotNull(rpcDef.getQName());
            Assert.assertEquals(rpc.getKey(), rpcDef.getQName().getLocalName());
        }
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getRestconfModule()}.
     */
    @Test
    public void testGetRestconfModule(){
        final Module restconfModule = restSchemaMinder.getRestconfModule();
        Assert.assertNotNull(restconfModule);
        Assert.assertNotNull(restconfModule.getQNameModule());
        Assert.assertNotNull(restconfModule.getQNameModule().getNamespace());
        Assert.assertTrue(restconfModule.getQNameModule().getNamespace().toString().equalsIgnoreCase("urn:ietf:params:xml:ns:yang:ietf-restconf"));
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getStreamListSchemaNode()}.
     */
    @Test
    public void testGetStreamListSchemaNode(){
        final ListSchemaNode streamListSchemaNode = restSchemaMinder.getStreamListSchemaNode();
        validateSchemaNode(streamListSchemaNode, "stream");
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getStreamContainerSchemaNode()}.
     */
    @Test
    public void testGetStreamContainerSchemaNode(){
        final ContainerSchemaNode streamContainerSchemaNode = restSchemaMinder.getStreamContainerSchemaNode();
        validateSchemaNode(streamContainerSchemaNode, "streams");
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getModuleListSchemaNode()}.
     */
    @Test
    public void testGetModuleListSchemaNode(){
        final ListSchemaNode moduleListSchemaNode = restSchemaMinder.getModuleListSchemaNode();
        validateSchemaNode(moduleListSchemaNode, "module");
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#getModuleContainerSchemaNode()}.
     */
    @Test
    public void testGetModuleContainerSchemaNode(){
        final ContainerSchemaNode moduleContainerSchemaNode = restSchemaMinder.getModuleContainerSchemaNode();
        validateSchemaNode(moduleContainerSchemaNode, "modules");
    }

    private static void validateSchemaNode(final SchemaNode schemaNode, final String localName) {
        Assert.assertNotNull(schemaNode);
        Assert.assertNotNull(schemaNode.getQName());
        Assert.assertTrue(schemaNode.getQName().getLocalName().equalsIgnoreCase(localName));
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequest(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestModulDepth1(){
        final String uri = "nested-module:depth1-cont";
        final InstanceIdentifierContext<?> iiCx = restSchemaMinder.parseUriRequest(uri);
        baseValidateParsedUri(iiCx, new String[]{"depth1-cont"});
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequest(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestNestedDepth2(){
        final String uri = "nested-module:depth1-cont/depth2-cont1";
        final InstanceIdentifierContext<?> iiCx = restSchemaMinder.parseUriRequest(uri);
        baseValidateParsedUri(iiCx, new String[]{"depth1-cont","depth2-cont1"});
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequest(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestNestedDepth3(){
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1";
        final InstanceIdentifierContext<?> iiCx = restSchemaMinder.parseUriRequest(uri);
        baseValidateParsedUri(iiCx, new String[]{"depth1-cont","depth2-cont1","depth3-cont1"});
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequest(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestNestedDepth4(){
        final String uri = "nested-module:depth1-cont/depth2-cont1/depth3-cont1/depth4-cont1";
        final InstanceIdentifierContext<?> iiCx = restSchemaMinder.parseUriRequest(uri);
        baseValidateParsedUri(iiCx, new String[]{"depth1-cont","depth2-cont1","depth3-cont1","depth4-cont1"});
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequest(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestRpc(){
        final String uri = "module1:dummy-rpc1-module1";
        final InstanceIdentifierContext<?> iiCx = restSchemaMinder.parseUriRequest(uri);
        baseValidateParsedUri(iiCx, new String[]{"dummy-rpc1-module1"});
    }

    private static void baseValidateParsedUri(final InstanceIdentifierContext<?> iiCx, final String[] pathArgs) {
        Assert.assertNotNull(iiCx);
        Assert.assertNotNull(iiCx.getSchemaContext());
        Assert.assertNotNull(iiCx.getSchemaNode());
        Assert.assertNotNull(iiCx.getInstanceIdentifier());
        final Iterable<PathArgument> pathArgIter = iiCx.getInstanceIdentifier().getPathArguments();
        Assert.assertNotNull(pathArgIter);
        final Iterator<PathArgument> pathIterator = pathArgIter.iterator();
        Assert.assertNotNull(pathIterator);
        for (final String pathArg : pathArgs) {
            Assert.assertTrue(pathIterator.hasNext());
            Assert.assertNotNull(pathIterator.next().getNodeType().toString().contains(pathArg));
        }
        Assert.assertTrue(iiCx.getSchemaNode().getQName().toString().contains(pathArgs[pathArgs.length - 1]));
    }

    /**
     * Test method for {@link RestSchemaMinderImpl#parseUriRequestToMountPoint(java.lang.String)}.
     */
    @Test
    public void testParseUriRequestToMountPoint(){
        final String uri = "nested-module:depth1-cont/depth2-cont1/yang-ext:mount/nested-module:depth1-cont/depth2-cont1";
        final DOMMountPoint mPoint = restSchemaMinder.parseUriRequestToMountPoint(uri);
        Assert.assertNotNull(mPoint);
        Assert.assertEquals(mPoint, mountPoint);
    }

}
