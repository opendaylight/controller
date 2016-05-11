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
import java.util.Objects;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Identifier of a logical {@link DistributedDataStore} instance.
 */
final class DistributedDataStoreIdentifier implements Identifier {
    private static final long serialVersionUID = 1L;

    private final MemberName member;
    private final String storeName;

    DistributedDataStoreIdentifier(final MemberName member, final String storeName) {
        this.member = Preconditions.checkNotNull(member);
        this.storeName = Preconditions.checkNotNull(storeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(member, storeName);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DistributedDataStoreIdentifier)) {
            return false;
        }

        final DistributedDataStoreIdentifier other = (DistributedDataStoreIdentifier) o;
        return member.equals(other.member) && storeName.equals(other.storeName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(DistributedDataStoreIdentifier.class).add("member", member)
                .add("name", storeName).toString();
    }
}
