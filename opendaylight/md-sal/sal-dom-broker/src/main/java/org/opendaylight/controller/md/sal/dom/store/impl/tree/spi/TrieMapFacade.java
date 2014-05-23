/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingMap;
import com.romix.scala.collection.concurrent.TrieMap;

/**
 * A read-only facade in front of a TrieMap. This is what we give out from
 * MapAdaptor.optimize(). The idea is that we want our read-only users to
 * share a single snapshot. That snapshot is instantiated lazily either on
 * first access. Since we never leak the TrieMap and track its size as it
 * changes, we can cache it for future reference.
 */
final class TrieMapFacade<K, V> extends ForwardingMap<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(TrieMapFacade.class);
    private final TrieMap<K, V> readWrite;
    private final int size;
    private TrieMap<K, V> readOnly;

    TrieMapFacade(final TrieMap<K, V> map, final int size) {
        super();
        this.readWrite = Preconditions.checkNotNull(map);
        this.size = size;
    }

    Map<K, V> toReadWrite() {
        final Map<K, V> ret = new TrackingTrieMapFacade<>(readWrite.snapshot(), size);
        LOG.trace("Converted read-only TrieMap {} to read-write {}", this, ret);
        return ret;
    }

    @Override
    protected Map<K, V> delegate() {
        if (readOnly == null) {
            synchronized (this) {
                if (readOnly == null) {
                    readOnly = readWrite.readOnlySnapshot();
                }
            }
        }

        return readOnly;
    }

    @Override
    public int size() {
        return size;
    }
}
