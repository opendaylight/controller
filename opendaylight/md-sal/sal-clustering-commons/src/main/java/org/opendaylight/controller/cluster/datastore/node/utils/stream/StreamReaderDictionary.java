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
 * Dictionary for use with {@link org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter}s.
 * An instance can be owned by a single writer at any given time.
 */
@NotThreadSafe
public final class StreamReaderDictionary extends AbstractStreamDictionary {
    private final Map<Integer, String> strings = new HashMap<>();
    private final Map<Integer, QName> qnames = new HashMap<>();

    StreamReaderDictionary() {
        // Hidden on purpose
    }

    @Nullable final QName lookupQName(final int code) {
        return qnames.get(code);
    }

    @Nullable final String lookupString(final int code) {
        return strings.get(code);
    }

    void storeQName(final QName qname) {
        Preconditions.checkNotNull(qname);

        final int code = strings.size();
        final QName prev = qnames.put(code, QName.cachedReference(qname));
        Verify.verify(prev == null, "Conflicting assignment of code %s from '%s' to '%s'", code, prev, qname);
    }

    void storeString(final String string) {
        Preconditions.checkNotNull(string);

        final int code = strings.size();
        final String prev = strings.put(code, string);
        Verify.verify(prev == null, "Conflicting assignment of code %s from '%s' to '%s'", code, prev, string);
    }
}
