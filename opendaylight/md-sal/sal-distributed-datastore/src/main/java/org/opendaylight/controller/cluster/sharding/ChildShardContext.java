/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.store.inmemory.WriteableDOMDataTreeShard;

final class ChildShardContext {
    private final WriteableDOMDataTreeShard shard;
    private final DOMDataTreeIdentifier prefix;

    ChildShardContext(final DOMDataTreeIdentifier prefix, final WriteableDOMDataTreeShard shard) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.shard = Preconditions.checkNotNull(shard);
    }

    WriteableDOMDataTreeShard getShard() {
        return shard;
    }

    DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }
}
