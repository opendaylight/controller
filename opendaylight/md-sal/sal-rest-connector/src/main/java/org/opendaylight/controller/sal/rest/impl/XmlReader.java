package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.InputStream;
import java.net.URI;
import java.util.Stack;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;

public class XmlReader {

    private final static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private XMLEventReader eventReader;

    public CompositeNodeWrapper read(InputStream entityStream) throws XMLStreamException, UnsupportedFormatException {
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

    private boolean isSimpleNodeEvent(final XMLEvent event) throws XMLStreamException {
        checkArgument(event != null, "XML Event cannot be NULL!");
        if (event.isStartElement()) {
            if (eventReader.hasNext()) {
                final XMLEvent innerEvent;
                innerEvent = eventReader.peek();
                if (innerEvent.isCharacters()) {
                    final Characters chars = innerEvent.asCharacters();
                    if (!chars.isWhiteSpace()) {
                        return true;
                    }
                } else if (innerEvent.isEndElement()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCompositeNodeEvent(final XMLEvent event) throws XMLStreamException {
        checkArgument(event != null, "XML Event cannot be NULL!");
        if (event.isStartElement()) {
            if (eventReader.hasNext()) {
                XMLEvent innerEvent;
                innerEvent = eventReader.peek();
                if (innerEvent.isCharacters()) {
                    Characters chars = innerEvent.asCharacters();
                    if (chars.isWhiteSpace()) {
                        eventReader.nextEvent();
                        innerEvent = eventReader.peek();
                    }
                }
                if (innerEvent.isStartElement()) {
                    return true;
                }
            }
        }
        return false;
    }

    private SimpleNodeWrapper resolveSimpleNodeFromStartElement(final StartElement startElement)
            throws XMLStreamException {
        checkArgument(startElement != null, "Start Element cannot be NULL!");
        String data = null;

        if (eventReader.hasNext()) {
            final XMLEvent innerEvent = eventReader.peek();
            if (innerEvent.isCharacters()) {
                final Characters chars = innerEvent.asCharacters();
                if (!chars.isWhiteSpace()) {
                    data = innerEvent.asCharacters().getData();
                }
            } else if (innerEvent.isEndElement()) {
                if (startElement.getLocation().getCharacterOffset() == innerEvent.getLocation().getCharacterOffset()) {
                    data = null;
                } else {
                    data = "";
                }
            }
        }

        return new SimpleNodeWrapper(getNamespaceFrom(startElement), getLocalNameFrom(startElement), data);
    }

    private CompositeNodeWrapper resolveCompositeNodeFromStartElement(final StartElement startElement) {
        checkArgument(startElement != null, "Start Element cannot be NULL!");
        return new CompositeNodeWrapper(getNamespaceFrom(startElement), getLocalNameFrom(startElement));
    }

    private String getLocalNameFrom(StartElement startElement) {
        return startElement.getName().getLocalPart();
    }

    private URI getNamespaceFrom(StartElement startElement) {
        String namespaceURI = startElement.getName().getNamespaceURI();
        return namespaceURI.isEmpty() ? null : URI.create(namespaceURI);
    }

}
