/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil.getRevisionFormat;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.SchemaNodeUtils;

public class URIParametersParsing {

    private RestconfImpl restconf;
    private BrokerFacade mockedBrokerFacade;
    private ControllerContext controllerContext;

    @Before
    public void init() throws FileNotFoundException {
        restconf = RestconfImpl.getInstance();
        mockedBrokerFacade = mock(BrokerFacade.class);
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(TestUtils.loadSchemaContext("/datastore-and-scope-specification"));
        restconf.setControllerContext(controllerContext);
        restconf.setBroker(mockedBrokerFacade);
    }

    @Test
    @Ignore // URI parsing test - not able to catch a motivation + bad mocking response now - it needs to change Controller RPC table holder approach
    public void resolveURIParametersConcreteValues() {
        resolveURIParameters("OPERATIONAL", "SUBTREE", LogicalDatastoreType.OPERATIONAL, DataChangeScope.SUBTREE);
    }

    @Test
    @Ignore // URI parsing test - not able to catch a motivation + bad mocking response now - it needs to change Controller RPC table holder approach
    public void resolveURIParametersDefaultValues() {
        resolveURIParameters(null, null, LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE);
    }

    private void resolveURIParameters(final String datastore, final String scope,
            final LogicalDatastoreType datastoreExpected, final DataChangeScope scopeExpected) {

        final InstanceIdentifierBuilder iiBuilder = YangInstanceIdentifier.builder();
        iiBuilder.node(QName.create("dummyStreamName"));

        final String datastoreValue = datastore == null ? "CONFIGURATION" : datastore;
        final String scopeValue = scope == null ? "BASE" : scope + "";
        Notificator.createListener(iiBuilder.build(), "dummyStreamName/datastore=" + datastoreValue + "/scope="
                + scopeValue);

        final UriInfo mockedUriInfo = mock(UriInfo.class);
        final MultivaluedMap<String, String> mockedMultivaluedMap = mock(MultivaluedMap.class);
        when(mockedMultivaluedMap.getFirst(eq("datastore"))).thenReturn(datastoreValue);
        when(mockedMultivaluedMap.getFirst(eq("scope"))).thenReturn(scopeValue);

        when(mockedUriInfo.getQueryParameters(eq(false))).thenReturn(mockedMultivaluedMap);

         final UriBuilder uriBuilder = UriBuilder.fromUri("www.whatever.com");
         when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);

//       when(mockedBrokerFacade.invokeRpc(any(SchemaPath.class), any(NormalizedNode.class)))
//       .thenReturn(Futures.<DOMRpcResult, DOMRpcException> immediateCheckedFuture(new DefaultDOMRpcResult(Builders.containerBuilder().build())));

        restconf.invokeRpc("sal-remote:create-data-change-event-subscription", prepareDomRpcNode(datastore, scope),
                mockedUriInfo);

        final ListenerAdapter listener = Notificator.getListenerFor("opendaylight-inventory:nodes/datastore="
                + datastoreValue + "/scope=" + scopeValue);
        assertNotNull(listener);

    }

    private NormalizedNodeContext prepareDomRpcNode(final String datastore, final String scope) {
        final SchemaContext schema = controllerContext.getGlobalSchema();
        final Date revDate;
        try {
            revDate = getRevisionFormat().parse("2014-01-14");
        }
        catch (final ParseException e) {
            throw new IllegalStateException(e);
        }
        final Module rpcSalRemoteModule = schema.findModuleByName("sal-remote", revDate);
        final Set<RpcDefinition> setRpcs = rpcSalRemoteModule.getRpcs();
        final QName rpcQName = QName.create(rpcSalRemoteModule.getQNameModule(), "create-data-change-event-subscription");
        final QName rpcInputQName = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote","2014-01-14","input");
        ContainerSchemaNode rpcInputSchemaNode = null;
        for (final RpcDefinition rpc : setRpcs) {
            if (rpcQName.isEqualWithoutRevision(rpc.getQName())) {
                rpcInputSchemaNode = SchemaNodeUtils.getRpcDataSchema(rpc, rpcInputQName);
                break;
            }
        }
        assertNotNull("RPC ContainerSchemaNode was not found!", rpcInputSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> container = Builders.containerBuilder(rpcInputSchemaNode);

        final QName pathQName = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote", "2014-01-14", "path");
        final DataSchemaNode pathSchemaNode = rpcInputSchemaNode.getDataChildByName(pathQName);
        assertTrue(pathSchemaNode instanceof LeafSchemaNode);
        final LeafNode<Object> pathNode = (Builders.leafBuilder((LeafSchemaNode) pathSchemaNode)
                .withValue(YangInstanceIdentifier.builder().node(QName.create("urn:opendaylight:inventory", "2013-08-19", "nodes")).build())).build();
        container.withChild(pathNode);

        final QName dataStoreQName = QName.create("urn:sal:restconf:event:subscription", "2014-7-8", "datastore");
        final DataSchemaNode dsSchemaNode = rpcInputSchemaNode.getDataChildByName(dataStoreQName);
        assertTrue(dsSchemaNode instanceof LeafSchemaNode);
        final LeafNode<Object> dsNode = (Builders.leafBuilder((LeafSchemaNode) dsSchemaNode)
                .withValue(datastore)).build();
        container.withChild(dsNode);

        final QName scopeQName = QName.create("urn:sal:restconf:event:subscription", "2014-7-8", "scope");
        final DataSchemaNode scopeSchemaNode = rpcInputSchemaNode.getDataChildByName(scopeQName);
        assertTrue(scopeSchemaNode instanceof LeafSchemaNode);
        final LeafNode<Object> scopeNode = (Builders.leafBuilder((LeafSchemaNode) scopeSchemaNode)
                .withValue(scope)).build();
        container.withChild(scopeNode);

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, rpcInputSchemaNode, null, schema), container.build());
    }
}
