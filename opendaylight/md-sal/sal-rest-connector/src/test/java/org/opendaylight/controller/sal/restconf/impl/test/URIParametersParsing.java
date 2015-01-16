/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;

public class URIParametersParsing {

    private RestconfImpl restconf;
    private BrokerFacade mockedBrokerFacade;

    @Before
    public void init() throws FileNotFoundException {
        restconf = RestconfImpl.getInstance();
        mockedBrokerFacade = mock(BrokerFacade.class);
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(TestUtils.loadSchemaContext("/datastore-and-scope-specification"));
        restconf.setControllerContext(controllerContext);
        restconf.setBroker(mockedBrokerFacade);
    }

    @Test
    public void resolveURIParametersConcreteValues() {
        resolveURIParameters("OPERATIONAL", "SUBTREE", LogicalDatastoreType.OPERATIONAL, DataChangeScope.SUBTREE);
    }

    @Test
    public void resolveURIParametersDefaultValues() {
        resolveURIParameters(null, null, LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE);
    }

    private void resolveURIParameters(final String datastore, final String scope,
            final LogicalDatastoreType datastoreExpected, final DataChangeScope scopeExpected) {

        InstanceIdentifierBuilder iiBuilder = YangInstanceIdentifier.builder();
        iiBuilder.node(QName.create("dummyStreamName"));

        final String datastoreValue = datastore == null ? "CONFIGURATION" : datastore;
        final String scopeValue = scope == null ? "BASE" : scope + "";
        Notificator.createListener(iiBuilder.build(), "dummyStreamName/datastore=" + datastoreValue + "/scope="
                + scopeValue);

        UriInfo mockedUriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> mockedMultivaluedMap = mock(MultivaluedMap.class);
        when(mockedMultivaluedMap.getFirst(eq("datastore"))).thenReturn(datastoreValue);
        when(mockedMultivaluedMap.getFirst(eq("scope"))).thenReturn(scopeValue);

        when(mockedUriInfo.getQueryParameters(eq(false))).thenReturn(mockedMultivaluedMap);

         UriBuilder uriBuilder = UriBuilder.fromUri("www.whatever.com");
         when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);

        restconf.invokeRpc("sal-remote:create-data-change-event-subscription", prepareRpcNode(datastore, scope),
                mockedUriInfo);

        ListenerAdapter listener = Notificator.getListenerFor("opendaylight-inventory:nodes/datastore="
                + datastoreValue + "/scope=" + scopeValue);
        assertNotNull(listener);

    }

    private CompositeNode prepareRpcNode(final String datastore, final String scope) {
        CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote",
                "2014-01-14", "input"));
        inputBuilder.addLeaf(
                QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote", "2014-01-14", "path"),
                YangInstanceIdentifier.builder().node(QName.create("urn:opendaylight:inventory", "2013-08-19", "nodes")).build());
        inputBuilder.addLeaf(QName.create("urn:sal:restconf:event:subscription", "2014-7-8", "datastore"), datastore);
        inputBuilder.addLeaf(QName.create("urn:sal:restconf:event:subscription", "2014-7-8", "scope"), scope);
        return inputBuilder.build();
    }
}
