/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.exi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;

@RunWith(Parameterized.class)
public class EXIParametersTest {

    @Parameterized.Parameters
    public static Iterable<Object[]> data() throws Exception {
        final String noChangeXml =
                "<start-exi xmlns=\"urn:ietf:params:xml:ns:netconf:exi:1.0\">\n" +
                "<alignment>bit-packed</alignment>\n" +
                "</start-exi>\n";


        final String fullOptionsXml =
                "<start-exi xmlns=\"urn:ietf:params:xml:ns:netconf:exi:1.0\">\n" +
                "<alignment>byte-aligned</alignment>\n" +
                "<fidelity>\n" +
                "<comments/>\n" +
                "<dtd/>\n" +
                "<lexical-values/>\n" +
                "<pis/>\n" +
                "<prefixes/>\n" +
                "</fidelity>\n" +
                "</start-exi>\n";

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

    private final String sourceXml;
    private final EXIOptions exiOptions;

    public EXIParametersTest(final String sourceXml, final EXIOptions exiOptions) {
        this.sourceXml = sourceXml;
        this.exiOptions = exiOptions;
    }

    @Test
    public void testFromXmlElement() throws Exception {
        final EXIParameters opts =
                EXIParameters.fromXmlElement(
                        XmlElement.fromDomElement(
                                XmlUtil.readXmlToElement(sourceXml)));


        assertEquals(opts.getOptions().getAlignmentType(), exiOptions.getAlignmentType());
        assertEquals(opts.getOptions().getPreserveComments(), exiOptions.getPreserveComments());
        assertEquals(opts.getOptions().getPreserveLexicalValues(), exiOptions.getPreserveLexicalValues());
        assertEquals(opts.getOptions().getPreserveNS(), exiOptions.getPreserveNS());
        assertEquals(opts.getOptions().getPreserveDTD(), exiOptions.getPreserveDTD());
        assertEquals(opts.getOptions().getPreserveNS(), exiOptions.getPreserveNS());
    }
}