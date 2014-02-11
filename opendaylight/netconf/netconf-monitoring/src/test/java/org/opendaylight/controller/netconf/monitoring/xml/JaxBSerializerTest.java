/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.xml;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.NetconfTcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.Session1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfSsh;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Transport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.ZeroBasedCounter32;
import org.w3c.dom.Element;

import java.util.Date;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class JaxBSerializerTest {

    @Test
    public void testName() throws Exception {

        NetconfMonitoringService service = new NetconfMonitoringService() {

            @Override
            public Sessions getSessions() {
                return new SessionsBuilder().setSession(Lists.newArrayList(getMockSession(NetconfTcp.class), getMockSession(NetconfSsh.class))).build();
            }

            @Override
            public Schemas getSchemas() {
                return new SchemasBuilder().setSchema(Lists.<Schema>newArrayList()).build();
            }
        };
        NetconfState model = new NetconfState(service);
        Element xml = new JaxBSerializer().toXml(model);
    }

    private Session getMockSession(Class<? extends Transport> transportType) {
        Session mocked = mock(Session.class);
        Session1 mockedSession1 = mock(Session1.class);
        doReturn("client").when(mockedSession1).getSessionIdentifier();
        doReturn(1L).when(mocked).getSessionId();
        doReturn(new DateAndTime(new Date().toString())).when(mocked).getLoginTime();
        doReturn(new Host(new DomainName("address/port"))).when(mocked).getSourceHost();
        doReturn(new ZeroBasedCounter32(0L)).when(mocked).getInBadRpcs();
        doReturn(new ZeroBasedCounter32(0L)).when(mocked).getInRpcs();
        doReturn(new ZeroBasedCounter32(0L)).when(mocked).getOutNotifications();
        doReturn(new ZeroBasedCounter32(0L)).when(mocked).getOutRpcErrors();
        doReturn(transportType).when(mocked).getTransport();
        doReturn("username").when(mocked).getUsername();
        doReturn(mockedSession1).when(mocked).getAugmentation(Session1.class);
        return mocked;
    }
}
