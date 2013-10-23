package org.opendaylight.controller.sal.binding.impl.util

import java.util.Map.Entry
import org.opendaylight.yangtools.concepts.Path
import java.util.Map
import java.util.Set
import java.util.Collection
import java.util.HashSet
import com.google.common.collect.Multimap

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
