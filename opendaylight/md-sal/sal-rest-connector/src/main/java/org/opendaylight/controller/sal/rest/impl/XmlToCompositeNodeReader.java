/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.EmptyNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.data.api.Node;

public class XmlToCompositeNodeReader {

    private final static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private XMLEventReader eventReader;

    public CompositeNodeWrapper read(InputStream entityStream) throws XMLStreamException, UnsupportedFormatException,
            IOException {
        // Get an XML stream which can be marked, and reset, so we can check and
        // see if there is any content being provided.
        entityStream = getMarkableStream(entityStream);

        if (isInputStreamEmpty(entityStream)) {
            return null;
        }

        eventReader = xmlInputFactory.createXMLEventReader(entityStream);

        if (eventReader.hasNext()) {
            XMLEvent element = eventReader.peek();
            if (element.isStartDocument()) {
                eventReader.nextEvent();
            }
        }

        if (eventReader.hasNext() && !isCompositeNodeEvent(eventReader.peek())) {
            throw new UnsupportedFormatException("Root element of XML has to be composite element.");
        }

        final Stack<NodeWrapper<?>> processingQueue = new Stack<>();
        CompositeNodeWrapper root = null;
        NodeWrapper<?> element = null;
        while (eventReader.hasNext()) {
            final XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();
                CompositeNodeWrapper compParentNode = null;
                if (!processingQueue.isEmpty() && processingQueue.peek() instanceof CompositeNodeWrapper) {
                    compParentNode = (CompositeNodeWrapper) processingQueue.peek();
                }
                NodeWrapper<?> newNode = null;
                if (isCompositeNodeEvent(event)) {
                    if (root == null) {
                        root = resolveCompositeNodeFromStartElement(startElement);
                        newNode = root;
                    } else {
                        newNode = resolveCompositeNodeFromStartElement(startElement);
                    }
                } else if (isSimpleNodeEvent(event)) {
                    if (root == null) {
                        throw new UnsupportedFormatException("Root element of XML has to be composite element.");
                    }
                    newNode = resolveSimpleNodeFromStartElement(startElement);
                }

                if (newNode != null) {
                    processingQueue.push(newNode);
                    if (compParentNode != null) {
                        compParentNode.addValue(newNode);
                    }
                }
            } else if (event.isEndElement()) {
                element = processingQueue.pop();
            }
        }

        if (!root.getLocalName().equals(element.getLocalName())) {
            throw new UnsupportedFormatException("XML should contain only one root element");
        }

        return root;
    }

    /**
     * If the input stream is not markable, then it wraps the input stream with a buffered stream, which is mark able.
     * That way we can check if the stream is empty safely.
     *
     * @param entityStream
     * @return
     */
    private InputStream getMarkableStream(InputStream entityStream) {
        if (!entityStream.markSupported()) {
            entityStream = new BufferedInputStream(entityStream);
        }
        return entityStream;
    }

    private boolean isInputStreamEmpty(InputStream entityStream) throws IOException {
        boolean isEmpty = false;
        entityStream.mark(1);
        if (entityStream.read() == -1) {
            isEmpty = true;
        }
        entityStream.reset();
        return isEmpty;
    }

    private boolean isSimpleNodeEvent(final XMLEvent event) throws XMLStreamException {
        checkArgument(event != null, "XML Event cannot be NULL!");
        if (event.isStartElement()) {
            XMLEvent innerEvent = skipCommentsAndWhitespace();
            if (innerEvent != null && (innerEvent.isCharacters() || innerEvent.isEndElement())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompositeNodeEvent(final XMLEvent event) throws XMLStreamException {
        checkArgument(event != null, "XML Event cannot be NULL!");
        if (event.isStartElement()) {
            XMLEvent innerEvent = skipCommentsAndWhitespace();
            if (innerEvent != null) {
                if (innerEvent.isStartElement()) {
                    return true;
                }
            }
        }
        return false;
    }

    private XMLEvent skipCommentsAndWhitespace() throws XMLStreamException {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.peek();
            if (event.getEventType() == XMLStreamConstants.COMMENT) {
                eventReader.nextEvent();
                continue;
            }

            if (event.isCharacters()) {
                Characters chars = event.asCharacters();
                if (chars.isWhiteSpace()) {
                    eventReader.nextEvent();
                    continue;
                }
            }
            return event;
        }
        return null;
    }

    private CompositeNodeWrapper resolveCompositeNodeFromStartElement(final StartElement startElement) {
        checkArgument(startElement != null, "Start Element cannot be NULL!");
        return new CompositeNodeWrapper(getNamespaceFor(startElement), getLocalNameFor(startElement));
    }

    private NodeWrapper<? extends Node<?>> resolveSimpleNodeFromStartElement(final StartElement startElement)
            throws XMLStreamException {
        checkArgument(startElement != null, "Start Element cannot be NULL!");
        String data = getValueOf(startElement);
        if (data == null) {
            return new EmptyNodeWrapper(getNamespaceFor(startElement), getLocalNameFor(startElement));
        }
        return new SimpleNodeWrapper(getNamespaceFor(startElement), getLocalNameFor(startElement),
                resolveValueOfElement(data, startElement));
    }

    private String getValueOf(StartElement startElement) throws XMLStreamException {
        String data = null;
        if (eventReader.hasNext()) {
            final XMLEvent innerEvent = eventReader.peek();
            if (innerEvent.isCharacters()) {
                final Characters chars = innerEvent.asCharacters();
                if (!chars.isWhiteSpace()) {
                    data = innerEvent.asCharacters().getData();
                    data = data + getAdditionalData(eventReader.nextEvent());
                }
            } else if (innerEvent.isEndElement()) {
                if (startElement.getLocation().getCharacterOffset() == innerEvent.getLocation().getCharacterOffset()) {
                    data = null;
                } else {
                    data = "";
                }
            }
        }
        return data == null ? null : data.trim();
    }

    private String getAdditionalData(XMLEvent event) throws XMLStreamException {
        String data = "";
        if (eventReader.hasNext()) {
            final XMLEvent innerEvent = eventReader.peek();
            if (innerEvent.isCharacters() && !innerEvent.isEndElement()) {
                final Characters chars = innerEvent.asCharacters();
                if (!chars.isWhiteSpace()) {
                    data = innerEvent.asCharacters().getData();
                    data = data + getAdditionalData(eventReader.nextEvent());
                }
            }
        }
        return data;
    }

    private String getLocalNameFor(StartElement startElement) {
        return startElement.getName().getLocalPart();
    }

    private URI getNamespaceFor(StartElement startElement) {
        String namespaceURI = startElement.getName().getNamespaceURI();
        return namespaceURI.isEmpty() ? null : URI.create(namespaceURI);
    }

    private Object resolveValueOfElement(String value, StartElement startElement) {
        // it could be instance-identifier Built-In Type
        if (value.startsWith("/")) {
            IdentityValuesDTO iiValue = RestUtil.asInstanceIdentifier(value, new RestUtil.PrefixMapingFromXml(
                    startElement));
            if (iiValue != null) {
                return iiValue;
            }
        }
        // it could be identityref Built-In Type
        String[] namespaceAndValue = value.split(":");
        if (namespaceAndValue.length == 2) {
            String namespace = startElement.getNamespaceContext().getNamespaceURI(namespaceAndValue[0]);
            if (namespace != null && !namespace.isEmpty()) {
                return new IdentityValuesDTO(namespace, namespaceAndValue[1], namespaceAndValue[0], value);
            }
        }
        // it is not "prefix:value" but just "value"
        return value;
    }

}
