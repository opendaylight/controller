/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications.impl.ops;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChangeBuilder;

public class NotificationsTransformUtilTest {

    private static final Date DATE = new Date();
    private static final String innerNotification = "<netconf-capability-change xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-notifications\">" +
            "<deleted-capability>uri3</deleted-capability>" +
            "<deleted-capability>uri4</deleted-capability>" +
            "<added-capability>uri1</added-capability>" +
            "</netconf-capability-change>";

    private static final String expectedNotification = "<notification xmlns=\"urn:ietf:params:netconf:capability:notification:1.0\">" +
            innerNotification +
            "<eventTime>" + new SimpleDateFormat(NetconfNotification.RFC3339_DATE_FORMAT_BLUEPRINT).format(DATE) + "</eventTime>" +
            "</notification>";

    @Test
    public void testTransform() throws Exception {
        final NetconfCapabilityChangeBuilder netconfCapabilityChangeBuilder = new NetconfCapabilityChangeBuilder();

        netconfCapabilityChangeBuilder.setAddedCapability(Lists.newArrayList(new Uri("uri1"), new Uri("uri1")));
        netconfCapabilityChangeBuilder.setDeletedCapability(Lists.newArrayList(new Uri("uri3"), new Uri("uri4")));

        final NetconfCapabilityChange capabilityChange = netconfCapabilityChangeBuilder.build();
        final NetconfNotification transform = NotificationsTransformUtil.transform(capabilityChange, DATE);

        final String serialized = XmlUtil.toString(transform.getDocument());

        XMLUnit.setIgnoreWhitespace(true);
        final Diff diff = XMLUnit.compareXML(expectedNotification, serialized);
        assertTrue(diff.toString(), diff.similar());
    }

    @Test
    public void testTransformFromDOM() throws Exception {
        final NetconfNotification netconfNotification = new NetconfNotification(XmlUtil.readXmlToDocument(innerNotification), DATE);

        XMLUnit.setIgnoreWhitespace(true);
        final Diff diff = XMLUnit.compareXML(expectedNotification, netconfNotification.toString());
        assertTrue(diff.toString(), diff.similar());
    }

}