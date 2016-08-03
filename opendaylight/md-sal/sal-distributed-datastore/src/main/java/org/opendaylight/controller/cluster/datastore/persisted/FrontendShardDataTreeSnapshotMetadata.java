/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FrontendShardDataTreeSnapshotMetadata extends
        ShardDataTreeSnapshotMetadata<FrontendShardDataTreeSnapshotMetadata> {

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private List<FrontendClientMetadata> clients;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendShardDataTreeSnapshotMetadata metadata) {
            this.clients = metadata.getClients();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(clients.size());
            for (final FrontendClientMetadata c : clients) {
                c.writeTo(out);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();
            final List<FrontendClientMetadata> readedClients = new ArrayList<>(size);
            for (int i = 0; i < size ; ++i) {
                readedClients.add(FrontendClientMetadata.readFrom(in));
            }
            this.clients = ImmutableList.copyOf(readedClients);
        }

        private Object readResolve() {
            return new FrontendShardDataTreeSnapshotMetadata(clients);
        }
    }

    private static final long serialVersionUID = 1L;

    private final List<FrontendClientMetadata> clients;

    FrontendShardDataTreeSnapshotMetadata(final Collection<FrontendClientMetadata> clients) {
        this.clients = ImmutableList.copyOf(clients);
    }

    public List<FrontendClientMetadata> getClients() {
        return clients;
    }

    @Override
    protected Externalizable externalizableProxy() {
        return new Proxy(this);
    }

    @Override
    public Class<FrontendShardDataTreeSnapshotMetadata> getType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }

    @Override
    public int hashCode() {
        return clients.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontendShardDataTreeSnapshotMetadata)) {
            return false;
        }

        final FrontendShardDataTreeSnapshotMetadata other = (FrontendShardDataTreeSnapshotMetadata) o;
        // TODO : is order important ?
//        return clients.containsAll(other.clients) && other.clients.containsAll(clients);
        return clients.equals(other.clients);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendShardDataTreeSnapshotMetadata.class).add("clients", clients)
                .toString();
    }
}
