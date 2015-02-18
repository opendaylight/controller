/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfState;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.NetconfTcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.Session1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfSsh;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Transport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.ZeroBasedCounter32;

public class JaxBSerializerTest {

    @Test
    public void testSerialization() throws Exception {

        final NetconfMonitoringService service = new NetconfMonitoringService() {

            @Override
            public void onSessionUp(final NetconfManagementSession session) {

            }

            @Override
            public void onSessionDown(final NetconfManagementSession session) {

            }

            @Override
            public void onCapabilitiesAdded(final Set<Capability> addedCaps) {

            }

            @Override
            public void onCapabilitiesRemoved(final Set<Capability> addedCaps) {

            }

            @Override
            public Sessions getSessions() {
                return new SessionsBuilder().setSession(Lists.newArrayList(getMockSession(NetconfTcp.class), getMockSession(NetconfSsh.class))).build();
            }

            @Override
            public Schemas getSchemas() {
                return new SchemasBuilder().setSchema(Lists.newArrayList(getMockSchema("id", "v1", Yang.class), getMockSchema("id2", "", Yang.class))).build();
            }

            @Override
            public String getSchemaForCapability(final String moduleName, final Optional<String> revision) {
                return null;
            }

            @Override
            public Capabilities getCapabilities() {
                return null;
            }

            @Override
            public AutoCloseable registerListener(final MonitoringListener listener) {
                return new AutoCloseable() {
                    @Override
                    public void close() throws Exception {
                        // NOOP
                    }
                };
            }
        };
        final NetconfState model = new NetconfState(service);
        final String xml = XmlUtil.toString(new JaxBSerializer().toXml(model)).replaceAll("\\s", "");

        assertThat(xml, CoreMatchers.containsString(
                "<schema>" +
                "<format>yang</format>" +
                "<identifier>id</identifier>" +
                "<location>NETCONF</location>" +
                "<namespace>localhost</namespace>" +
                "<version>v1</version>" +
                "</schema>"));

        assertThat(xml, CoreMatchers.containsString(
                "<session>" +
                "<session-id>1</session-id>" +
                "<in-bad-rpcs>0</in-bad-rpcs>" +
                "<in-rpcs>0</in-rpcs>" +
                "<login-time>2010-10-10T12:32:32Z</login-time>" +
                "<out-notifications>0</out-notifications>" +
                "<out-rpc-errors>0</out-rpc-errors>" +
                "<ncme:session-identifier>client</ncme:session-identifier>" +
                "<source-host>192.168.1.1</source-host>" +
                "<transport>ncme:netconf-tcp</transport>" +
                "<username>username</username>" +
                "</session>"));
    }

    private Schema getMockSchema(final String id, final String version, final Class<Yang> format) {
        final Schema mock = mock(Schema.class);

        doReturn(format).when(mock).getFormat();
        doReturn(id).when(mock).getIdentifier();
        doReturn(new Uri("localhost")).when(mock).getNamespace();
        doReturn(version).when(mock).getVersion();
        doReturn(Lists.newArrayList(new Schema.Location(Schema.Location.Enumeration.NETCONF))).when(mock).getLocation();
        doReturn(new SchemaKey(format, id, version)).when(mock).getKey();
        return mock;
    }

    private Session getMockSession(final Class<? extends Transport> transportType) {
        final Session mocked = mock(Session.class);
        final Session1 mockedSession1 = mock(Session1.class);
        doReturn("client").when(mockedSession1).getSessionIdentifier();
        doReturn(1L).when(mocked).getSessionId();
        doReturn(new DateAndTime("2010-10-10T12:32:32Z")).when(mocked).getLoginTime();
        doReturn(new Host(new DomainName("192.168.1.1"))).when(mocked).getSourceHost();
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
