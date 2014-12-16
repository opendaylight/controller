/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import org.opendaylight.yangtools.yang.common.QName;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Picked from Yang tools because it was not public class
 */

final class RandomPrefix {
    final Map<URI, String> prefixes = new HashMap<>();

    Iterable<Entry<URI, String>> getPrefixes() {
        return prefixes.entrySet();
    }

    String encodeQName(final QName qname) {
        String prefix = prefixes.get(qname.getNamespace());
        if (prefix == null) {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            do {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    sb.append((char)('a' + random.nextInt(25)));
                }

                prefix = sb.toString();
            } while (prefixes.containsValue(prefix));
            prefixes.put(qname.getNamespace(), prefix);
        }

        return prefix + ':' + qname.getLocalName();
    }
}
