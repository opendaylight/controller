/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.util.xml;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
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

public final class XmlUtil {

    public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final DocumentBuilderFactory BUILDER_FACTORY;
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            // Performance improvement for messages with size <10k according to
            // https://xerces.apache.org/xerces2-j/faq-performance.html
            factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        } catch (ParserConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDER_FACTORY = factory;
    }

    private static final ThreadLocal<DocumentBuilder> DEFAULT_DOM_BUILDER = new ThreadLocal<DocumentBuilder>(){
        @Override
        protected DocumentBuilder initialValue() {
            try {
                return BUILDER_FACTORY.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Failed to create threadLocal dom builder", e);
            }
        }

        @Override
        public void set(DocumentBuilder value) {
            throw new UnsupportedOperationException();
        }
    };

    private XmlUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Element readXmlToElement(final String xmlContent) throws SAXException, IOException {
        Document doc = readXmlToDocument(xmlContent);
        return doc.getDocumentElement();
    }

    public static Element readXmlToElement(final InputStream xmlContent) throws SAXException, IOException {
        Document doc = readXmlToDocument(xmlContent);
        return doc.getDocumentElement();
    }

    public static Document readXmlToDocument(final String xmlContent) throws SAXException, IOException {
        return readXmlToDocument(new ByteArrayInputStream(xmlContent.getBytes(Charsets.UTF_8)));
    }

    // TODO improve exceptions throwing
    // along with XmlElement

    public static Document readXmlToDocument(final InputStream xmlContent) throws SAXException, IOException {
        Document doc = DEFAULT_DOM_BUILDER.get().parse(xmlContent);

        doc.getDocumentElement().normalize();
        return doc;
    }

    public static Element readXmlToElement(final File xmlFile) throws SAXException, IOException {
        return readXmlToDocument(new FileInputStream(xmlFile)).getDocumentElement();
    }

    public static Document newDocument() {
        return DEFAULT_DOM_BUILDER.get().newDocument();
    }

    public static Element createElement(final Document document, final String qName, final Optional<String> namespaceURI) {
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

    public static Element createTextElement(final Document document, final String qName, final String content, final Optional<String> namespaceURI) {
        Element typeElement = createElement(document, qName, namespaceURI);
        typeElement.appendChild(document.createTextNode(content));
        return typeElement;
    }

    public static Element createTextElementWithNamespacedContent(final Document document, final String qName, final String prefix,
                                                                 final String namespace, final String contentWithoutPrefix) {

        return createTextElementWithNamespacedContent(document, qName, prefix, namespace, contentWithoutPrefix, Optional.<String>absent());
    }

    public static Element createTextElementWithNamespacedContent(final Document document, final String qName, final String prefix,
                                                                 final String namespace, final String contentWithoutPrefix, final Optional<String> namespaceURI) {

        String content = createPrefixedValue(XmlMappingConstants.PREFIX, contentWithoutPrefix);
        Element element = createTextElement(document, qName, content, namespaceURI);
        String prefixedNamespaceAttr = createPrefixedValue(XMLNS_ATTRIBUTE_KEY, prefix);
        element.setAttributeNS(XMLNS_URI, prefixedNamespaceAttr, namespace);
        return element;
    }

    public static String createPrefixedValue(final String prefix, final String value) {
        return prefix + ":" + value;
    }

    public static String toString(final Document document) {
        return toString(document.getDocumentElement());
    }

    public static String toString(final Element xml) {
        return toString(xml, false);
    }

    public static String toString(final XmlElement xmlElement) {
        return toString(xmlElement.getDomElement(), false);
    }

    public static String toString(final Element xml, final boolean addXmlDeclaration) {
        try {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, addXmlDeclaration ? "no" : "yes");

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (Exception | TransformerFactoryConfigurationError e) {
            throw new IllegalStateException("Unable to serialize xml element " + xml, e);
        }
    }

    public static String toString(final Document doc, final boolean addXmlDeclaration) {
        return toString(doc.getDocumentElement(), addXmlDeclaration);
    }

    public static Schema loadSchema(final InputStream... fromStreams) {
        Source[] sources = new Source[fromStreams.length];
        int i = 0;
        for (InputStream stream : fromStreams) {
            sources[i++] = new StreamSource(stream);
        }

        try {
            return SCHEMA_FACTORY.newSchema(sources);
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to instantiate XML schema", e);
        }
    }

    public static Object evaluateXPath(final XPathExpression expr, final Object rootNode, final QName returnType) {
        try {
            return expr.evaluate(rootNode, returnType);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Error while evaluating xpath expression " + expr, e);
        }
    }

    public static Document createDocumentCopy(final Document original) {
        final Document copiedDocument = newDocument();
        final Node copiedRoot = copiedDocument.importNode(original.getDocumentElement(), true);
        copiedDocument.appendChild(copiedRoot);
        return copiedDocument;
    }
}
