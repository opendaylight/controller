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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public class XmlToNormalizedNodeReaderWithSchema {

    private final static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private XMLEventReader eventReader;
    private InstanceIdWithSchemaNode iiWithSchema;

    public XmlToNormalizedNodeReaderWithSchema(final InstanceIdWithSchemaNode iiWithSchema) {
        this.iiWithSchema = iiWithSchema;
    }

    public Node<?> read(InputStream entityStream) throws XMLStreamException, UnsupportedFormatException, IOException {
        // Get an XML stream which can be marked, and reset, so we can check and see if there is
        // any content being provided.
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

        final Stack<NodeWrapper<?>> processingQueue = new Stack<>();
        NodeWrapper<?> root = null;
        NodeWrapper<?> element = null;
        Stack<DataSchemaNode> processingQueueSchema = new Stack<>();

        while (eventReader.hasNext()) {
            final XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();
                CompositeNodeWrapper compParentNode = null;
                if (!processingQueue.isEmpty() && processingQueue.peek() instanceof CompositeNodeWrapper) {
                    compParentNode = (CompositeNodeWrapper) processingQueue.peek();
                    findSchemaNodeForElement(startElement, processingQueueSchema);
                } else {
                    processingQueueSchema = checkElementAndSchemaNodeNameAndNamespace(startElement,
                            iiWithSchema.getSchemaNode());
                    DataSchemaNode currentSchemaNode = processingQueueSchema.peek();
                    if (!(currentSchemaNode instanceof ListSchemaNode)
                            && !(currentSchemaNode instanceof ContainerSchemaNode)) {
                        throw new UnsupportedFormatException(
                                "Top level element has to be of type list or container schema node.");
                    }
                }

                NodeWrapper<?> newNode = null;
                if (isCompositeNodeEvent(event)) {
                    newNode = resolveCompositeNodeFromStartElement(processingQueueSchema.peek().getQName());
                    if (root == null) {
                        root = newNode;
                    }
                } else if (isSimpleNodeEvent(event)) {
                    newNode = resolveSimpleNodeFromStartElement(processingQueueSchema.peek(), getValueOf(startElement));
                    if (root == null) {
                        root = newNode;
                    }
                }

                if (newNode != null) {
                    processingQueue.push(newNode);
                    if (compParentNode != null) {
                        compParentNode.addValue(newNode);
                    }
                }
            } else if (event.isEndElement()) {
                element = processingQueue.pop();
//                if(((EndElement)event).getName().getLocalPart().equals
                processingQueueSchema.pop();
            }
        }

        if (!root.getLocalName().equals(element.getLocalName())) {
            throw new UnsupportedFormatException("XML should contain only one root element");
        }

        return root.unwrap();
    }

    private void findSchemaNodeForElement(StartElement element, Stack<DataSchemaNode> processingQueueSchema) {
        DataSchemaNode currentSchemaNode = processingQueueSchema.peek();
        if (currentSchemaNode instanceof DataNodeContainer) {
            final URI realNamespace = getNamespaceFor(element);
            final String realName = getLocalNameFor(element);
            Map<URI, DataSchemaNode> childNamesakes = resolveChildsWithNameAsElement(
                    ((DataNodeContainer) currentSchemaNode), realName);
            DataSchemaNode childDataSchemaNode = childNamesakes.get(realNamespace);
            if (childDataSchemaNode == null) {
                throw new RestconfDocumentedException("Element " + realName + " has namespace " + realNamespace
                        + ". Available namespaces are: " + childNamesakes.keySet(), ErrorType.APPLICATION,
                        ErrorTag.INVALID_VALUE);
            }
            processingQueueSchema.push(childDataSchemaNode);
        } else {
            throw new RestconfDocumentedException("Element " + processingQueueSchema.peek().getQName().getLocalName()
                    + " should be data node container .", ErrorType.APPLICATION, ErrorTag.INVALID_VALUE);
        }

    }

    /**
     * Returns map of data schema node which are accesible by URI which have equal name
     */
    private Map<URI, DataSchemaNode> resolveChildsWithNameAsElement(final DataNodeContainer dataNodeContainer,
            final String realName) {
        final Map<URI, DataSchemaNode> namespaceToDataSchemaNode = new HashMap<URI, DataSchemaNode>();
        for (DataSchemaNode dataSchemaNode : dataNodeContainer.getChildNodes()) {
            if (dataSchemaNode.equals(realName)) {
                namespaceToDataSchemaNode.put(dataSchemaNode.getQName().getNamespace(), dataSchemaNode);
            }
        }
        return namespaceToDataSchemaNode;
    }

    private final Stack<DataSchemaNode> checkElementAndSchemaNodeNameAndNamespace(final StartElement startElement,
            final DataSchemaNode node) {
        checkArgument(startElement != null, "Start Element cannot be NULL!");
        final String expectedName = node.getQName().getLocalName();
        final String xmlName = getLocalNameFor(startElement);
        final URI expectedNamespace = node.getQName().getNamespace();
        final URI xmlNamespace = getNamespaceFor(startElement);
        if (!expectedName.equals(xmlName)) {
            throw new RestconfDocumentedException("Xml element name: " + xmlName + "\nSchema node name: "
                    + expectedName, org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType.APPLICATION,
                    ErrorTag.INVALID_VALUE);
        }

        if (xmlNamespace != null && !expectedNamespace.equals(xmlNamespace)) {
            throw new RestconfDocumentedException("Xml element ns: " + xmlNamespace + "\nSchema node ns: "
                    + expectedNamespace,
                    org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType.APPLICATION,
                    ErrorTag.INVALID_VALUE);
        }
        Stack<DataSchemaNode> processingQueueSchema = new Stack<>();
        processingQueueSchema.push(node);
        return processingQueueSchema;
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

    private boolean isInputStreamEmpty(final InputStream entityStream) throws IOException {
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

    private CompositeNodeWrapper resolveCompositeNodeFromStartElement(final QName qName) {
        // checkArgument(startElement != null, "Start Element cannot be NULL!");
        CompositeNodeWrapper compositeNodeWrapper = new CompositeNodeWrapper("dummy");
        compositeNodeWrapper.setQname(qName);
        return compositeNodeWrapper;

    }

    private SimpleNodeWrapper resolveSimpleNodeFromStartElement(final DataSchemaNode node, final String value)
            throws XMLStreamException {
        // checkArgument(startElement != null, "Start Element cannot be NULL!");
        Object deserializedValue = null;

        if (node instanceof LeafSchemaNode) {
            TypeDefinition<?> baseType = RestUtil.resolveBaseTypeFrom(((LeafSchemaNode) node).getType());
            deserializedValue = RestCodec.from(baseType, iiWithSchema.getMountPoint()).deserialize(value);
        } else if (node instanceof LeafListSchemaNode) {
            TypeDefinition<?> baseType = RestUtil.resolveBaseTypeFrom(((LeafListSchemaNode) node).getType());
            deserializedValue = RestCodec.from(baseType, iiWithSchema.getMountPoint()).deserialize(value);
        }
        // String data;
        // if (data == null) {
        // return new EmptyNodeWrapper(getNamespaceFor(startElement), getLocalNameFor(startElement));
        // }
        SimpleNodeWrapper simpleNodeWrapper = new SimpleNodeWrapper("dummy", deserializedValue);
        simpleNodeWrapper.setQname(node.getQName());
        return simpleNodeWrapper;
    }

    private String getValueOf(final StartElement startElement) throws XMLStreamException {
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

    private String getAdditionalData(final XMLEvent event) throws XMLStreamException {
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

    private String getLocalNameFor(final StartElement startElement) {
        return startElement.getName().getLocalPart();
    }

    private URI getNamespaceFor(final StartElement startElement) {
        String namespaceURI = startElement.getName().getNamespaceURI();
        return namespaceURI.isEmpty() ? null : URI.create(namespaceURI);
    }

    private Object resolveValueOfElement(final String value, final StartElement startElement) {
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
