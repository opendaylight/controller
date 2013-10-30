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

public class XmlUtil {

    public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";

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

    public static Document readXmlToDocument(InputStream xmlContent) throws SAXException, IOException {
        DocumentBuilderFactory factory = getDocumentBuilderFactory();
        DocumentBuilder dBuilder;
        try {
            dBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = dBuilder.parse(xmlContent);

        doc.getDocumentElement().normalize();
        return doc;
    }

    public static Element readXmlToElement(File xmlFile) throws SAXException, IOException {
        return readXmlToDocument(new FileInputStream(xmlFile)).getDocumentElement();
    }

    private static final DocumentBuilderFactory getDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        return factory;
    }

    public static Document newDocument() {
        DocumentBuilderFactory factory = getDocumentBuilderFactory();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            return document;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Element createTextElement(Document document, String name, String content) {
        Element typeElement = document.createElement(name);
        typeElement.appendChild(document.createTextNode(content));
        return typeElement;
    }

    public static void addNamespaceAttr(Element root, String namespace) {
        root.setAttribute(XMLNS_ATTRIBUTE_KEY, namespace);
    }

    public static void addPrefixedNamespaceAttr(Element root, String prefix, String namespace) {
        root.setAttribute(concat(XMLNS_ATTRIBUTE_KEY, prefix), namespace);
    }

    public static Element createPrefixedTextElement(Document document, String key, String prefix, String moduleName) {
        return createTextElement(document, key, concat(prefix, moduleName));
    }

    private static String concat(String prefix, String value) {
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
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, addXmlDeclaration == true ? "no" : "yes");

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();
            return xmlString;
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
