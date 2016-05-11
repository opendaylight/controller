/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

/**
 * Identifier of a logical {@link DistributedDataStore} instance.
 */
final class DistributedDataStoreFrontend implements FrontendType, Comparable<DistributedDataStoreFrontend> {
    private static final long serialVersionUID = 1L;

    private final String storeName;

    DistributedDataStoreFrontend(final String storeName) {
        this.storeName = Preconditions.checkNotNull(storeName);
    }

    String getStoreName() {
        return storeName;
    }

    @Override
    public int hashCode() {
        return storeName.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        return o instanceof DistributedDataStoreFrontend &&
                storeName.equals(((DistributedDataStoreFrontend) o).storeName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(DistributedDataStoreFrontend.class).add("storeName", storeName)
                .toString();
    }

    @Override
    public int compareTo(DistributedDataStoreFrontend o) {
        return storeName.compareTo(o.storeName);
    }
}
