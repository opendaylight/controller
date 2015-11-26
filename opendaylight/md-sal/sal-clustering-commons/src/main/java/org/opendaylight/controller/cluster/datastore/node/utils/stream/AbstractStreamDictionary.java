/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for stream dictionaries. This class is kept package-private and we expose only
 * direction-specific subclasses are exposed to outside users to prevent accidental misuse.
 */
@NotThreadSafe
abstract class AbstractStreamDictionary {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStreamDictionary.class);
    private final Map<Integer, String> strings = new HashMap<>();

    AbstractStreamDictionary() {
        // Hidden to prevent instantiation
    }

    String lookupString(final int code) {
        return strings.get(code);
    }

    void storeString(final int code, @Nonnull final String string) {
        Preconditions.checkNotNull(string);

        final String prev = strings.put(code, string);
        if (prev != null) {
            LOG.warn("Overriding code %s mapping from \'%s\' to \'%s\'", code, prev, string);
        }
    }
}
