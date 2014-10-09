/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Sets;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;

public class NetconfDeviceDataBrokerTest {

    @Mock
    private RpcImplementation rpcImplementation;
    @Mock
    private SchemaContext schemaContext;

    private NetconfSessionCapabilities sessionCaps;
    private NetconfDeviceDataBroker broker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        QName qname = new QName(new URI("scheme","ssp","fragment"), "lname");
        sessionCaps = NetconfSessionCapabilities.fromStrings(Sets.newHashSet("a1"));
        doReturn(qname).when(schemaContext).getQName();
        broker = new NetconfDeviceDataBroker(new RemoteDeviceId("id"), rpcImplementation, schemaContext, sessionCaps);
    }

    @Test
    public void test() throws Exception {
        assertNotNull(broker.newReadOnlyTransaction());
        assertNotNull(broker.newReadWriteTransaction());
        assertNotNull(broker.newWriteOnlyTransaction());
    }
}
