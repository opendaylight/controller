/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceStatus;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceStatusNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceStatusNotificationBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ConnectionNotificationTopicRegistration extends NotificationTopicRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionNotificationTopicRegistration.class);

    public static final SchemaPath EVENT_SOURCE_STATUS_PATH = SchemaPath.create(true, QName.create(EventSourceStatusNotification.QNAME, "event-source-status"));
    private static final NodeIdentifier EVENT_SOURCE_STATUS_ARG = new NodeIdentifier(EventSourceStatusNotification.QNAME);
    private static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private final DOMNotificationListener domNotificationListener;
    private ConcurrentHashMap<SchemaPath, ArrayList<TopicId>> notificationTopicMap = new ConcurrentHashMap<>();

    public ConnectionNotificationTopicRegistration(String SourceName, DOMNotificationListener domNotificationListener) {
        super(NotificationSourceType.ConnectionStatusChange, SourceName, EVENT_SOURCE_STATUS_PATH.getLastComponent().getNamespace().toString());
        this.domNotificationListener = Preconditions.checkNotNull(domNotificationListener);
        LOG.info("Connection notification source has been initialized...");
        setActive(true);
        setReplaySupported(false);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Connection notification - publish Deactive");
        publishNotification(EventSourceStatus.Deactive);
        notificationTopicMap.clear();
        setActive(false);
    }

    @Override
    void activateNotificationSource() {
        LOG.info("Connection notification - publish Active");
        publishNotification(EventSourceStatus.Active);
    }

    @Override
    void deActivateNotificationSource() {
        LOG.info("Connection notification - publish Inactive");
        publishNotification(EventSourceStatus.Inactive);
    }

    @Override
    void reActivateNotificationSource() {
        LOG.info("Connection notification - reactivate - publish active");
        publishNotification(EventSourceStatus.Active);
    }

    @Override
    boolean registerNotificationTopic(SchemaPath notificationPath, TopicId topicId) {
        if(validateNotifactionSchemaPath(notificationPath) == false){
            LOG.debug("Bad SchemaPath for notification try to register");
            return false;
        }
        ArrayList<TopicId> topicIds = getNotificationTopicIds(notificationPath);
        if(topicIds == null){
            topicIds = new ArrayList<>();
            topicIds.add(topicId);
        } else {
            if(topicIds.contains(topicId) == false){
                topicIds.add(topicId);
            }
        }
        notificationTopicMap.put(notificationPath, topicIds);
        return true;
    }

    @Override
    ArrayList<TopicId> getNotificationTopicIds(SchemaPath notificationPath) {
        return notificationTopicMap.get(notificationPath);
    }

    @Override
    void unRegisterNotificationTopic(TopicId topicId) {
        // TODO: need code when EventAggregator.destroyTopic will be implemented
    }

    private boolean validateNotifactionSchemaPath(SchemaPath notificationPath){
        if(notificationPath == null){
            return false;
        }
        URI notificationNameSpace = notificationPath.getLastComponent().getNamespace();
        return getNotificationUrnPrefix().startsWith(notificationNameSpace.toString());
    }

    private void publishNotification(EventSourceStatus eventSourceStatus){

        final EventSourceStatusNotification notification = new EventSourceStatusNotificationBuilder()
                    .setStatus(eventSourceStatus)
                    .build();
        domNotificationListener.onNotification(createNotification(notification));
    }

    private DOMNotification createNotification(EventSourceStatusNotification notification){
        final ContainerNode cn = Builders.containerBuilder()
                .withNodeIdentifier(EVENT_SOURCE_STATUS_ARG)
                .withChild(encapsulate(notification))
                .build();
        DOMNotification dn = new DOMNotification() {

            @Override
            public SchemaPath getType() {
                return EVENT_SOURCE_STATUS_PATH;
            }

            @Override
            public ContainerNode getBody() {
                return cn;
            }
        };
        return dn;
    }

    private AnyXmlNode encapsulate(EventSourceStatusNotification notification){

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Can not create XML DocumentBuilder");
        }

        Document doc = docBuilder.newDocument();

        final Optional<String> namespace = Optional.of(EVENT_SOURCE_STATUS_ARG.getNodeType().getNamespace().toString());
        final Element rootElement = createElement(doc , "EventSourceStatusNotification", namespace);

        final Element sourceElement = doc.createElement("status");
        sourceElement.appendChild(doc.createTextNode(notification.getStatus().name()));
        rootElement.appendChild(sourceElement);


        return Builders.anyXmlBuilder().withNodeIdentifier(EVENT_SOURCE_STATUS_ARG)
                     .withValue(new DOMSource(rootElement))
                     .build();

    }

    // Helper to create root XML element with correct namespace and attribute
    private Element createElement(final Document document, final String qName, final Optional<String> namespaceURI) {
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
}
