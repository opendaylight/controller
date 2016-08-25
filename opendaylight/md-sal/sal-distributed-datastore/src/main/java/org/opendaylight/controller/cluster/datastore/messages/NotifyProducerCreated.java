/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import java.util.Collection;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Message sent to remote {@link org.opendaylight.controller.cluster.datastore.ShardedDataTreeActor}'s when attempting
 * to create a producer. The remote node should attempt to create a producer in the local sharding service and reply
 * with success/failure based on the attempt result.
 */
public class NotifyProducerCreated implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Collection<DOMDataTreeIdentifier> subtrees;

    public NotifyProducerCreated(final Collection<DOMDataTreeIdentifier> subtrees) {
        this.subtrees = subtrees;
    }

    public Collection<DOMDataTreeIdentifier> getSubtrees() {
        return subtrees;
    }
}
