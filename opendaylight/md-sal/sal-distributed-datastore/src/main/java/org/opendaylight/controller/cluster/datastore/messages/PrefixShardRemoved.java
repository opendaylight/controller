/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Message sent to remote {@link org.opendaylight.controller.cluster.datastore.ShardedDataTreeActor}'s when there is an
 * attempt to remove a shard from the sharding service.
 */
public class PrefixShardRemoved implements Serializable {
    private static final long serialVersionUID = 1L;

    private final DOMDataTreeIdentifier prefix;

    public PrefixShardRemoved(final DOMDataTreeIdentifier prefix) {
        this.prefix = prefix;
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }
}
