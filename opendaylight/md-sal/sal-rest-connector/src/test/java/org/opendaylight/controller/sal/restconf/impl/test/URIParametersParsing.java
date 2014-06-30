/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder;

public class URIParametersParsing {

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
        RestconfImpl restconf = RestconfImpl.getInstance();
        BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);
        restconf.setBroker(mockedBrokerFacade);

        InstanceIdentifierBuilder iiBuilder = InstanceIdentifier.builder();
        iiBuilder.node(QName.create("dummyStreamName"));

        final String datastoreValue = datastore == null ? "CONFIGURATION" : datastore;
        final String scopeValue = scope == null ? "BASE" : scope + "";
        Notificator.createListener(iiBuilder.build(),
                "dummyStreamName?datastore=" + datastoreValue + "&scope=" + scopeValue);

        UriInfo mockedUriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> mockedMultivaluedMap = mock(MultivaluedMap.class);
        when(mockedMultivaluedMap.getFirst(eq("datastore"))).thenReturn(datastore);
        when(mockedMultivaluedMap.getFirst(eq("scope"))).thenReturn(scope);

        when(mockedUriInfo.getQueryParameters(eq(false))).thenReturn(mockedMultivaluedMap);

        UriBuilder uriBuilder = UriBuilder.fromUri("www.whatever.com");
        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);

        restconf.subscribeToStream("dummyStreamName", mockedUriInfo);

        verify(mockedBrokerFacade).registerToListenDataChanges(eq(datastoreExpected), eq(scopeExpected),
                any(ListenerAdapter.class));

    }
}
