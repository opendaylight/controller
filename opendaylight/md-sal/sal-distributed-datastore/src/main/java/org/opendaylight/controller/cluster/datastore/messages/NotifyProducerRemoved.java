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
 * to close a producer. The remote node should attempt to close a producer in the local sharding service and reply
 * with success/failure based on the attempt result. If the producer doesn't exist on this node report Success.
 */
public class NotifyProducerRemoved implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Collection<DOMDataTreeIdentifier> subtrees;

    public NotifyProducerRemoved(final Collection<DOMDataTreeIdentifier> subtrees) {
        this.subtrees = subtrees;
    }

    public Collection<DOMDataTreeIdentifier> getSubtrees() {
        return subtrees;
    }
}
