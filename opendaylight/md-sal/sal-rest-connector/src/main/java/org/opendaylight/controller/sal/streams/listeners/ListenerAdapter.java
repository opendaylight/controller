/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.streams.listeners;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.ConcurrentSet;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * {@link ListenerAdapter} is responsible to track events, which occurred by changing data in data source.
 */
public class ListenerAdapter implements DOMDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerAdapter.class);
    private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory FACTORY = TransformerFactory.newInstance();
    private static final Pattern RFC3339_PATTERN = Pattern.compile("(\\d\\d)(\\d\\d)$");

    private final XmlMapper xmlMapper = new XmlMapper();
    private final SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    private final YangInstanceIdentifier path;
    private ListenerRegistration<DOMDataChangeListener> registration;
    private final String streamName;
    private Set<Channel> subscribers = new ConcurrentSet<>();
    private final EventBus eventBus;
    private final EventBusChangeRecorder eventBusChangeRecorder;

    /**
     * Creates new {@link ListenerAdapter} listener specified by path and stream name.
     *
     * @param path
     *            Path to data in data store.
     * @param streamName
     *            The name of the stream.
     */
    ListenerAdapter(final YangInstanceIdentifier path, final String streamName) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(streamName != null && !streamName.isEmpty());
        this.path = path;
        this.streamName = streamName;
        eventBus = new AsyncEventBus(Executors.newSingleThreadExecutor());
        eventBusChangeRecorder = new EventBusChangeRecorder();
        eventBus.register(eventBusChangeRecorder);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        // TODO Auto-generated method stub

        if (!change.getCreatedData().isEmpty() || !change.getUpdatedData().isEmpty()
                || !change.getRemovedPaths().isEmpty()) {
            final String xml = prepareXmlFrom(change);
            final Event event = new Event(EventType.NOTIFY);
            event.setData(xml);
            eventBus.post(event);
        }
    }

    /**
     * Tracks events of data change by customer.
     */
    private final class EventBusChangeRecorder {
        @Subscribe
        public void recordCustomerChange(final Event event) {
            if (event.getType() == EventType.REGISTER) {
                final Channel subscriber = event.getSubscriber();
                if (!subscribers.contains(subscriber)) {
                    subscribers.add(subscriber);
                }
            } else if (event.getType() == EventType.DEREGISTER) {
                subscribers.remove(event.getSubscriber());
                Notificator.removeListenerIfNoSubscriberExists(ListenerAdapter.this);
            } else if (event.getType() == EventType.NOTIFY) {
                for (final Channel subscriber : subscribers) {
                    if (subscriber.isActive()) {
                        LOG.debug("Data are sent to subscriber {}:", subscriber.remoteAddress());
                        subscriber.writeAndFlush(new TextWebSocketFrame(event.getData()));
                    } else {
                        LOG.debug("Subscriber {} is removed - channel is not active yet.", subscriber.remoteAddress());
                        subscribers.remove(subscriber);
                    }
                }
            }
        }
    }

    /**
     * Represents event of specific {@link EventType} type, holds data and {@link Channel} subscriber.
     */
    private final class Event {
        private final EventType type;
        private Channel subscriber;
        private String data;

        /**
         * Creates new event specified by {@link EventType} type.
         *
         * @param type
         *            EventType
         */
        public Event(final EventType type) {
            this.type = type;
        }

        /**
         * Gets the {@link Channel} subscriber.
         *
         * @return Channel
         */
        public Channel getSubscriber() {
            return subscriber;
        }

        /**
         * Sets subscriber for event.
         *
         * @param subscriber
         *            Channel
         */
        public void setSubscriber(final Channel subscriber) {
            this.subscriber = subscriber;
        }

        /**
         * Gets event data.
         *
         * @return String representation of event data.
         */
        public String getData() {
            return data;
        }

        /**
         * Sets event data.
         *
         * @param data
         *            data.
         */
        public void setData(final String data) {
            this.data = data;
        }

        /**
         * Gets event type.
         *
         * @return The type of the event.
         */
        public EventType getType() {
            return type;
        }
    }

    /**
     * Type of the event.
     */
    private enum EventType {
        REGISTER,
        DEREGISTER,
        NOTIFY;
    }

    /**
     * Prepare data in printable form and transform it to String.
     *
     * @param change
     *            DataChangeEvent
     * @return Data in printable form.
     */
    private String prepareXmlFrom(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        final Document doc = createDocument();
        final Element notificationElement = doc.createElementNS("urn:ietf:params:xml:ns:netconf:notification:1.0",
                "notification");
        doc.appendChild(notificationElement);

        final Element eventTimeElement = doc.createElement("eventTime");
        eventTimeElement.setTextContent(toRFC3339(new Date()));
        notificationElement.appendChild(eventTimeElement);

        final Element dataChangedNotificationEventElement = doc.createElementNS(
                "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote", "data-changed-notification");
        addValuesToDataChangedNotificationEventElement(doc, dataChangedNotificationEventElement, change);
        notificationElement.appendChild(dataChangedNotificationEventElement);

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final Transformer transformer = FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, Charsets.UTF_8)));
            final byte[] charData = out.toByteArray();
            return new String(charData, "UTF-8");
        } catch (TransformerException | UnsupportedEncodingException e) {
            final String msg = "Error during transformation of Document into String";
            LOG.error(msg, e);
            return msg;
        }
    }

    /**
     * Formats data specified by RFC3339.
     *
     * @param d
     *            Date
     * @return Data specified by RFC3339.
     */
    private String toRFC3339(final Date d) {
        return RFC3339_PATTERN.matcher(rfc3339.format(d)).replaceAll("$1:$2");
    }

    /**
     * Creates {@link Document} document.
     * @return {@link Document} document.
     */
    private Document createDocument() {
        final DocumentBuilder bob;
        try {
            bob = DBF.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            return null;
        }
        return bob.newDocument();
    }

    /**
     * Adds values to data changed notification event element.
     *
     * @param doc
     *            {@link Document}
     * @param dataChangedNotificationEventElement
     *            {@link Element}
     * @param change
     *            {@link org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode}
     */
    private void addValuesToDataChangedNotificationEventElement(final Document doc,
            final Element dataChangedNotificationEventElement,
            final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        addValuesFromDataToElement(doc, change.getCreatedData().keySet(), dataChangedNotificationEventElement,
                Operation.CREATED);
        if (change.getCreatedData().isEmpty()) {
            addValuesFromDataToElement(doc, change.getUpdatedData().keySet(), dataChangedNotificationEventElement,
                    Operation.UPDATED);
        }
        addValuesFromDataToElement(doc, change.getRemovedPaths(), dataChangedNotificationEventElement,
                Operation.DELETED);
    }

    /**
     * Adds values from data to element.
     *
     * @param doc
     *            {@link Document}
     * @param data
     *            Set of {@link YangInstanceIdentifier}.
     * @param element
     *            {@link Element}
     * @param operation
     *            {@link Operation}
     */
    private void addValuesFromDataToElement(final Document doc, final Set<YangInstanceIdentifier> data, final Element element,
            final Operation operation) {
        if (data == null || data.isEmpty()) {
            return;
        }
        for (final YangInstanceIdentifier path : data) {
            if (!ControllerContext.getInstance().isNodeMixin(path)) {
                Node node = createDataChangeEventElement(doc, path, operation);
                element.appendChild(node);
            }
        }
    }


    /**
     * Creates changed event element from data.
     *
     * @param doc
     *            {@link Document}
     * @param path
     *            Path to data in data store.
     * @param operation
     *            {@link Operation}
     * @return {@link Node} node represented by changed event element.
     */
    private Node createDataChangeEventElement(final Document doc, final YangInstanceIdentifier path, final Operation operation) {
        final Element dataChangeEventElement = doc.createElement("data-change-event");
        final Element pathElement = doc.createElement("path");
        addPathAsValueToElement(path, pathElement);
        dataChangeEventElement.appendChild(pathElement);

        final Element operationElement = doc.createElement("operation");
        operationElement.setTextContent(operation.value);
        dataChangeEventElement.appendChild(operationElement);

        return dataChangeEventElement;
    }


    /**
     * Adds path as value to element.
     *
     * @param path
     *            Path to data in data store.
     * @param element
     *            {@link Element}
     */
    private void addPathAsValueToElement(final YangInstanceIdentifier path, final Element element) {
        // Map< key = namespace, value = prefix>
        final Map<String, String> prefixes = new HashMap<>();
        final YangInstanceIdentifier normalizedPath = ControllerContext.getInstance().toXpathRepresentation(path);
        final StringBuilder textContent = new StringBuilder();

        // FIXME: BUG-1281: this is duplicated code from yangtools (BUG-1275)
        for (final PathArgument pathArgument : normalizedPath.getPathArguments()) {
            if (pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier) {
                continue;
            }
            textContent.append("/");
            writeIdentifierWithNamespacePrefix(element, textContent, pathArgument.getNodeType(), prefixes);
            if (pathArgument instanceof NodeIdentifierWithPredicates) {
                final Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) pathArgument).getKeyValues();
                for (final QName keyValue : predicates.keySet()) {
                    final String predicateValue = String.valueOf(predicates.get(keyValue));
                    textContent.append("[");
                    writeIdentifierWithNamespacePrefix(element, textContent, keyValue, prefixes);
                    textContent.append("='");
                    textContent.append(predicateValue);
                    textContent.append("'");
                    textContent.append("]");
                }
            } else if (pathArgument instanceof NodeWithValue) {
                textContent.append("[.='");
                textContent.append(((NodeWithValue) pathArgument).getValue());
                textContent.append("'");
                textContent.append("]");
            }
        }
        element.setTextContent(textContent.toString());
    }

    /**
     * Writes identifier that consists of prefix and QName.
     *
     * @param element
     *            {@link Element}
     * @param textContent
     *            StringBuilder
     * @param qName
     *            QName
     * @param prefixes
     *            Map of namespaces and prefixes.
     */
    private static void writeIdentifierWithNamespacePrefix(final Element element, final StringBuilder textContent,
            final QName qName, final Map<String, String> prefixes) {
        final String namespace = qName.getNamespace().toString();
        String prefix = prefixes.get(namespace);
        if (prefix == null) {
            prefix = generateNewPrefix(prefixes.values());
        }

        element.setAttribute("xmlns:" + prefix, namespace);
        textContent.append(prefix);
        prefixes.put(namespace, prefix);

        textContent.append(":");
        textContent.append(qName.getLocalName());
    }

    /**
     * Generates new prefix which consists of four random characters <a-z>.
     *
     * @param prefixes
     *            Collection of prefixes.
     * @return New prefix which consists of four random characters <a-z>.
     */
    private static String generateNewPrefix(final Collection<String> prefixes) {
        StringBuilder result = null;
        final Random random = new Random();
        do {
            result = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                final int randomNumber = 0x61 + (Math.abs(random.nextInt()) % 26);
                result.append(Character.toChars(randomNumber));
            }
        } while (prefixes.contains(result.toString()));

        return result.toString();
    }

    /**
     * Gets path pointed to data in data store.
     *
     * @return Path pointed to data in data store.
     */
    public YangInstanceIdentifier getPath() {
        return path;
    }

    /**
     * Sets {@link ListenerRegistration} registration.
     *
     * @param registration
     *            ListenerRegistration<DataChangeListener>
     */
    public void setRegistration(final ListenerRegistration<DOMDataChangeListener> registration) {
        this.registration = registration;
    }

    /**
     * Gets the name of the stream.
     *
     * @return The name of the stream.
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Removes all subscribers and unregisters event bus change recorder form event bus.
     */
    public void close() throws Exception {
        subscribers = new ConcurrentSet<>();
        registration.close();
        registration = null;
        eventBus.unregister(eventBusChangeRecorder);
    }

    /**
     * Checks if {@link ListenerRegistration} registration exist.
     *
     * @return True if exist, false otherwise.
     */
    public boolean isListening() {
        return registration == null ? false : true;
    }

    /**
     * Creates event of type {@link EventType#REGISTER}, set {@link Channel} subscriber to the event and post event into
     * event bus.
     *
     * @param subscriber
     *            Channel
     */
    public void addSubscriber(final Channel subscriber) {
        if (!subscriber.isActive()) {
            LOG.debug("Channel is not active between websocket server and subscriber {}" + subscriber.remoteAddress());
        }
        final Event event = new Event(EventType.REGISTER);
        event.setSubscriber(subscriber);
        eventBus.post(event);
    }

    /**
     * Creates event of type {@link EventType#DEREGISTER}, sets {@link Channel} subscriber to the event and posts event
     * into event bus.
     *
     * @param subscriber
     */
    public void removeSubscriber(final Channel subscriber) {
        LOG.debug("Subscriber {} is removed.", subscriber.remoteAddress());
        final Event event = new Event(EventType.DEREGISTER);
        event.setSubscriber(subscriber);
        eventBus.post(event);
    }

    /**
     * Checks if exists at least one {@link Channel} subscriber.
     *
     * @return True if exist at least one {@link Channel} subscriber, false otherwise.
     */
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    /**
     * Consists of two types {@link Store#CONFIG} and {@link Store#OPERATION}.
     */
    private static enum Store {
        CONFIG("config"),
        OPERATION("operation");

        private final String value;

        private Store(final String value) {
            this.value = value;
        }
    }

    /**
     * Consists of three types {@link Operation#CREATED}, {@link Operation#UPDATED} and {@link Operation#DELETED}.
     */
    private static enum Operation {
        CREATED("created"),
        UPDATED("updated"),
        DELETED("deleted");

        private final String value;

        private Operation(final String value) {
            this.value = value;
        }
    }

}
