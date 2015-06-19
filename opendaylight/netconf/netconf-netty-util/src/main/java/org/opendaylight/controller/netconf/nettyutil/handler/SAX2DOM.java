/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.opendaylight.controller.config.util.xml.XmlUtil.XMLNS_ATTRIBUTE_KEY;
import static org.opendaylight.controller.config.util.xml.XmlUtil.XMLNS_URI;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

/**
 * Sax content handler that produces DOM nodes.
 */
public class SAX2DOM implements ContentHandler {

    private static final Joiner COLON_JOINER = Joiner.on(":");

    private Document document;
    private final Deque<Node> nodes = new LinkedList<>();
    private Map<String, String> namespaces = Maps.newHashMap();

    public void setDocument(final Document document) {
        this.document = document;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        final Node currentNode = nodes.peek();

        if (currentNode == document) {
            return;
        }

        Node lastChild = currentNode.getLastChild();

        final String text = new String(ch, start, length);
        if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
            // merge text content in case a single text node is presented in multiple calls
            ((Text) lastChild).appendData(text);
        } else {
            currentNode.appendChild(document.createTextNode(text));
        }
    }

    @Override
    public void startDocument() {
        nodes.push(document);
    }

    @Override
    public void endDocument() {
        nodes.pop();
        document = null;
        // We are done, stack should be empty
        Preconditions.checkState(nodes.isEmpty());
        Preconditions.checkState(namespaces.isEmpty());
    }

    @Override
    public void startElement(String namespace, String localName, String qName, Attributes attrs) {
        final Element newElement = document.createElementNS(namespace, qName);

        final Iterator<Map.Entry<String, String>> iterator = namespaces.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, String> prefixToNamespace = iterator.next();

            // Ignore the default namespace as attribute since we are creating the element already with namespace
            if(!prefixToNamespace.getKey().isEmpty()) {
                final Attr attributeNS = document.createAttributeNS(XMLNS_URI, COLON_JOINER.join(XMLNS_ATTRIBUTE_KEY, prefixToNamespace.getKey()));
                attributeNS.setValue(prefixToNamespace.getValue());
                newElement.setAttributeNodeNS(attributeNS);
            }

            iterator.remove();
        }

        for (int i = 0; i < attrs.getLength(); i++) {
            if (Strings.isNullOrEmpty(attrs.getURI(i))) {
                newElement.setAttribute(attrs.getQName(i), attrs.getValue(i));
            } else {
                newElement.setAttributeNS(attrs.getURI(i), attrs.getQName(i), attrs.getValue(i));
            }
        }

        nodes.peek().appendChild(newElement);
        nodes.push(newElement);
    }

    @Override
    public void endElement(String namespace, String localName, String qName) {
        nodes.pop();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) {
        // namespaces are removed in start element
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    @Override
    public void processingInstruction(String target, String data) {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) {
    }

}

