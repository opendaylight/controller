/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;

public final class XmlUtil {

    public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final DocumentBuilderFactory BUILDERFACTORY;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDERFACTORY = factory;
    }

    private XmlUtil() {}

    public static Element readXmlToElement(String xmlContent) throws SAXException, IOException {
        Document doc = readXmlToDocument(xmlContent);
        return doc.getDocumentElement();
    }

    public static Element readXmlToElement(InputStream xmlContent) throws SAXException, IOException {
        Document doc = readXmlToDocument(xmlContent);
        return doc.getDocumentElement();
    }

    public static Document readXmlToDocument(String xmlContent) throws SAXException, IOException {
        return readXmlToDocument(new ByteArrayInputStream(xmlContent.getBytes(Charsets.UTF_8)));
    }

    // TODO improve exceptions throwing
    // along with XmlElement

    public static Document readXmlToDocument(InputStream xmlContent) throws SAXException, IOException {
        DocumentBuilder dBuilder;
        try {
            dBuilder = BUILDERFACTORY.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to parse XML document", e);
        }
        Document doc = dBuilder.parse(xmlContent);

        doc.getDocumentElement().normalize();
        return doc;
    }

    public static Element readXmlToElement(File xmlFile) throws SAXException, IOException {
        return readXmlToDocument(new FileInputStream(xmlFile)).getDocumentElement();
    }

    public static Document newDocument() {
        try {
            DocumentBuilder builder = BUILDERFACTORY.newDocumentBuilder();
            Document document = builder.newDocument();
            return document;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create document", e);
        }
    }

    public static Element createElement(final Document document, String qName, Optional<String> namespaceURI) {
        if(namespaceURI.isPresent()) {
            final Element element = document.createElementNS(namespaceURI.get(), qName);
            String name = XMLNS_ATTRIBUTE_KEY;
            if(element.getPrefix() != null) {
                name += ":" + element.getPrefix();
            }
            element.setAttributeNS(XMLNS_URI, name, namespaceURI.get());
            return element;
        }
        return document.createElement(qName);
    }

    public static Element createTextElement(Document document, String qName, String content, Optional<String> namespaceURI) {
        Element typeElement = createElement(document, qName, namespaceURI);
        typeElement.appendChild(document.createTextNode(content));
        return typeElement;
    }

    public static Element createPrefixedTextElement(Document document, String qName, String prefix, String content, Optional<String> namespace) {
        return createTextElement(document, qName, createPrefixedValue(prefix, content), namespace);
    }

    public static String createPrefixedValue(String prefix, String value) {
        return prefix + ":" + value;
    }

    public static String toString(Document document) {
        return toString(document.getDocumentElement());
    }

    public static String toString(Element xml) {
        return toString(xml, false);
    }

    public static String toString(XmlElement xmlElement) {
        return toString(xmlElement.getDomElement(), false);
    }

    public static String toString(Element xml, boolean addXmlDeclaration) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, addXmlDeclaration ? "no" : "yes");

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (IllegalArgumentException | TransformerFactoryConfigurationError | TransformerException e) {
            throw new RuntimeException("Unable to serialize xml element " + xml, e);
        }
    }

    public static String toString(Document doc, boolean addXmlDeclaration) {
        return toString(doc.getDocumentElement(), addXmlDeclaration);
    }

    public static Schema loadSchema(InputStream... fromStreams) {
        Source[] sources = new Source[fromStreams.length];
        int i = 0;
        for (InputStream stream : fromStreams) {
            sources[i++] = new StreamSource(stream);
        }

        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            return schemaFactory.newSchema(sources);
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to instantiate XML schema", e);
        }
    }

    public static Object evaluateXPath(XPathExpression expr, Object rootNode, QName returnType) {
        try {
            return expr.evaluate(rootNode, returnType);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Error while evaluating xpath expression " + expr, e);
        }
    }

    public static Document createDocumentCopy(Document original) {
        final Document copiedDocument = newDocument();
        final Node copiedRoot = copiedDocument.importNode(original.getDocumentElement(), true);
        copiedDocument.appendChild(copiedRoot);
        return copiedDocument;
    }
}
