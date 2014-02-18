/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.util

import com.google.common.collect.Multimap
import java.util.Collection
import java.util.HashSet
import java.util.Map.Entry
import org.opendaylight.yangtools.concepts.Path

class MapUtils {

    public static def <P extends Path<P>, V> Collection<Entry<? extends P, ? extends V>> getAllChildren(
        Multimap<? extends P, ? extends V> map, P path) {
        val ret = new HashSet();
        val entries = map.entries;

        for (entry : entries) {
            val currentPath = entry.key;
            // If the registered reader processes nested elements
            if (path.contains(currentPath)) {
                ret.add(entry);
            } else if(currentPath.contains(path)) {
                // If the registered reader is parent of entry
                ret.add(entry);
            }
        }

        return ret;
    }
}
