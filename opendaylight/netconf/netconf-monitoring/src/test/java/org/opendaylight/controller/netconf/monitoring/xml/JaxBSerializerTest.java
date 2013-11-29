/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.xml;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfMonitoring;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.w3c.dom.Element;

import java.util.Date;
import java.util.Set;

public class JaxBSerializerTest {

    @Test
    public void testName() throws Exception {

        NetconfMonitoringService service = new NetconfMonitoringService() {
            @Override
            public Set<NetconfManagementSession> getManagementSessions() {
                return Sets.<NetconfManagementSession> newHashSet(new NetconfManagementSession() {
                    @Override
                    public long getId() {
                        return 1;
                    }

                    @Override
                    public Host getSourceHost() {
                        return new Host(new DomainName("address"));
                    }

                    @Override
                    public DateAndTime getLoginTime() {
                        return new DateAndTime(new Date().toString());
                    }
                });
            }
        };
        NetconfMonitoring model = new NetconfMonitoring(service);
        Element xml = new JaxBSerializer().toXml(model);
//        System.out.println(XmlUtil.toString(xml));
    }
}
