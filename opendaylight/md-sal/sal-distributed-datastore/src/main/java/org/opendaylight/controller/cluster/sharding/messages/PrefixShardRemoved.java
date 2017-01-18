/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import com.google.common.annotations.Beta;
import java.io.Serializable;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Message sent to remote {@link ShardedDataTreeActor}'s when there is an attempt to remove the shard,
 * the ShardedDataTreeActor should remove the shard from the current configuration so that the change is picked up
 * in the backend ShardManager.
 */
@Beta
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
