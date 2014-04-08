/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.impl.util;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import org.opendaylight.yangtools.concepts.Path;

@SuppressWarnings("all")
public class MapUtils {
  public static <P extends Path<P>, V extends Object> Collection<Entry<? extends P,? extends V>> getAllChildren(final Multimap<? extends P,? extends V> map, final P path) {
    HashSet<Entry<? extends P,? extends V>> _hashSet = new HashSet<Entry<? extends P, ? extends V>>();
    final HashSet<Entry<? extends P,? extends V>> ret = _hashSet;
    final Collection<? extends Entry<? extends P,? extends V>> entries = map.entries();
    for (final Entry<? extends P,? extends V> entry : entries) {
      {
        final P currentPath = entry.getKey();
        if (path.contains(currentPath)) {
          ret.add(entry);
        } else if (currentPath.contains(path)){
            ret.add(entry);
        }
      }
    }
    return ret;
  }
}

