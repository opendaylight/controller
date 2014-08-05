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

import org.opendaylight.yangtools.yang.common.QName;

import java.util.HashMap;
import java.util.Map;

public class QNameFactory {
    private static final Map<String, QName> cache = new HashMap<>();

    public static QName create(String name){
        QName value = cache.get(name);
        if(value == null){
            synchronized (cache){
                value = cache.get(name);
                if(value == null) {
                    value = QName.create(name);
                    cache.put(name, value);
                }
            }
        }
        return value;
    }
}
