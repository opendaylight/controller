/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.QName;

public final class QNameFactory {
    public static final class Key implements Immutable {
        private final @NonNull String localName;
        private final @NonNull String namespace;
        private final @Nullable String revision;

        public Key(final String localName, final String namespace, final String revision) {
            this.localName = requireNonNull(localName);
            this.namespace = requireNonNull(namespace);
            this.revision = revision;
        }

        @Override
        public int hashCode() {
            return Objects.hash(localName, namespace, revision);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            return localName.equals(other.localName) && namespace.equals(other.namespace)
                    && Objects.equals(revision, other.revision);
        }

        QName toQName() {
            return revision != null ? QName.create(namespace, revision, localName) : QName.create(namespace, localName);
        }
    }

    private static final int MAX_QNAME_CACHE_SIZE = Integer.getInteger(
        "org.opendaylight.controller.cluster.datastore.node.utils.qname-cache.max-size", 10000);

    private static final LoadingCache<String, QName> STRING_CACHE = CacheBuilder.newBuilder()
            .maximumSize(MAX_QNAME_CACHE_SIZE).weakValues().build(new CacheLoader<String, QName>() {
                @Override
                public QName load(final String key) {
                    return QName.create(key).intern();
                }
            });

    private static final LoadingCache<Key, QName> KEY_CACHE = CacheBuilder.newBuilder()
            .maximumSize(MAX_QNAME_CACHE_SIZE).weakValues().build(new CacheLoader<Key, QName>() {
                @Override
                public QName load(final Key key) {
                    return key.toQName().intern();
                }
            });

    private QNameFactory() {

    }

    @Deprecated
    public static QName create(final String name) {
        return STRING_CACHE.getUnchecked(name);
    }

    public static QName create(final Key key) {
        return KEY_CACHE.getUnchecked(key);
    }
}
