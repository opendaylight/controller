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
import java.io.IOException;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.opendaylight.controller.netconf.notifications.impl.ops.Get;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.StreamsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamKey;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class GetTest {

    @Test
    public void testSerializeStreamsSubtree() throws Exception {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final StreamBuilder streamBuilder = new StreamBuilder();
        final StreamNameType base = new StreamNameType("base");
        streamBuilder.setName(base);
        streamBuilder.setKey(new StreamKey(base));
        streamBuilder.setDescription("description");
        streamBuilder.setReplaySupport(false);
        streamsBuilder.setStream(Lists.newArrayList(streamBuilder.build()));
        final Streams streams = streamsBuilder.build();

        final Document response = getBlankResponse();
        Get.serializeStreamsSubtree(response, streams);
        final Diff diff = XMLUnit.compareXML(XmlUtil.toString(response),
                "<rpc-reply message-id=\"101\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<data>\n" +
                "<netconf xmlns=\"urn:ietf:params:xml:ns:netmod:notification\">\n" +
                "<streams>\n" +
                "<stream>\n" +
                "<name>base</name>\n" +
                "<description>description</description>\n" +
                "<replaySupport>false</replaySupport>\n" +
                "</stream>\n" +
                "</streams>\n" +
                "</netconf>\n" +
                "</data>\n" +
                "</rpc-reply>\n");

        assertTrue(diff.toString(), diff.identical());
    }

    private Document getBlankResponse() throws IOException, SAXException {

        return XmlUtil.readXmlToDocument("<rpc-reply message-id=\"101\"\n" +
                "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<data>\n" +
                "</data>\n" +
                "</rpc-reply>");
    }
}