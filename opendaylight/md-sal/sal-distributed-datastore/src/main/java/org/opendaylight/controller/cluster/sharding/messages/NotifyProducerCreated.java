/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding.messages;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Collection;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Message sent to remote {@link ShardedDataTreeActor}'s when attempting
 * to create a producer. The remote node should attempt to create a producer in the local sharding service and reply
 * with success/failure based on the attempt result.
 */
@Beta
@Deprecated(forRemoval = true)
public class NotifyProducerCreated implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Collection<DOMDataTreeIdentifier> subtrees;

    public NotifyProducerCreated(final Collection<DOMDataTreeIdentifier> subtrees) {
        this.subtrees = ImmutableList.copyOf(subtrees);
    }

    public Collection<DOMDataTreeIdentifier> getSubtrees() {
        return subtrees;
    }
}
