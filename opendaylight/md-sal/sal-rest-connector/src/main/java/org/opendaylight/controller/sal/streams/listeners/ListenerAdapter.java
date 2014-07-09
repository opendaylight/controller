/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.streams.listeners;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.ConcurrentSet;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlStreamUtils;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ListenerAdapter} is responsible to track events, which occurred by changing data in data source.
 */
public class ListenerAdapter implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerAdapter.class);
    private static final Pattern RFC3339_PATTERN = Pattern.compile("(\\d\\d)(\\d\\d)$");

    private final XmlMapper xmlMapper = new XmlMapper();
    private final SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    private final InstanceIdentifier path;
    private ListenerRegistration<DataChangeListener> registration;
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
    ListenerAdapter(final InstanceIdentifier path, final String streamName) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(streamName != null && !streamName.isEmpty());
        this.path = path;
        this.streamName = streamName;
        eventBus = new AsyncEventBus(Executors.newSingleThreadExecutor());
        eventBusChangeRecorder = new EventBusChangeRecorder();
        eventBus.register(eventBusChangeRecorder);
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
        if (!change.getCreatedConfigurationData().isEmpty() || !change.getCreatedOperationalData().isEmpty()
                || !change.getUpdatedConfigurationData().isEmpty() || !change.getUpdatedOperationalData().isEmpty()
                || !change.getRemovedConfigurationData().isEmpty() || !change.getRemovedOperationalData().isEmpty()) {
            String xml = prepareXmlFrom(change);
            Event event = new Event(EventType.NOTIFY);
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
                Channel subscriber = event.getSubscriber();
                if (!subscribers.contains(subscriber)) {
                    subscribers.add(subscriber);
                }
            } else if (event.getType() == EventType.DEREGISTER) {
                subscribers.remove(event.getSubscriber());
                Notificator.removeListenerIfNoSubscriberExists(ListenerAdapter.this);
            } else if (event.getType() == EventType.NOTIFY) {
                for (Channel subscriber : subscribers) {
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
         * @param String
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
    private String prepareXmlFrom(final DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
        try {
            final ByteArrayOutputStream s = new ByteArrayOutputStream();
            final XMLStreamWriter w = XMLOutputFactory.newFactory().createXMLStreamWriter(s);
            w.writeStartDocument();
            w.writeStartElement("urn:ietf:params:xml:ns:netconf:notification:1.0", "notification");

            w.writeStartElement("eventTime");
            w.writeCharacters(toRFC3339(new Date()));
            w.writeEndElement();

            w.writeStartElement("urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote", "data-changed-notification");
            writeDataChangedNotification(w, change);
            w.writeEndElement();

            w.writeEndElement();
            w.writeEndDocument();
            w.close();

            return s.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            LOG.error("Failed to create XML", e);
            return null;
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
     * Adds values to data changed notification event element.
     *
     * @param writer XML stream writer
     * @param change {@link DataChangeEvent}
     * @throws XMLStreamException
     */
    private void writeDataChangedNotification(final XMLStreamWriter writer, final DataChangeEvent<InstanceIdentifier, CompositeNode> change) throws XMLStreamException {
        addValuesFromDataToElement(writer, change.getCreatedConfigurationData(),
                Store.CONFIG, Operation.CREATED);
        addValuesFromDataToElement(writer, change.getCreatedOperationalData(),
                Store.OPERATION, Operation.CREATED);
        if (change.getCreatedConfigurationData().isEmpty()) {
            addValuesFromDataToElement(writer,
                    change.getUpdatedConfigurationData(),
                    Store.CONFIG, Operation.UPDATED);
        }
        if (change.getCreatedOperationalData().isEmpty()) {
            addValuesFromDataToElement(writer, change.getUpdatedOperationalData(),
                    Store.OPERATION, Operation.UPDATED);
        }
        addValuesFromDataToElement(writer, change.getRemovedConfigurationData(),
                Store.CONFIG, Operation.DELETED);
        addValuesFromDataToElement(writer, change.getRemovedOperationalData(),
                Store.OPERATION, Operation.DELETED);
    }

    /**
     * Adds values from data to element.
     *
     * @param writer
     *            {@link XMLStreamWriter}
     * @param data
     *            Set of {@link InstanceIdentifier}.
     * @param store
     *            {@link Store}
     * @param operation
     *            {@link Operation}
     * @throws XMLStreamException
     */
    private void addValuesFromDataToElement(final XMLStreamWriter writer,
            final Set<InstanceIdentifier> data, final Store store,
            final Operation operation) throws XMLStreamException {
        if (data != null) {
            for (InstanceIdentifier path : data) {
                createDataChangeEventElement(writer, path, null, store,
                        operation);
            }
        }
    }

    /**
     * Adds values from data to element.
     *
     * @param writer
     *            {@link XMLStreamWriter}
     * @param data
     *            Map of {@link InstanceIdentifier} and {@link CompositeNode}.
     * @param store
     *            {@link Store}
     * @param operation
     *            {@link Operation}
     * @throws XMLStreamException
     */
    private void addValuesFromDataToElement(final XMLStreamWriter writer,
            final Map<InstanceIdentifier, CompositeNode> data,
            final Store store, final Operation operation) throws XMLStreamException {
        if (data != null) {
            for (Entry<InstanceIdentifier, CompositeNode> entry : data.entrySet()) {
                createDataChangeEventElement(writer, entry.getKey(),
                        entry.getValue(), store, operation);
            }
        }
    }

    /**
     * Creates changed event element from data.
     *
     * @param writer
     *            {@link XMLStreamWriter}
     * @param path
     *            Path to data in data store.
     * @param data
     *            {@link CompositeNode}
     * @param store
     *            {@link Store}
     * @param operation
     *            {@link Operation}
     * @throws XMLStreamException
     */
    private void createDataChangeEventElement(final XMLStreamWriter writer,
            final InstanceIdentifier path, final CompositeNode data, final Store store,
            final Operation operation) throws XMLStreamException {
        writer.writeStartElement("data-change-event");

        writer.writeStartElement("path");
        XmlStreamUtils.write(writer, path);
        writer.writeEndElement();

        writer.writeStartElement("store");
        writer.writeCharacters(store.value);
        writer.writeEndElement();

        writer.writeStartElement("operation");
        writer.writeCharacters(operation.value);
        writer.writeEndElement();

        if (data != null) {
            writer.writeStartElement("data");
            translateToXml(writer, path, data);
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    /**
     * Translates {@link CompositeNode} data to XML format.
     *
     * @param path
     *            Path to data in data store.
     * @param data
     *            {@link CompositeNode}
     * @return Data in XML format.
     * @throws XMLStreamException
     */
    private void translateToXml(final XMLStreamWriter writer, final InstanceIdentifier path, final CompositeNode data) throws XMLStreamException {
        DataNodeContainer schemaNode = ControllerContext.getInstance()
                .getDataNodeContainerFor(path);
        if (schemaNode != null) {
            xmlMapper.writeData(writer, data, schemaNode);
        } else {
            LOG.info(
                    "Path '{}' contains node with unsupported type (supported type is Container or List) or some node was not found.",
                    path);
        }
    }

    /**
     * Gets path pointed to data in data store.
     *
     * @return Path pointed to data in data store.
     */
    public InstanceIdentifier getPath() {
        return path;
    }

    /**
     * Sets {@link ListenerRegistration} registration.
     *
     * @param registration
     *            ListenerRegistration<DataChangeListener>
     */
    public void setRegistration(final ListenerRegistration<DataChangeListener> registration) {
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
        Event event = new Event(EventType.REGISTER);
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
        Event event = new Event(EventType.DEREGISTER);
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
