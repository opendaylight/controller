/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.util;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class TopicDOMNotification implements DOMNotification {

    private static final SchemaPath TOPIC_NOTIFICATION_ID = SchemaPath.create(true, TopicNotification.QNAME);
    private final ContainerNode body;

    public TopicDOMNotification(final ContainerNode body) {
        this.body = body;
    }

    @Override
    public SchemaPath getType() {
        return TOPIC_NOTIFICATION_ID;
    }

    @Override
    public ContainerNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "TopicDOMNotification [body=" + body + "]";
    }
}
