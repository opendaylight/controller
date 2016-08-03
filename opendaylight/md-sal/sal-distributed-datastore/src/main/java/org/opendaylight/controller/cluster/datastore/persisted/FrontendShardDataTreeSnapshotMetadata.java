/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public final class FrontendShardDataTreeSnapshotMetadata extends
    ShardDataTreeSnapshotMetadata<FrontendShardDataTreeSnapshotMetadata> {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ImmutableRangeSet<UnsignedLong> purgedHistories;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendShardDataTreeSnapshotMetadata metadata) {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(purgedHistories);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            purgedHistories = (ImmutableRangeSet<UnsignedLong>) in.readObject();
        }

        private Object readResolve() {
            return new FrontendShardDataTreeSnapshotMetadata(purgedHistories);
        }
    }

    private static final long serialVersionUID = 1L;

    private final ImmutableRangeSet<UnsignedLong> purgedHistories;

    public FrontendShardDataTreeSnapshotMetadata(final RangeSet<UnsignedLong> purgedHistories) {
        this.purgedHistories = ImmutableRangeSet.copyOf(purgedHistories);
    }

    public RangeSet<UnsignedLong> getPurgedHistories() {
        return purgedHistories;
    }

    @Override
    protected Externalizable externalizableProxy() {
        return new Proxy(this);
    }

    @Override
    public Class<FrontendShardDataTreeSnapshotMetadata> getType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }
}
