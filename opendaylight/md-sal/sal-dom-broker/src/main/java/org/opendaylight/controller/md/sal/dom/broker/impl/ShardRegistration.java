/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShard;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;

final class ShardRegistration<T extends DOMDataTreeShard> extends AbstractListenerRegistration<T> {
    private final DOMDataTreeIdentifier prefix;
    private final ShardedDOMDataTree tree;

    protected ShardRegistration(final ShardedDOMDataTree tree, final DOMDataTreeIdentifier prefix, final T shard) {
        super(shard);
        this.tree = Preconditions.checkNotNull(tree);
        this.prefix = Preconditions.checkNotNull(prefix);
    }

    DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    @Override
    protected void removeRegistration() {
        tree.removeShard(this);
    }
}
