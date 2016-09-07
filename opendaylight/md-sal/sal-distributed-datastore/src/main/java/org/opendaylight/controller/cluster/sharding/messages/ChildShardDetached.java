/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;

/**
 * Message sent to a local instance of a backend shard(if there is a local instance present) when a subshard with a more
 * specific prefix is getting removed from the system.
 */
public class ChildShardDetached {
    private final DOMDataTreeIdentifier prefix;
    private final DOMDataTreeShard subshard;

    public ChildShardDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard subshard) {
        this.prefix = prefix;
        this.subshard = subshard;
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    public DOMDataTreeShard getSubshard() {
        return subshard;
    }
}
