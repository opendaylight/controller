/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

public final class ElectionTimeout implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long count;

    /**
     * Create a new ElectionTimeout message with the current count.
     * When the count is negative, the ET is to be acted upon immediately.
     * @param count holds the count of the current ET message.
     */
    public ElectionTimeout(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    @SuppressWarnings({ "static-method", "unused" })
    private ElectionTimeout readResolve() {
        return new ElectionTimeout(-1L);
    }
}
