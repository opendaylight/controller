/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.api;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Source of event has to implement this interface. In this context, source of
 * event is an object that is able to produce notifications or another message
 * about event. Event sources are managed by their own "event source manager".
 * Each event source should be represented as a node. If event source manager
 * want to announce object as an event source then it have to create an instance
 * that implements EventSource interface and register it via
 * {@link EventSourceRegistry#registerEventSource(Node, EventSource)()}.
 * Implementation of this interface contains implementation of EventSourceService.
 * To implement EventSourceService you have to implement the method JoinTopic.
 * Method JointTopic is called on registered EventSource when Topic is created
 * and EventSource can publish notification fulfill parameters of Topic.
 */

public interface EventSource extends EventSourceService, AutoCloseable {
    /**
     * Identifier of node that represents event source
     *
     * @return instance of NodeKey
     */
    NodeKey getSourceNodeKey();

    /**
     * List the types of notifications which source can produce.
     *
     * @return
     */
    List<SchemaPath> getAvailableNotifications();

}