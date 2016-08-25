/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.util.Collection;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Message sent to local {@link org.opendaylight.controller.cluster.datastore.ShardedDataTreeActor}'s when there was an
 * attempt to create a producer on the local node.
 */
public class ProducerCreated {
    private final Collection<DOMDataTreeIdentifier> subtrees;

    public ProducerCreated(final Collection<DOMDataTreeIdentifier> subtrees) {
        this.subtrees = subtrees;
    }

    public Collection<DOMDataTreeIdentifier> getSubtrees() {
        return subtrees;
    }
}
