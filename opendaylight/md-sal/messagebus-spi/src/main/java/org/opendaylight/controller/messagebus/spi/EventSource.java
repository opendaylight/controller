/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.spi;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Event source is a node in topology which is able to produce notifications.
 * To register event source you use {@link EventSourceRegistry#registerEventSource(EventSource)}.
 * EventSourceRegistry will request registered event source to publish notifications
 * whenever EventSourceRegistry has been asked to publish a certain type of notifications.
 * EventSourceRegistry will call method JoinTopic to request EventSource to publish notification.
 * Event source must implement method JoinTopic (from superinterface {@link EventSourceService}).
 */
@Deprecated(forRemoval = true)
public interface EventSource extends EventSourceService, AutoCloseable {
    /**
     * Identifier of node associated with event source.
     *
     * @return instance of NodeKey
     */
    NodeKey getSourceNodeKey();

    /**
     * List the types of notifications which source can produce.
     *
     * @return list of available notification
     */
    List<SchemaPath> getAvailableNotifications();
}
