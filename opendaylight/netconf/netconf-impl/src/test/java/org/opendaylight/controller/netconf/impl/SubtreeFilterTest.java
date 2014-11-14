/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@RunWith(value = Parameterized.class)
public class SubtreeFilterTest {
    private static final Logger LOG = LoggerFactory.getLogger(SubtreeFilterTest.class);

    private final int directoryIndex;

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            result.add(new Object[]{i});
        }
        return result;
    }

    public SubtreeFilterTest(int directoryIndex) {
        this.directoryIndex = directoryIndex;
    }

    @Before
    public void setUp(){
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void test() throws Exception {
        Document requestDocument = getDocument("request.xml");
        Document preFilterDocument = getDocument("pre-filter.xml");
        Document postFilterDocument = getDocument("post-filter.xml");
        Document actualPostFilterDocument = SubtreeFilter.applySubtreeFilter(requestDocument, preFilterDocument);
        LOG.info("Actual document: {}", XmlUtil.toString(actualPostFilterDocument));
        Diff diff = XMLUnit.compareXML(postFilterDocument, actualPostFilterDocument);
        assertTrue(diff.toString(), diff.similar());

    }

    public Document getDocument(String fileName) throws SAXException, IOException {
        return XmlUtil.readXmlToDocument(getClass().getResourceAsStream("/subtree/" + directoryIndex + "/" +
                fileName));
    }
}
