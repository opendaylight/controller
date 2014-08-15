/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.cached.store.impl;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

public class NormalizedNodeCache {
    //
    //TODO: make cache configurable, maybe use object cache for this.
    private Cache<NormalizedNode<?, ?>, NormalizedNode<?, ?>> normalizedNodeCache =
            CacheBuilder.newBuilder().weakValues().build();

    public Optional<? extends NormalizedNode<?, ?>> getReference(NormalizedNode<?, ?> normalizedNode) {
        return Optional.fromNullable(normalizedNodeCache.getIfPresent(normalizedNode));
    }

    // if whole normalized node tree reference cannot be shared, walk children and try to find subtree that can be
    public Optional<? extends NormalizedNode<?, ?>> getReferenceWithChildNodes(NormalizedNode<?, ?> normalizedNode) {
        NormalizedNode<?, ?> foundReference = normalizedNodeCache.getIfPresent(normalizedNode);
        if (foundReference == null && normalizedNode instanceof NormalizedNodeContainer) {
            NormalizedNodeContainer containerNode = (NormalizedNodeContainer) normalizedNode;
            for (Object child : containerNode.getValue()) {
                getReferenceWithChildNodes((NormalizedNode) child);
            }
        }
        return Optional.fromNullable(normalizedNodeCache.getIfPresent(normalizedNode));
    }

    public void putObject(NormalizedNode<?, ?> normalizedNode) {
        normalizedNodeCache.put(normalizedNode, normalizedNode);
    }

    // put children inside cache
    public void putObjectWithChildNodes(NormalizedNode<?, ?> normalizedNode) {
        normalizedNodeCache.put(normalizedNode, normalizedNode);
        if (normalizedNode instanceof NormalizedNodeContainer) {
            NormalizedNodeContainer containerNode = (NormalizedNodeContainer) normalizedNode;
            for (Object child : containerNode.getValue()) {
                putObjectWithChildNodes((NormalizedNode) child);
            }
        }
    }
}
