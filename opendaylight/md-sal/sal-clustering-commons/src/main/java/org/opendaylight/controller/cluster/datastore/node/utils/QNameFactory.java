/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.yangtools.yang.common.QName;

public class QNameFactory {

    private static final int MAX_QNAME_CACHE_SIZE = 10000;

    private static final LoadingCache<String, QName> CACHE = CacheBuilder.newBuilder()
        .maximumSize(MAX_QNAME_CACHE_SIZE)
        .softValues()
        .build(
            new CacheLoader<String, QName>() {
                @Override
                public QName load(String key) {
                    return QName.create(key);
                }
            }
        );


    public static QName create(String name){
        return CACHE.getUnchecked(name);
    }
}
