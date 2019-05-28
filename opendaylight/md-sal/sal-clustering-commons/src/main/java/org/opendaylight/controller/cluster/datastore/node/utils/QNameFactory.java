/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.yangtools.yang.common.QName;

public final class QNameFactory {
    private static final int MAX_QNAME_CACHE_SIZE = Integer.getInteger(
        "org.opendaylight.controller.cluster.datastore.node.utils.qname-cache.max-size", 10000);

    private static final LoadingCache<String, QName> CACHE = CacheBuilder.newBuilder().maximumSize(MAX_QNAME_CACHE_SIZE)
            .weakValues().build(new CacheLoader<String, QName>() {
                @Override
                public QName load(final String key) {
                    return QName.create(key).intern();
                }
            });

    private QNameFactory() {

    }

    public static QName create(final String name) {
        return CACHE.getUnchecked(name);
    }
}
