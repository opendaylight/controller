/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.exi;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;

@RunWith(Parameterized.class)
public class NetconfStartExiMessageTest {

    @Parameterized.Parameters
    public static Iterable<Object[]> data() throws Exception {
        final String noChangeXml = "<rpc xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:message-id=\"id\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<start-exi xmlns=\"urn:ietf:params:xml:ns:netconf:exi:1.0\">\n" +
                "<alignment>bit-packed</alignment>\n" +
                "</start-exi>\n" +
                "</rpc>";


        final String fullOptionsXml = "<rpc xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:message-id=\"id\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<start-exi xmlns=\"urn:ietf:params:xml:ns:netconf:exi:1.0\">\n" +
                "<alignment>byte-aligned</alignment>\n" +
                "<fidelity>\n" +
                "<comments/>\n" +
                "<dtd/>\n" +
                "<lexical-values/>\n" +
                "<pis/>\n" +
                "<prefixes/>\n" +
                "</fidelity>\n" +
                "</start-exi>\n" +
                "</rpc>";

        final EXIOptions fullOptions = new EXIOptions();
        fullOptions.setAlignmentType(AlignmentType.byteAligned);
        fullOptions.setPreserveLexicalValues(true);
        fullOptions.setPreserveDTD(true);
        fullOptions.setPreserveComments(true);
        fullOptions.setPreserveNS(true);
        fullOptions.setPreservePIs(true);

        return Arrays.asList(new Object[][]{
            {noChangeXml, new EXIOptions()},
            {fullOptionsXml, fullOptions},
        });
    }

    private final String controlXml;
    private final EXIOptions exiOptions;

    public NetconfStartExiMessageTest(final String controlXml, final EXIOptions exiOptions) {
        this.controlXml = controlXml;
        this.exiOptions = exiOptions;
    }

    @Test
    public void testCreate() throws Exception {
        final NetconfStartExiMessage startExiMessage = NetconfStartExiMessage.create(exiOptions, "id");

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        final Diff diff = XMLUnit.compareXML(XMLUnit.buildControlDocument(controlXml), startExiMessage.getDocument());
        assertTrue(diff.toString(), diff.similar());
    }
}