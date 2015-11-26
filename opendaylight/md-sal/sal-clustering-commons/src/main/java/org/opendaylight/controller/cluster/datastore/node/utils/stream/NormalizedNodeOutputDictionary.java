/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * Dictionary for use with {@link DictionaryNormalizedNodeDataOutput}s. An instance can be owned by a single output at
 * any given time.
 */
@NotThreadSafe
public final class NormalizedNodeOutputDictionary extends AbstractNormalizedNodeDictionary {
    private final Map<String, Integer> strings = new HashMap<>();
    private final Map<QName, Integer> qnames = new HashMap<>();

    NormalizedNodeOutputDictionary() {
        // Hidden on purpose
    }

    @Nullable Integer lookupQName(final QName key) {
        return qnames.get(Preconditions.checkNotNull(key));
    }

    @Nullable Integer lookupString(final String key) {
        return strings.get(Preconditions.checkNotNull(key));
    }

    void storeQName(final QName qname) {
        Preconditions.checkNotNull(qname);

        final int code = qnames.size();
        final Integer prev = qnames.put(qname, code);
        Verify.verify(prev == null, "Conflicting assignment of qname %s from '%s' to '%s'", qname, prev, code);
    }

    void storeString(final String string) {
        Preconditions.checkNotNull(string);

        final int code = strings.size();
        final Integer prev = strings.put(string, code);
        Verify.verify(prev == null, "Conflicting assignment of string %s from '%s' to '%s'", string, prev, code);
    }
}
