/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.util.xml;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlElementTest {

    private final String elementAsString = "<top xmlns=\"namespace\" xmlns:a=\"attrNamespace\" a:attr1=\"value1\" attr2=\"value2\">" +
            "<inner>" +
            "<deepInner>deepValue</deepInner>" +
            "</inner>" +
            "<innerNamespace xmlns=\"innerNamespace\">innerNamespaceValue</innerNamespace>" +
            "<innerPrefixed xmlns:b=\"prefixedValueNamespace\">b:valueWithPrefix</innerPrefixed>" +
            "</top>";
    private Document document;
    private Element element;
    private XmlElement xmlElement;

    @Before
    public void setUp() throws Exception {
        document = XmlUtil.readXmlToDocument(elementAsString);
        element = document.getDocumentElement();
        xmlElement = XmlElement.fromDomElement(element);
    }

    @Test
    public void testConstruct() throws Exception {
        final XmlElement fromString = XmlElement.fromString(elementAsString);
        assertEquals(fromString, xmlElement);
        XmlElement.fromDomDocument(document);
        XmlElement.fromDomElement(element);
        XmlElement.fromDomElementWithExpected(element, "top");
        XmlElement.fromDomElementWithExpected(element, "top", "namespace");

        try {
            XmlElement.fromString("notXml");
            fail();
        } catch (final DocumentedException e) {}

        try {
            XmlElement.fromDomElementWithExpected(element, "notTop");
            fail();
        } catch (final DocumentedException e) {}

        try {
            XmlElement.fromDomElementWithExpected(element, "top", "notNamespace");
            fail();
        } catch (final DocumentedException e) {}
    }

    @Test
    public void testGetters() throws Exception {
        assertEquals(element, xmlElement.getDomElement());
        assertEquals(element.getElementsByTagName("inner").getLength(), xmlElement.getElementsByTagName("inner").getLength());

        assertEquals("top", xmlElement.getName());
        assertTrue(xmlElement.hasNamespace());
        assertEquals("namespace", xmlElement.getNamespace());
        assertEquals("namespace", xmlElement.getNamespaceAttribute());
        assertEquals(Optional.of("namespace"), xmlElement.getNamespaceOptionally());

        assertEquals("value1", xmlElement.getAttribute("attr1", "attrNamespace"));
        assertEquals("value2", xmlElement.getAttribute("attr2"));
        assertEquals(2 + 2/*Namespace definition*/, xmlElement.getAttributes().size());

        assertEquals(3, xmlElement.getChildElements().size());
        assertEquals(1, xmlElement.getChildElements("inner").size());
        assertTrue(xmlElement.getOnlyChildElementOptionally("inner").isPresent());
        assertTrue(xmlElement.getOnlyChildElementWithSameNamespaceOptionally("inner").isPresent());
        assertEquals(0, xmlElement.getChildElements("unknown").size());
        assertFalse(xmlElement.getOnlyChildElementOptionally("unknown").isPresent());
        assertEquals(1, xmlElement.getChildElementsWithSameNamespace("inner").size());
        assertEquals(0, xmlElement.getChildElementsWithSameNamespace("innerNamespace").size());
        assertEquals(1, xmlElement.getChildElementsWithinNamespace("innerNamespace", "innerNamespace").size());
        assertTrue(xmlElement.getOnlyChildElementOptionally("innerNamespace", "innerNamespace").isPresent());
        assertFalse(xmlElement.getOnlyChildElementOptionally("innerNamespace", "unknownNamespace").isPresent());

        final XmlElement noNamespaceElement = XmlElement.fromString("<noNamespace/>");
        assertFalse(noNamespaceElement.hasNamespace());
        try {
            noNamespaceElement.getNamespace();
            fail();
        } catch (final MissingNameSpaceException e) {}

        final XmlElement inner = xmlElement.getOnlyChildElement("inner");
        final XmlElement deepInner = inner.getOnlyChildElementWithSameNamespaceOptionally().get();
        assertEquals(deepInner, inner.getOnlyChildElementWithSameNamespace());
        assertEquals(Optional.<XmlElement>absent(), xmlElement.getOnlyChildElementOptionally("unknown"));
        assertEquals("deepValue", deepInner.getTextContent());
        assertEquals("deepValue", deepInner.getOnlyTextContentOptionally().get());
        assertEquals("deepValue", deepInner.getOnlyTextContentOptionally().get());
    }

    @Test
    public void testExtractNamespaces() throws Exception {
        final XmlElement innerPrefixed = xmlElement.getOnlyChildElement("innerPrefixed");
        Map.Entry<String, String> namespaceOfTextContent = innerPrefixed.findNamespaceOfTextContent();

        assertNotNull(namespaceOfTextContent);
        assertEquals("b", namespaceOfTextContent.getKey());
        assertEquals("prefixedValueNamespace", namespaceOfTextContent.getValue());
        final XmlElement innerNamespace = xmlElement.getOnlyChildElement("innerNamespace");
        namespaceOfTextContent = innerNamespace.findNamespaceOfTextContent();

        assertEquals("", namespaceOfTextContent.getKey());
        assertEquals("innerNamespace", namespaceOfTextContent.getValue());
    }

    @Test
    public void testUnrecognisedElements() throws Exception {
        xmlElement.checkUnrecognisedElements(xmlElement.getOnlyChildElement("inner"), xmlElement.getOnlyChildElement("innerPrefixed"), xmlElement.getOnlyChildElement("innerNamespace"));

        try {
            xmlElement.checkUnrecognisedElements(xmlElement.getOnlyChildElement("inner"));
            fail();
        } catch (final DocumentedException e) {
            assertThat(e.getMessage(), both(containsString("innerNamespace")).and(containsString("innerNamespace")));
        }
    }
}
